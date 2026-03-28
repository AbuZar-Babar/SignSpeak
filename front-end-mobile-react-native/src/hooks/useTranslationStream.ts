import { RefObject, useCallback, useEffect, useRef, useState } from "react";
import { CameraView } from "expo-camera";
import * as FileSystem from "expo-file-system";
import {
  BackendDebugEchoResponse,
  InferenceModel,
  LandmarkDebugSnapshot,
  LandmarkExtractorStage,
  PredictionResponse,
  StreamTransport,
} from "../types/prediction";
import {
  debugEchoLandmarks,
  predictFromFrames,
  predictFromLandmarks,
} from "../services/predictionApi";
import {
  disposeLandmarkExtractor,
  extractHandLandmarksFromPhoto,
  prepareLandmarkExtractor,
} from "../services/mobileLandmarkExtractor";
import {
  prepareOnDeviceInferenceModel,
  predictFromLandmarksOnDevice,
  resetOnDeviceInferenceCache,
} from "../services/onDeviceTflite";

interface UseTranslationStreamArgs {
  cameraRef: RefObject<CameraView | null>;
  backendUrl: string;
  model: InferenceModel;
  transportMode?: StreamTransport;
  sequenceLength?: number;
  captureIntervalMs?: number;
  requestGapMs?: number;
  confidenceThreshold?: number;
  smoothingWindow?: number;
  requiredMatches?: number;
  maxSentenceWords?: number;
  jpegQuality?: number;
}

async function deleteFrameQuietly(uri: string): Promise<void> {
  try {
    await FileSystem.deleteAsync(uri, { idempotent: true });
  } catch {
    // Ignore cache cleanup failures.
  }
}

function summarizeHandRanges(sequence: number[][], startIndex: number) {
  let present = false;
  let min = Number.POSITIVE_INFINITY;
  let max = Number.NEGATIVE_INFINITY;
  let sum = 0;
  let count = 0;
  let xMin = Number.POSITIVE_INFINITY;
  let xMax = Number.NEGATIVE_INFINITY;

  for (const frame of sequence) {
    for (let i = startIndex; i < startIndex + 63; i += 1) {
      const value = Number(frame[i] ?? 0);
      if (value !== 0) {
        present = true;
      }
      if (value < min) {
        min = value;
      }
      if (value > max) {
        max = value;
      }
      sum += value;
      count += 1;
    }
    for (let i = startIndex; i < startIndex + 63; i += 3) {
      const x = Number(frame[i] ?? 0);
      if (x < xMin) {
        xMin = x;
      }
      if (x > xMax) {
        xMax = x;
      }
    }
  }

  if (!present || count === 0) {
    return {
      present: false,
      min: 0,
      max: 0,
      mean: 0,
      xMin: 0,
      xMax: 0,
    };
  }

  return {
    present: true,
    min,
    max,
    mean: sum / count,
    xMin,
    xMax,
  };
}

function buildLandmarkDebugSnapshot(sequence: number[][]): LandmarkDebugSnapshot {
  return {
    frames: sequence.length,
    leftHand: summarizeHandRanges(sequence, 0),
    rightHand: summarizeHandRanges(sequence, 63),
  };
}

