import {
  BackendDebugEchoResponse,
  InferenceModel,
  PredictionResponse,
} from "../types/prediction";

function normalizeBaseUrl(input: string): string {
  return input.trim().replace(/\/+$/, "");
}

async function tryParseErrorBody(response: Response): Promise<string> {
  try {
    const text = await response.text();
    return text || `HTTP ${response.status}`;
  } catch {
    return `HTTP ${response.status}`;
  }
}

export async function checkHealth(baseUrl: string): Promise<boolean> {
  const target = `${normalizeBaseUrl(baseUrl)}/health`;
  try {
    const response = await fetch(target, { method: "GET" });
    return response.ok;
  } catch {
    return false;
  }
}

export async function predictFromFrames(
  baseUrl: string,
  frameUris: string[],
  model: InferenceModel
): Promise<PredictionResponse> {
  const normalized = normalizeBaseUrl(baseUrl);
  const endpoint = `${normalized}/predict-frames?model=${model}`;

  const formData = new FormData();
  frameUris.forEach((uri, index) => {
    formData.append("frames", {
      uri,
      name: `${index}.jpg`,
      type: "image/jpeg",
    } as any);
  });

  const response = await fetch(endpoint, {
    method: "POST",
    headers: {
      Accept: "application/json",
    },
    body: formData,
  });

  if (!response.ok) {
    const message = await tryParseErrorBody(response);
    throw new Error(`Prediction failed: ${message}`);
  }

  return (await response.json()) as PredictionResponse;
}

export async function predictFromLandmarks(
  baseUrl: string,
  landmarks: number[][],
  model: InferenceModel
): Promise<PredictionResponse> {
  const normalized = normalizeBaseUrl(baseUrl);
  const endpoint = `${normalized}/predict?model=${model}`;

  const response = await fetch(endpoint, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ landmarks }),
  });

  if (!response.ok) {
    const message = await tryParseErrorBody(response);
    throw new Error(`Landmark prediction failed: ${message}`);
  }

  return (await response.json()) as PredictionResponse;
}

export async function debugEchoLandmarks(
  baseUrl: string,
  landmarks: number[][]
): Promise<BackendDebugEchoResponse> {
  const normalized = normalizeBaseUrl(baseUrl);
  const endpoint = `${normalized}/debug-echo`;

  const response = await fetch(endpoint, {
    method: "POST",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ landmarks }),
  });

  if (!response.ok) {
    const message = await tryParseErrorBody(response);
    throw new Error(`Debug echo failed: ${message}`);
  }

  return (await response.json()) as BackendDebugEchoResponse;
}
