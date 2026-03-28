import * as FileSystem from "expo-file-system/legacy";
import * as tf from "@tensorflow/tfjs";
import "@tensorflow/tfjs-backend-cpu";
import * as handPoseDetection from "@tensorflow-models/hand-pose-detection";
import jpeg from "jpeg-js";
import { LandmarkExtractorStage } from "../types/prediction";

type HandDetector = handPoseDetection.HandDetector;

let detectorPromise: Promise<HandDetector> | null = null;

function clamp01(value: number): number {
  if (!Number.isFinite(value)) {
    return 0;
  }
  if (value < 0) {
    return 0;
  }
  if (value > 1) {
    return 1;
  }
  return value;
}

function resolveHandSlot(hand: any): "left" | "right" | null {
  const direct = typeof hand?.handedness === "string" ? hand.handedness : null;
  const arrayLabel = Array.isArray(hand?.handedness)
    ? hand.handedness[0]?.categoryName ??
      hand.handedness[0]?.label ??
      hand.handedness[0]?.displayName
    : null;
  const label = (direct ?? arrayLabel ?? "").toString().toLowerCase();

  if (label.includes("left")) {
    return "left";
  }
  if (label.includes("right")) {
    return "right";
  }
  return null;
}

async function getDetector(
  onStage?: (stage: LandmarkExtractorStage) => void
): Promise<HandDetector> {
  if (detectorPromise) {
    onStage?.("ready");
    return detectorPromise;
  }

  detectorPromise = (async () => {
    onStage?.("initializing_tf");
    await tf.ready();
    if (tf.getBackend() !== "cpu") {
      await tf.setBackend("cpu");
    }

    onStage?.("loading_model");
    const detector = await handPoseDetection.createDetector(
      handPoseDetection.SupportedModels.MediaPipeHands,
      {
        runtime: "tfjs",
        modelType: "lite",
        maxHands: 2,
      }
    );
    onStage?.("ready");
    return detector;
  })();

  try {
    return await detectorPromise;
  } catch (error) {
    detectorPromise = null;
    throw error;
  }
}

export async function prepareLandmarkExtractor(
  onStage?: (stage: LandmarkExtractorStage) => void
): Promise<void> {
  await getDetector(onStage);
}

export async function disposeLandmarkExtractor(): Promise<void> {
  if (!detectorPromise) {
    return;
  }

  try {
    const detector = await detectorPromise;
    detector.dispose();
  } catch {
    // Ignore disposal failures.
  } finally {
    detectorPromise = null;
  }
}

export async function extractHandLandmarksFromPhoto(photoUri: string): Promise<number[]> {
  const detector = await getDetector();
  const base64 = await FileSystem.readAsStringAsync(photoUri, {
    encoding: FileSystem.EncodingType.Base64,
  });
  const jpegBytes = tf.util.encodeString(base64, "base64");
  const decoded = jpeg.decode(jpegBytes, { useTArray: true });

  const transformedImage = tf.tidy(() => {
    const rgba = tf.tensor3d(decoded.data, [decoded.height, decoded.width, 4], "int32");
    const rgb = tf.slice(rgba, [0, 0, 0], [-1, -1, 3]);
    const rotated = tf.reverse(tf.transpose(rgb, [1, 0, 2]), [0]);
    const mirrored = tf.reverse(rotated, [1]);
    return mirrored as tf.Tensor3D;
  });

  try {
    const [height, width] = transformedImage.shape;
    const maxDim = Math.max(width, height);
    const hands = await detector.estimateHands(transformedImage, { flipHorizontal: false });

    const leftHand = new Array<number>(63).fill(0);
    const rightHand = new Array<number>(63).fill(0);

    for (const hand of hands) {
      const slot = resolveHandSlot(hand);
      if (!slot) {
        continue;
      }
      const points = Array.isArray(hand.keypoints) ? hand.keypoints : [];
      if (points.length < 21) {
        continue;
      }

      const target = slot === "left" ? leftHand : rightHand;
      for (let i = 0; i < 21; i += 1) {
        const point = points[i] as { x?: number; y?: number; z?: number };
        const base = i * 3;
        target[base] = clamp01(Number(point.x ?? 0) / width);
        target[base + 1] = clamp01(Number(point.y ?? 0) / height);
        target[base + 2] = Number(point.z ?? 0) / maxDim;
      }
    }

    return [...leftHand, ...rightHand];
  } finally {
    transformedImage.dispose();
  }
}
