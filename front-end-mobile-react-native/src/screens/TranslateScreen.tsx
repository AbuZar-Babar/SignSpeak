import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import { CameraView, useCameraPermissions } from "expo-camera";
import { colors } from "../theme/colors";
import { StatusPill } from "../components/StatusPill";
import { checkHealth } from "../services/predictionApi";
import { InferenceModel, StreamTransport } from "../types/prediction";
import { useTranslationStream } from "../hooks/useTranslationStream";

const SEQUENCE_LENGTH = 60;

function prettyActionName(action: string): string {
  return action.replaceAll("_", " ").toUpperCase();
}

function formatMetric(value: number): string {
  return Number.isFinite(value) ? value.toFixed(3) : "0.000";
}

export function TranslateScreen() {
  const [permission, requestPermission] = useCameraPermissions();
  const cameraRef = useRef<CameraView | null>(null);

  const [backendUrlInput, setBackendUrlInput] = useState("http://192.168.100.94:8000");
  const [backendUrl, setBackendUrl] = useState("http://192.168.100.94:8000");
  const [model, setModel] = useState<InferenceModel>("augmented");
  const [transportMode, setTransportMode] = useState<StreamTransport>("frames");
  const [isConnected, setIsConnected] = useState(false);
  const [isCheckingConnection, setIsCheckingConnection] = useState(false);
  const [connectionMessage, setConnectionMessage] = useState<string | null>(null);

  const {
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
  } = useTranslationStream({
    cameraRef,
    backendUrl,
    model,
    transportMode,
    sequenceLength: SEQUENCE_LENGTH,
    captureIntervalMs: transportMode === "frames" ? 34 : 120,
    requestGapMs: transportMode === "frames" ? 900 : 1200,
    confidenceThreshold: 0.8,
    smoothingWindow: 10,
    requiredMatches: 9,
  });

  const refreshConnection = useCallback(async () => {
    if (transportMode === "ondevice") {
      setIsConnected(true);
      setConnectionMessage(null);
      setIsCheckingConnection(false);
      return;
    }
    setIsCheckingConnection(true);
    const connected = await checkHealth(backendUrl);
    setIsConnected(connected);
    setIsCheckingConnection(false);
    setConnectionMessage(connected ? null : "Backend unreachable from phone");
  }, [backendUrl, transportMode]);

  useEffect(() => {
    void refreshConnection();
    const interval = setInterval(() => {
      void refreshConnection();
    }, 10000);
    return () => clearInterval(interval);
  }, [refreshConnection]);

  const requiresBackend = transportMode !== "ondevice";
  const canStartStreaming = permission?.granted && (requiresBackend ? isConnected : true);
  const isBufferReady = frameBufferCount >= SEQUENCE_LENGTH;

  const confidencePercent = useMemo(() => {
    if (!lastResult) {
      return "0.0";
    }
    return (lastResult.confidence * 100).toFixed(1);
  }, [lastResult]);

  const latencyTone = useMemo(() => {
    if (lastRoundTripMs === null) {
      return "neutral" as const;
    }
    if (lastRoundTripMs <= 500) {
      return "ok" as const;
    }
    if (lastRoundTripMs <= 1200) {
      return "warn" as const;
    }
    return "danger" as const;
  }, [lastRoundTripMs]);

  const extractorTone = useMemo(() => {
    if (transportMode === "frames") {
      return "neutral" as const;
    }
    if (extractorStage === "ready") {
      return "ok" as const;
    }
    if (extractorStage === "error") {
      return "danger" as const;
    }
    if (extractorStage === "loading_model" || extractorStage === "initializing_tf") {
      return "warn" as const;
    }
    return "neutral" as const;
  }, [extractorStage, transportMode]);

  const extractorLabel = useMemo(() => {
    if (transportMode === "frames") {
      return "N/A";
    }
    if (extractorStage === "ready") {
      return "READY";
    }
    if (extractorStage === "initializing_tf") {
      return "INIT TF";
    }
    if (extractorStage === "loading_model") {
      return "LOAD MODEL";
    }
    if (extractorStage === "loading_tflite") {
      return "LOAD TFLITE";
    }
    if (extractorStage === "error") {
      return "ERROR";
    }
    return "IDLE";
  }, [extractorStage, transportMode]);

  const onApplyBackend = useCallback(() => {
    const trimmed = backendUrlInput.trim();
    if (!trimmed) {
      return;
    }
    setBackendUrl(trimmed);
  }, [backendUrlInput]);

  const onStreamPress = useCallback(async () => {
    if (isStreaming) {
      stopStreaming();
      return;
    }

    if (requiresBackend) {
      const connected = await checkHealth(backendUrl);
      setIsConnected(connected);
      if (!connected) {
        setConnectionMessage("Cannot start stream until backend responds to /health");
        return;
      }
      setConnectionMessage(null);
    } else {
      setConnectionMessage(null);
    }
    await startStreaming();
  }, [backendUrl, isStreaming, requiresBackend, startStreaming, stopStreaming]);

  if (!permission) {
    return (
      <View style={styles.loaderWrap}>
        <ActivityIndicator size="large" color={colors.accentMain} />
        <Text style={styles.loaderText}>Checking camera permissions...</Text>
      </View>
    );
  }

  if (!permission.granted) {
    return (
      <View style={styles.loaderWrap}>
        <Text style={styles.loaderTitle}>Camera access required</Text>
        <Text style={styles.loaderText}>
          Translation stream needs camera frames to build the 60-frame sequence.
        </Text>
        <Pressable style={styles.primaryButton} onPress={requestPermission}>
          <Text style={styles.primaryButtonText}>Allow Camera</Text>
        </Pressable>
      </View>
    );
  }

  return (
    <ScrollView contentContainerStyle={styles.container}>
      <View style={styles.cameraCard}>
        <CameraView ref={cameraRef} style={styles.camera} facing="front" />
        <View style={styles.cameraOverlay}>
          <StatusPill
            label="Connection"
            value={transportMode === "ondevice" ? "LOCAL" : isConnected ? "ONLINE" : "OFFLINE"}
            tone={transportMode === "ondevice" ? "ok" : isConnected ? "ok" : "danger"}
          />
          <StatusPill
            label="Buffer"
            value={`${frameBufferCount}/${SEQUENCE_LENGTH}`}
            tone={isBufferReady ? "ok" : "warn"}
          />
        </View>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Translation Stream</Text>
        <View style={styles.pillRow}>
          <StatusPill
            label="Mode"
            value={model.toUpperCase()}
            tone={model === "augmented" ? "ok" : "neutral"}
          />
          <StatusPill
            label="Transport"
            value={transportMode.toUpperCase()}
            tone={
              transportMode === "ondevice"
                ? "ok"
                : transportMode === "landmarks"
                  ? "warn"
                  : "neutral"
            }
          />
          <StatusPill
            label="Extractor"
            value={extractorLabel}
            tone={extractorTone}
          />
          <StatusPill
            label="Predicting"
            value={isPredicting ? "YES" : "NO"}
            tone={isPredicting ? "warn" : "neutral"}
          />
          <StatusPill
            label="Requests"
            value={`${totalRequests}`}
            tone="neutral"
          />
          <StatusPill
            label="Latency"
            value={lastRoundTripMs === null ? "--" : `${lastRoundTripMs}ms`}
            tone={latencyTone}
          />
        </View>

        <View style={styles.modelRow}>
          {(["augmented", "baseline"] as InferenceModel[]).map((choice) => {
            const selected = model === choice;
            return (
              <Pressable
                key={choice}
                style={[
                  styles.modelButton,
                  selected ? styles.modelButtonActive : styles.modelButtonInactive,
                ]}
                onPress={() => setModel(choice)}
              >
                <Text
                  style={[
                    styles.modelButtonText,
                    selected ? styles.modelButtonTextActive : styles.modelButtonTextInactive,
                  ]}
                >
                  {choice.toUpperCase()}
                </Text>
              </Pressable>
            );
          })}
        </View>

        <View style={styles.modelRow}>
          {(["frames", "landmarks", "ondevice"] as StreamTransport[]).map((choice) => {
            const selected = transportMode === choice;
            return (
              <Pressable
                key={choice}
                style={[
                  styles.modelButton,
                  selected ? styles.modelButtonActive : styles.modelButtonInactive,
                  isStreaming ? styles.modelButtonDisabled : undefined,
                ]}
                onPress={() => setTransportMode(choice)}
                disabled={isStreaming}
              >
                <Text
                  style={[
                    styles.modelButtonText,
                    selected ? styles.modelButtonTextActive : styles.modelButtonTextInactive,
                  ]}
                >
                  {choice.toUpperCase()}
                </Text>
              </Pressable>
            );
          })}
        </View>
        {transportMode !== "frames" ? (
          <View style={styles.extractorCard}>
            <Text style={styles.helperText}>
              {transportMode === "ondevice"
                ? "On-device mode runs landmarks + TFLite inference entirely on phone."
                : "Landmark mode extracts keypoints on phone and sends only tensors to laptop."}
            </Text>
            {extractorStage === "initializing_tf" ||
            extractorStage === "loading_model" ||
            extractorStage === "loading_tflite" ? (
              <View style={styles.extractorRow}>
                <ActivityIndicator size="small" color={colors.warning} />
                <Text style={styles.helperText}>
                  Warming up extractor ({extractorProgress}%)
                </Text>
              </View>
            ) : null}
            {extractorStage === "ready" && extractorWarmupMs !== null ? (
              <Text style={styles.helperText}>
                Extractor ready in {extractorWarmupMs}ms
              </Text>
            ) : null}
          </View>
        ) : null}

        <Pressable
          style={[
            styles.primaryButton,
            !canStartStreaming && !isStreaming ? styles.primaryButtonDisabled : undefined,
          ]}
          onPress={onStreamPress}
          disabled={!canStartStreaming && !isStreaming}
        >
          <Text style={styles.primaryButtonText}>
            {isStreaming ? "Stop Stream" : "Start Continuous Stream"}
          </Text>
        </Pressable>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Backend</Text>
        {transportMode === "ondevice" ? (
          <Text style={styles.helperText}>Not used in on-device mode.</Text>
        ) : null}
        <TextInput
          style={styles.input}
          value={backendUrlInput}
          onChangeText={setBackendUrlInput}
          autoCapitalize="none"
          autoCorrect={false}
          placeholder="http://192.168.x.x:8000"
          placeholderTextColor={colors.inactive}
        />
        <View style={styles.row}>
          <Pressable style={styles.secondaryButton} onPress={onApplyBackend}>
            <Text style={styles.secondaryButtonText}>Apply URL</Text>
          </Pressable>
          <Pressable style={styles.secondaryButton} onPress={refreshConnection}>
            <Text style={styles.secondaryButtonText}>
              {isCheckingConnection ? "Checking..." : "Check Health"}
            </Text>
          </Pressable>
        </View>
        <Text style={styles.helperText}>Active URL: {backendUrl}</Text>
        {connectionMessage ? <Text style={styles.errorText}>{connectionMessage}</Text> : null}
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Live Result</Text>
        <View style={styles.resultCard}>
          <Text style={styles.resultAction}>
            {lastResult ? prettyActionName(lastResult.action) : "WAITING FOR STABLE SIGN"}
          </Text>
          <Text style={styles.resultConfidence}>Confidence: {confidencePercent}%</Text>
          <View style={styles.progressRail}>
            <View
              style={[
                styles.progressFill,
                { width: `${Math.max(2, Number(confidencePercent))}%` },
              ]}
            />
          </View>
          {lastResult ? (
            <Text style={styles.metaText}>
              hands {lastResult.hands_detected}/{lastResult.frames_processed} |{" "}
              {transportMode === "ondevice" ? "on-device" : "backend"}{" "}
              {lastResult.processing_time_ms}ms
            </Text>
          ) : null}
          <Text style={styles.metaText}>
            round-trip {lastRoundTripMs === null ? "--" : `${lastRoundTripMs}ms`} | avg{" "}
            {avgRoundTripMs === null ? "--" : `${avgRoundTripMs}ms`}
          </Text>
          <Text style={styles.metaText}>stability votes: {stabilityHits}/10</Text>
        </View>
      </View>

      {transportMode !== "frames" ? (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Landmark Debug</Text>
          <View style={styles.resultCard}>
            <Text style={styles.metaText}>
              local frames: {landmarkDebugLocal?.frames ?? 0}/{SEQUENCE_LENGTH}
            </Text>
            {landmarkDebugLocal ? (
              <>
                <Text style={styles.metaText}>
                  local LH x {formatMetric(landmarkDebugLocal.leftHand.xMin)} to{" "}
                  {formatMetric(landmarkDebugLocal.leftHand.xMax)} | RH x{" "}
                  {formatMetric(landmarkDebugLocal.rightHand.xMin)} to{" "}
                  {formatMetric(landmarkDebugLocal.rightHand.xMax)}
                </Text>
                <Text style={styles.metaText}>
                  local LH min/max {formatMetric(landmarkDebugLocal.leftHand.min)}/
                  {formatMetric(landmarkDebugLocal.leftHand.max)} | RH min/max{" "}
                  {formatMetric(landmarkDebugLocal.rightHand.min)}/
                  {formatMetric(landmarkDebugLocal.rightHand.max)}
                </Text>
              </>
            ) : (
              <Text style={styles.metaText}>Collecting local landmark ranges...</Text>
            )}
            {transportMode === "landmarks"
              ? landmarkDebugBackend ? (
                  <>
                    <Text style={styles.metaText}>
                      backend LH x {formatMetric(landmarkDebugBackend.x_value_ranges.lh_x_min)} to{" "}
                      {formatMetric(landmarkDebugBackend.x_value_ranges.lh_x_max)} | RH x{" "}
                      {formatMetric(landmarkDebugBackend.x_value_ranges.rh_x_min)} to{" "}
                      {formatMetric(landmarkDebugBackend.x_value_ranges.rh_x_max)}
                    </Text>
                    <Text style={styles.metaText}>
                      backend LH min/max {formatMetric(landmarkDebugBackend.left_hand.min)}/
                      {formatMetric(landmarkDebugBackend.left_hand.max)} | RH min/max{" "}
                      {formatMetric(landmarkDebugBackend.right_hand.min)}/
                      {formatMetric(landmarkDebugBackend.right_hand.max)}
                    </Text>
                  </>
                ) : (
                  <Text style={styles.metaText}>Waiting for backend debug echo...</Text>
                )
              : (
                <Text style={styles.metaText}>
                  Backend debug is disabled in on-device mode.
                </Text>
              )}
            <Text style={styles.helperText}>
              Expected rough range: x,y in [0..1] and z near 0 for most frames.
            </Text>
            {transportMode === "landmarks" && landmarkDebugError ? (
              <Text style={styles.errorText}>Debug: {landmarkDebugError}</Text>
            ) : null}
          </View>
        </View>
      ) : null}

      <View style={styles.section}>
        <View style={styles.row}>
          <Text style={styles.sectionTitle}>Sentence</Text>
          <Pressable style={styles.clearLink} onPress={resetSentence}>
            <Text style={styles.clearLinkText}>Clear</Text>
          </Pressable>
        </View>
        <View style={styles.sentenceCard}>
          <Text style={styles.sentenceText}>
            {sentence.length ? sentence.join(" ") : "No confirmed words yet."}
          </Text>
        </View>
      </View>

      {error ? (
        <View style={styles.section}>
          <Text style={styles.errorText}>Stream error: {error}</Text>
        </View>
      ) : null}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    padding: 14,
    gap: 12,
  },
  loaderWrap: {
    flex: 1,
    backgroundColor: colors.appBg,
    alignItems: "center",
    justifyContent: "center",
    gap: 14,
    padding: 20,
  },
  loaderTitle: {
    color: colors.textPrimary,
    fontSize: 22,
    fontWeight: "800",
  },
  loaderText: {
    color: colors.textSecondary,
    textAlign: "center",
    fontSize: 14,
    lineHeight: 20,
  },
  cameraCard: {
    backgroundColor: colors.cardBg,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: colors.border,
    overflow: "hidden",
    height: 320,
  },
  camera: {
    flex: 1,
  },
  cameraOverlay: {
    position: "absolute",
    left: 10,
    top: 10,
    flexDirection: "row",
    gap: 8,
  },
  section: {
    backgroundColor: colors.cardBg,
    borderColor: colors.border,
    borderWidth: 1,
    borderRadius: 16,
    padding: 14,
    gap: 10,
  },
  sectionTitle: {
    color: colors.textPrimary,
    fontWeight: "800",
    fontSize: 16,
  },
  pillRow: {
    flexDirection: "row",
    gap: 8,
    flexWrap: "wrap",
  },
  modelRow: {
    flexDirection: "row",
    gap: 10,
  },
  modelButton: {
    flex: 1,
    borderRadius: 12,
    paddingVertical: 10,
    alignItems: "center",
    borderWidth: 1,
  },
  modelButtonActive: {
    backgroundColor: "rgba(32, 201, 151, 0.2)",
    borderColor: colors.accentMain,
  },
  modelButtonInactive: {
    backgroundColor: colors.cardMuted,
    borderColor: colors.border,
  },
  modelButtonDisabled: {
    opacity: 0.5,
  },
  modelButtonText: {
    fontSize: 13,
    fontWeight: "800",
    letterSpacing: 0.7,
  },
  modelButtonTextActive: {
    color: colors.accentMain,
  },
  modelButtonTextInactive: {
    color: colors.textSecondary,
  },
  primaryButton: {
    backgroundColor: colors.accentMain,
    borderRadius: 12,
    minHeight: 48,
    alignItems: "center",
    justifyContent: "center",
  },
  primaryButtonDisabled: {
    backgroundColor: colors.inactive,
  },
  primaryButtonText: {
    color: "#042a1f",
    fontWeight: "900",
    letterSpacing: 0.5,
    fontSize: 15,
  },
  input: {
    borderRadius: 12,
    borderColor: colors.border,
    borderWidth: 1,
    backgroundColor: colors.cardMuted,
    color: colors.textPrimary,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 14,
  },
  row: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    gap: 10,
  },
  secondaryButton: {
    flex: 1,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.cardMuted,
    borderRadius: 10,
    minHeight: 42,
    alignItems: "center",
    justifyContent: "center",
  },
  secondaryButtonText: {
    color: colors.textPrimary,
    fontWeight: "700",
    fontSize: 13,
  },
  helperText: {
    color: colors.textSecondary,
    fontSize: 12,
  },
  extractorCard: {
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.cardMuted,
    borderRadius: 10,
    padding: 10,
    gap: 6,
  },
  extractorRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
  resultCard: {
    borderRadius: 14,
    borderColor: colors.border,
    borderWidth: 1,
    backgroundColor: colors.cardMuted,
    padding: 12,
    gap: 8,
  },
  resultAction: {
    color: colors.textPrimary,
    fontSize: 24,
    fontWeight: "900",
    letterSpacing: 1,
  },
  resultConfidence: {
    color: colors.textSecondary,
    fontSize: 14,
    fontWeight: "700",
  },
  progressRail: {
    height: 10,
    borderRadius: 999,
    backgroundColor: "#0d1e2b",
    overflow: "hidden",
  },
  progressFill: {
    height: "100%",
    backgroundColor: colors.accentAlt,
  },
  metaText: {
    color: colors.textSecondary,
    fontSize: 12,
  },
  clearLink: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: "rgba(255, 159, 67, 0.2)",
    borderWidth: 1,
    borderColor: colors.accentAlt,
  },
  clearLinkText: {
    color: colors.accentAlt,
    fontWeight: "800",
    fontSize: 12,
  },
  sentenceCard: {
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.cardMuted,
    minHeight: 80,
    padding: 12,
  },
  sentenceText: {
    color: colors.textPrimary,
    fontSize: 15,
    lineHeight: 22,
    fontWeight: "600",
  },
  errorText: {
    color: colors.danger,
    fontWeight: "700",
    fontSize: 13,
  },
});
