// Gọi API backend: luôn gửi cookie phiên (HttpOnly), đọc lỗi dạng problem-details.

export type FieldError = { field: string; message: string };

export type ProblemDetails = {
  type?: string;
  title?: string;
  status: number;
  code?: string;
  correlationId?: string;
  errors?: FieldError[];
  [key: string]: unknown;
};

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly problem: ProblemDetails;

  constructor(problem: ProblemDetails) {
    super(problem.title ?? "Hệ thống gặp lỗi, vui lòng thử lại sau.");
    this.name = "ApiError";
    this.status = problem.status;
    this.code = problem.code ?? "UNKNOWN";
    this.problem = problem;
  }
}

export type ApiRequestInit = Omit<RequestInit, "body"> & {
  body?: unknown;
  idempotencyKey?: string;
};

const API_BASE = "/api/v1";

export async function apiFetch<T>(path: string, init: ApiRequestInit = {}): Promise<T> {
  const { body, idempotencyKey, headers, ...rest } = init;
  const finalHeaders = new Headers(headers);
  finalHeaders.set("Accept", "application/json");

  let payload: BodyInit | undefined;
  if (body instanceof FormData) {
    payload = body;
  } else if (body !== undefined) {
    finalHeaders.set("Content-Type", "application/json");
    payload = JSON.stringify(body);
  }
  if (idempotencyKey) finalHeaders.set("Idempotency-Key", idempotencyKey);

  const response = await fetch(`${API_BASE}${path}`, {
    ...rest,
    headers: finalHeaders,
    body: payload,
    credentials: "include",
  });

  if (!response.ok) {
    throw new ApiError(await readProblem(response));
  }
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

async function readProblem(response: Response): Promise<ProblemDetails> {
  const contentType = response.headers.get("Content-Type") ?? "";
  if (contentType.includes("json")) {
    try {
      const data = (await response.json()) as Partial<ProblemDetails>;
      return { ...data, status: response.status };
    } catch {
      // Body hỏng: rơi xuống thông điệp chung.
    }
  }
  return { status: response.status, code: "HTTP_" + response.status };
}