export function useTranslationStream({
  cameraRef,
  backendUrl,
  model,
  transportMode = "frames",
  sequenceLength = 60,
  captureIntervalMs = 70,
  requestGapMs = 900,
  confidenceThreshold = 0.8,
  smoothingWindow = 10,
  requiredMatches = 9,
  maxSentenceWords = 20,
  jpegQuality = 0.5,
}: UseTranslationStreamArgs) {
  const [isStreaming, setIsStreaming] = useState(false);
  const [isPredicting, setIsPredicting] = useState(false);
  const [frameBufferCount, setFrameBufferCount] = useState(0);
  const [lastResult, setLastResult] = useState<PredictionResponse | null>(null);
  const [sentence, setSentence] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [stabilityHits, setStabilityHits] = useState(0);
  const [totalRequests, setTotalRequests] = useState(0);
  const [lastRoundTripMs, setLastRoundTripMs] = useState<number | null>(null);
  const [avgRoundTripMs, setAvgRoundTripMs] = useState<number | null>(null);
  const [extractorStage, setExtractorStage] = useState<LandmarkExtractorStage>("idle");
  const [extractorProgress, setExtractorProgress] = useState(0);
  const [extractorWarmupMs, setExtractorWarmupMs] = useState<number | null>(null);
  const [landmarkDebugLocal, setLandmarkDebugLocal] = useState<LandmarkDebugSnapshot | null>(null);
  const [landmarkDebugBackend, setLandmarkDebugBackend] =
    useState<BackendDebugEchoResponse | null>(null);
  const [landmarkDebugError, setLandmarkDebugError] = useState<string | null>(null);

  const frameUrisRef = useRef<string[]>([]);
  const landmarkSequenceRef = useRef<number[][]>([]);
  const captureTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const captureLockRef = useRef(false);
  const predictionLockRef = useRef(false);
  const recentPredictionsRef = useRef<string[]>([]);
  const lastRequestAtRef = useRef(0);
  const mountedRef = useRef(true);
  const latencySampleCountRef = useRef(0);
  const debugCounterRef = useRef(0);

  const clearFrameBuffer = useCallback(async () => {
    const existing = [...frameUrisRef.current];
    frameUrisRef.current = [];
    landmarkSequenceRef.current = [];
    setFrameBufferCount(0);
    setLandmarkDebugLocal(null);
    setLandmarkDebugBackend(null);
    setLandmarkDebugError(null);
    await Promise.all(existing.map((uri) => deleteFrameQuietly(uri)));
  }, []);

  const applySmoothing = useCallback(
    (result: PredictionResponse) => {
      const current = recentPredictionsRef.current;
      current.push(result.action);
      if (current.length > smoothingWindow) {
        current.shift();
      }

      const matches = current.filter((value) => value === result.action).length;
      setStabilityHits(matches);

      if (result.confidence < confidenceThreshold || matches < requiredMatches) {
        return;
      }

      if (result.action.toLowerCase() === "nothing") {
        return;
      }

      setSentence((prev) => {
        if (prev.length > 0 && prev[prev.length - 1] === result.action) {
          return prev;
        }
        const next = [...prev, result.action];
        if (next.length <= maxSentenceWords) {
          return next;
        }
        return next.slice(next.length - maxSentenceWords);
      });
    },
    [confidenceThreshold, maxSentenceWords, requiredMatches, smoothingWindow]
  );

  const runPrediction = useCallback(async () => {
    if (predictionLockRef.current) {
      return;
    }

    const frames = frameUrisRef.current.slice(-sequenceLength);
    const landmarks = landmarkSequenceRef.current.slice(-sequenceLength);
    const ready =
      transportMode === "landmarks" || transportMode === "ondevice"
        ? landmarks.length >= sequenceLength
        : frames.length >= sequenceLength;
    if (!ready) {
      return;
    }

    predictionLockRef.current = true;
    setIsPredicting(true);
    setError(null);
    setTotalRequests((prev) => prev + 1);
    const startedAt = Date.now();

    try {
      const result =
        transportMode === "ondevice"
          ? await predictFromLandmarksOnDevice(landmarks, model)
          : transportMode === "landmarks"
            ? await predictFromLandmarks(backendUrl, landmarks, model)
            : await predictFromFrames(backendUrl, frames, model);
      if (!mountedRef.current) {
        return;
      }
      const elapsedMs = Date.now() - startedAt;
      setLastRoundTripMs(elapsedMs);
      setAvgRoundTripMs((prev) => {
        const nextCount = latencySampleCountRef.current + 1;
        latencySampleCountRef.current = nextCount;
        if (prev === null) {
          return elapsedMs;
        }
        return Math.round((prev * (nextCount - 1) + elapsedMs) / nextCount);
      });
      setLastResult(result);
      applySmoothing(result);

      if (transportMode === "landmarks") {
        debugCounterRef.current += 1;
        if (debugCounterRef.current % 2 === 0) {
          void debugEchoLandmarks(backendUrl, landmarks)
            .then((debugResult) => {
              if (!mountedRef.current) {
                return;
              }
              setLandmarkDebugBackend(debugResult);
              setLandmarkDebugError(null);
            })
            .catch((debugError) => {
              if (!mountedRef.current) {
                return;
              }
              const message =
                debugError instanceof Error ? debugError.message : "Debug echo failed";
              setLandmarkDebugError(message);
            });
        }
      }
    } catch (predictionError) {
      if (!mountedRef.current) {
        return;
      }
      const message =
        predictionError instanceof Error ? predictionError.message : "Prediction failed";
      setError(message);
    } finally {
      lastRequestAtRef.current = Date.now();
      predictionLockRef.current = false;
      if (mountedRef.current) {
        setIsPredicting(false);
      }
    }
  }, [applySmoothing, backendUrl, model, sequenceLength, transportMode]);

  const captureTick = useCallback(async () => {
    if (!isStreaming || captureLockRef.current || !cameraRef.current) {
      return;
    }

    captureLockRef.current = true;

    try {
      const photo = await cameraRef.current.takePictureAsync({
        quality: jpegQuality,
        base64: false,
        exif: false,
        skipProcessing: true,
      });

      if (!photo?.uri) {
        return;
      }

      if (transportMode === "landmarks" || transportMode === "ondevice") {
        try {
          const landmarks = await extractHandLandmarksFromPhoto(photo.uri);
          landmarkSequenceRef.current.push(landmarks);
          if (landmarkSequenceRef.current.length > sequenceLength) {
            landmarkSequenceRef.current.shift();
          }
          if (mountedRef.current) {
            setLandmarkDebugLocal(buildLandmarkDebugSnapshot(landmarkSequenceRef.current));
          }
        } finally {
          void deleteFrameQuietly(photo.uri);
        }
      } else {
        frameUrisRef.current.push(photo.uri);
        if (frameUrisRef.current.length > sequenceLength) {
          const removed = frameUrisRef.current.shift();
          if (removed) {
            void deleteFrameQuietly(removed);
          }
        }
      }

      if (mountedRef.current) {
        setFrameBufferCount(
          transportMode === "landmarks" || transportMode === "ondevice"
            ? landmarkSequenceRef.current.length
            : frameUrisRef.current.length
        );
      }

      const readyToPredict =
        transportMode === "landmarks" || transportMode === "ondevice"
          ? landmarkSequenceRef.current.length >= sequenceLength
          : frameUrisRef.current.length >= sequenceLength;
      const gapElapsed = Date.now() - lastRequestAtRef.current >= requestGapMs;
      if (readyToPredict && gapElapsed && !predictionLockRef.current) {
        void runPrediction();
      }
    } catch (captureError) {
      if (!mountedRef.current) {
        return;
      }
      const message = captureError instanceof Error ? captureError.message : "Capture failed";
      setError(message);
    } finally {
      captureLockRef.current = false;
    }
  }, [
    cameraRef,
    isStreaming,
    jpegQuality,
    requestGapMs,
    runPrediction,
    sequenceLength,
    transportMode,
  ]);

  const startStreaming = useCallback(async () => {
    if (captureTimerRef.current) {
      return;
    }

    if (transportMode === "landmarks" || transportMode === "ondevice") {
      const initStartedAt = Date.now();
      setExtractorStage("initializing_tf");
      setExtractorProgress(20);
      setExtractorWarmupMs(null);
      try {
        await prepareLandmarkExtractor((stage) => {
          if (!mountedRef.current) {
            return;
          }
          setExtractorStage(stage);
          if (stage === "initializing_tf") {
            setExtractorProgress(30);
          } else if (stage === "loading_model") {
            setExtractorProgress(75);
          } else if (stage === "ready") {
            setExtractorProgress(100);
          }
        });
        if (mountedRef.current) {
          setExtractorStage("ready");
          setExtractorProgress(100);
        }

        if (transportMode === "ondevice") {
          setExtractorStage("loading_tflite");
          setExtractorProgress(92);
          await prepareOnDeviceInferenceModel(model);
          if (mountedRef.current) {
            setExtractorStage("ready");
            setExtractorProgress(100);
          }
        }

        if (mountedRef.current) {
          setExtractorWarmupMs(Date.now() - initStartedAt);
        }
      } catch (initError) {
        const message =
          initError instanceof Error
            ? initError.message
            : transportMode === "ondevice"
              ? "Failed to initialize on-device inference"
              : "Failed to initialize landmark extractor";
        setExtractorStage("error");
        setExtractorProgress(0);
        setError(message);
        return;
      }
    } else {
      setExtractorStage("idle");
      setExtractorProgress(0);
      setExtractorWarmupMs(null);
    }

    await clearFrameBuffer();
    setError(null);
    latencySampleCountRef.current = 0;
    debugCounterRef.current = 0;
    setLastRoundTripMs(null);
    setAvgRoundTripMs(null);
    lastRequestAtRef.current = 0;
    setIsStreaming(true);
    captureTimerRef.current = setInterval(() => {
      void captureTick();
    }, captureIntervalMs);
    void captureTick();
  }, [captureIntervalMs, captureTick, clearFrameBuffer, model, transportMode]);

  const stopStreaming = useCallback(() => {
    if (captureTimerRef.current) {
      clearInterval(captureTimerRef.current);
      captureTimerRef.current = null;
    }
    setIsStreaming(false);
    predictionLockRef.current = false;
    captureLockRef.current = false;
  }, []);

  const resetSentence = useCallback(() => {
    setSentence([]);
    recentPredictionsRef.current = [];
    setStabilityHits(0);
  }, []);

  useEffect(() => {
    if (!isStreaming) {
      void clearFrameBuffer();
      lastRequestAtRef.current = 0;
      if (transportMode === "frames") {
        setExtractorStage("idle");
        setExtractorProgress(0);
        setExtractorWarmupMs(null);
      }
    }
  }, [clearFrameBuffer, isStreaming, transportMode]);

  useEffect(() => {
    return () => {
      mountedRef.current = false;
      if (captureTimerRef.current) {
        clearInterval(captureTimerRef.current);
      }
      void clearFrameBuffer();
      void disposeLandmarkExtractor();
      resetOnDeviceInferenceCache();
    };
  }, [clearFrameBuffer]);

  return {
    isStreaming,
    isPredicting,
    frameBufferCount,
    lastResult,
    sentence,
    error,
    stabilityHits,
    totalRequests,
    lastRoundTripMs,
    avgRoundTripMs,
    extractorStage,
    extractorProgress,
    extractorWarmupMs,
    landmarkDebugLocal,
    landmarkDebugBackend,
    landmarkDebugError,
    startStreaming,
    stopStreaming,
    resetSentence,
    clearFrameBuffer,
  };
}
