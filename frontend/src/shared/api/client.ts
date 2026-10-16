// Gọi API backend cùng origin (/api/v1/...). Phiên nằm trong cookie HttpOnly do U01 cấp.

export type ApiProblem = {
  status: number;
  title?: string;
  detail?: string;
  code?: string;
};

export class ApiError extends Error {
  readonly problem: ApiProblem;

  constructor(problem: ApiProblem) {
    super(problem.detail ?? problem.title ?? `HTTP ${problem.status}`);
    this.name = "ApiError";
    this.problem = problem;
  }
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body !== undefined && !(init.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  headers.set("Accept", "application/json");

  const response = await fetch(path, { ...init, headers, credentials: "include" });
  if (!response.ok) {
    let problem: ApiProblem = { status: response.status };
    try {
      problem = { ...(await response.json()), status: response.status };
    } catch {
      // Body không phải JSON: giữ mã HTTP.
    }
    throw new ApiError(problem);
  }
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}
