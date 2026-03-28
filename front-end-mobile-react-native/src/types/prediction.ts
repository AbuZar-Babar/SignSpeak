export type InferenceModel = "baseline" | "augmented";
export type StreamTransport = "frames" | "landmarks" | "ondevice";
export type LandmarkExtractorStage =
  | "idle"
  | "initializing_tf"
  | "loading_model"
  | "loading_tflite"
  | "ready"
  | "error";

export interface PredictionResponse {
  action: string;
  confidence: number;
  all_probabilities: Record<string, number>;
  model_used: string;
  processing_time_ms: number;
  frames_processed: number;
  hands_detected: number;
}

export interface HandRangeStats {
  present: boolean;
  min: number;
  max: number;
  mean: number;
  xMin: number;
  xMax: number;
}

export interface LandmarkDebugSnapshot {
  frames: number;
  leftHand: HandRangeStats;
  rightHand: HandRangeStats;
}

export interface BackendDebugEchoResponse {
  shape: number[];
  left_hand: {
    present: boolean;
    min: number;
    max: number;
    mean: number;
    wrist_xyz_frame0: number[];
  };
  right_hand: {
    present: boolean;
    min: number;
    max: number;
    mean: number;
    wrist_xyz_frame0: number[];
  };
  x_value_ranges: {
    lh_x_min: number;
    lh_x_max: number;
    rh_x_min: number;
    rh_x_max: number;
  };
}
