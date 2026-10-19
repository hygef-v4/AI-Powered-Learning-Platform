// Gọi API backend cùng origin (/api/v1/...). Phiên nằm trong cookie HttpOnly do U01 cấp.
// Request đổi trạng thái gửi kèm header CSRF đọc từ cookie XSRF-TOKEN (U01, Q16).
// Gặp 401 thì thử refresh phiên một lần rồi gửi lại; refresh lỗi thì gọi handler do U01 đặt (chuyển về đăng nhập).

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

const CSRF_COOKIE = "XSRF-TOKEN";
const CSRF_HEADER = "X-XSRF-TOKEN";
const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS"]);
const AUTH_PREFIX = "/api/v1/auth/";

let refreshing: Promise<boolean> | null = null;
let onUnauthenticated: (() => void) | null = null;

/** U01 đặt hành động khi phiên hết hạn hẳn (refresh cũng lỗi), ví dụ chuyển về /login. */
export function setUnauthenticatedHandler(handler: (() => void) | null) {
  onUnauthenticated = handler;
}

function readCookie(name: string): string | undefined {
  if (typeof document === "undefined") return undefined;
  const prefix = `${name}=`;
  const found = document.cookie.split("; ").find((part) => part.startsWith(prefix));
  return found ? decodeURIComponent(found.slice(prefix.length)) : undefined;
}

/**
 * Xoay refresh token; nhiều request cùng gặp 401 dùng chung một lần refresh. 409 là tab khác vừa xoay cùng token:
 * cookie mới đã được đặt, coi như refresh thành công để gửi lại request.
 */
export function refreshSession(): Promise<boolean> {
  refreshing ??= fetch(`${AUTH_PREFIX}refresh`, { method: "POST", credentials: "include" })
    .then((response) => response.ok || response.status === 409)
    .catch(() => false)
    .finally(() => {
      refreshing = null;
    });
  return refreshing;
}

async function send(path: string, init: RequestInit): Promise<Response> {
  const headers = new Headers(init.headers);
  if (init.body !== undefined && !(init.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  headers.set("Accept", "application/json");
  const method = (init.method ?? "GET").toUpperCase();
  const csrf = readCookie(CSRF_COOKIE);
  if (!SAFE_METHODS.has(method) && csrf && !headers.has(CSRF_HEADER)) {
    headers.set(CSRF_HEADER, csrf);
  }
  return fetch(path, { ...init, headers, credentials: "include" });
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response = await send(path, init);
  if (response.status === 401 && !path.startsWith(AUTH_PREFIX)) {
    if (await refreshSession()) {
      response = await send(path, init);
    }
    if (response.status === 401) onUnauthenticated?.();
  }
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
