import { loadTensorflowModel, TensorflowModel } from "react-native-fast-tflite";
import { InferenceModel, PredictionResponse } from "../types/prediction";

const SEQUENCE_LENGTH = 60;
const FEATURES_PER_FRAME = 126;

type ModelCacheEntry = {
  model: TensorflowModel;
  labels: string[];
  warmedUp: boolean;
};

const MODEL_ASSETS: Record<InferenceModel, number> = {
  baseline: require("../../assets/models/action_model_baseline.tflite"),
  augmented: require("../../assets/models/action_model_augmented.tflite"),
};

const LABEL_ASSETS: Record<InferenceModel, string[]> = {
  baseline: require("../../assets/models/labels_baseline.json") as string[],
  augmented: require("../../assets/models/labels_augmented.json") as string[],
};

const cache = new Map<InferenceModel, ModelCacheEntry>();
const loadingPromises = new Map<InferenceModel, Promise<ModelCacheEntry>>();

function countDetectedHands(sequence: number[][]): number {
  let count = 0;
  for (const frame of sequence) {
    const leftPresent = frame.slice(0, 63).some((value) => value !== 0);
    const rightPresent = frame.slice(63).some((value) => value !== 0);
    if (leftPresent || rightPresent) {
      count += 1;
    }
  }
  return count;
}

function flattenSequence(sequence: number[][]): Float32Array {
  const flat = new Float32Array(SEQUENCE_LENGTH * FEATURES_PER_FRAME);
  for (let t = 0; t < SEQUENCE_LENGTH; t += 1) {
    const frame = sequence[t];
    const offset = t * FEATURES_PER_FRAME;
    for (let i = 0; i < FEATURES_PER_FRAME; i += 1) {
      flat[offset + i] = Number(frame[i] ?? 0);
    }
  }
  return flat;
}

async function ensureLoaded(modelKey: InferenceModel): Promise<ModelCacheEntry> {
  const existing = cache.get(modelKey);
  if (existing) {
    return existing;
  }

  const inFlight = loadingPromises.get(modelKey);
  if (inFlight) {
    return inFlight;
  }

  const promise = (async () => {
    const labels = LABEL_ASSETS[modelKey];
    if (!Array.isArray(labels) || labels.length === 0) {
      throw new Error(`Missing labels for model '${modelKey}'`);
    }

    const model = await loadTensorflowModel(MODEL_ASSETS[modelKey]);
    const created: ModelCacheEntry = {
      model,
      labels,
      warmedUp: false,
    };
    cache.set(modelKey, created);
    return created;
  })();

  loadingPromises.set(modelKey, promise);
  try {
    return await promise;
  } finally {
    loadingPromises.delete(modelKey);
  }
}

export async function prepareOnDeviceInferenceModel(modelKey: InferenceModel): Promise<void> {
  const entry = await ensureLoaded(modelKey);
  if (entry.warmedUp) {
    return;
  }

  const warmupInput = new Float32Array(SEQUENCE_LENGTH * FEATURES_PER_FRAME);
  entry.model.runSync([warmupInput]);
  entry.warmedUp = true;
}

export async function predictFromLandmarksOnDevice(
  sequence: number[][],
  modelKey: InferenceModel
): Promise<PredictionResponse> {
  if (sequence.length !== SEQUENCE_LENGTH) {
    throw new Error(`Invalid sequence length ${sequence.length}; expected ${SEQUENCE_LENGTH}`);
  }
  for (const frame of sequence) {
    if (frame.length !== FEATURES_PER_FRAME) {
      throw new Error(`Invalid frame width ${frame.length}; expected ${FEATURES_PER_FRAME}`);
    }
  }

  const entry = await ensureLoaded(modelKey);
  const input = flattenSequence(sequence);
  const startedAt = Date.now();

  const outputs = entry.model.runSync([input]);
  if (!outputs || outputs.length === 0) {
    throw new Error("TFLite returned no outputs");
  }

  const probabilities = Array.from(outputs[0] as ArrayLike<number>, (value) => Number(value));
  const labelCount = entry.labels.length;
  const clampedProbabilities = probabilities.slice(0, labelCount);
  while (clampedProbabilities.length < labelCount) {
    clampedProbabilities.push(0);
  }

  let predictedIndex = 0;
  for (let i = 1; i < clampedProbabilities.length; i += 1) {
    if (clampedProbabilities[i] > clampedProbabilities[predictedIndex]) {
      predictedIndex = i;
    }
  }

  const action = entry.labels[predictedIndex] ?? "unknown";
  const confidence = Number(clampedProbabilities[predictedIndex] ?? 0);
  const processingTime = Date.now() - startedAt;
  const handsDetected = countDetectedHands(sequence);

  return {
    action,
    confidence,
    all_probabilities: Object.fromEntries(
      entry.labels.map((label, idx) => [label, Number(clampedProbabilities[idx] ?? 0)])
    ),
    model_used: `mobile_tflite_${modelKey}`,
    processing_time_ms: processingTime,
    frames_processed: SEQUENCE_LENGTH,
    hands_detected: handsDetected,
  };
}

export function resetOnDeviceInferenceCache(): void {
  cache.clear();
  loadingPromises.clear();
}
