// Gọi API backend cùng origin (/api/v1/...). Phiên nằm trong cookie HttpOnly do U01 cấp.
// Request đổi trạng thái gửi kèm header CSRF đọc từ cookie XSRF-TOKEN (U01, Q16).
// Gặp 401 thì thử refresh phiên rồi gửi lại; phiên hết hẳn thì gọi handler do U01 đặt (chuyển về đăng nhập).
// Backend bận (503, mất mạng) khi refresh thì báo "hệ thống tạm bận", không coi là hết phiên.

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
export const CSRF_HEADER = "X-XSRF-TOKEN";
const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS"]);
const AUTH_PREFIX = "/api/v1/auth/";
/** Chờ response refresh của tab kia đặt cookie mới trước khi gửi lại (refresh trả 409). */
export const CONCURRENT_REFRESH_WAIT_MS = 500;
const SYSTEM_BUSY: ApiProblem = { status: 503, code: "SYSTEM_BUSY", detail: "Hệ thống tạm bận, vui lòng thử lại sau" };

/** `concurrent`: tab khác vừa xoay cùng refresh token (409), phiên vẫn còn; cookie mới do response của tab kia đặt. */
export type RefreshOutcome = "refreshed" | "concurrent" | "expired" | "unavailable";

let refreshing: Promise<RefreshOutcome> | null = null;
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

export function getCsrfToken(): string | undefined {
  return readCookie(CSRF_COOKIE);
}

function delay(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function outcomeOf(response: Response): RefreshOutcome {
  if (response.ok) return "refreshed";
  if (response.status === 409) return "concurrent";
  return response.status >= 500 ? "unavailable" : "expired";
}

/**
 * Xoay refresh token; nhiều request cùng gặp 401 trong một tab dùng chung một lần refresh. 409 nghĩa là tab khác vừa
 * xoay cùng token và cookie mới có thể chưa về: chờ một chút rồi mới cho gửi lại.
 */
export function refreshSession(): Promise<RefreshOutcome> {
  refreshing ??= fetch(`${AUTH_PREFIX}refresh`, { method: "POST", credentials: "include" })
    .then(async (response) => {
      const outcome = outcomeOf(response);
      if (outcome === "concurrent") await delay(CONCURRENT_REFRESH_WAIT_MS);
      return outcome;
    })
    .catch((): RefreshOutcome => "unavailable")
    .finally(() => {
      refreshing = null;
    });
  return refreshing;
}

/** Refresh rồi gửi lại request bị 401; trả response cuối (vẫn 401 nếu phiên đã hết hẳn). */
async function resendAfterRefresh(path: string, init: RequestInit, unauthorized: Response): Promise<Response> {
  let response = unauthorized;
  let outcome = await refreshSession();
  if (outcome !== "expired" && outcome !== "unavailable") {
    response = await send(path, init);
    if (response.status === 401 && outcome === "concurrent") {
      // Cookie mới của tab kia về muộn hơn thời gian chờ: refresh lại bằng cookie hiện có.
      outcome = await refreshSession();
      if (outcome === "refreshed") response = await send(path, init);
    }
  }
  if (outcome === "unavailable") throw new ApiError(SYSTEM_BUSY);
  return response;
}

async function send(path: string, init: RequestInit): Promise<Response> {
  const headers = new Headers(init.headers);
  if (init.body !== undefined && !(init.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (!headers.has("Accept")) headers.set("Accept", "application/json");
  const method = (init.method ?? "GET").toUpperCase();
  const csrf = getCsrfToken();
  if (!SAFE_METHODS.has(method) && csrf && !headers.has(CSRF_HEADER)) {
    headers.set(CSRF_HEADER, csrf);
  }
  return fetch(path, { ...init, headers, credentials: "include" });
}

async function apiResponse(path: string, init: RequestInit = {}): Promise<Response> {
  let response = await send(path, init);
  if (response.status === 401 && !path.startsWith(AUTH_PREFIX)) {
    response = await resendAfterRefresh(path, init, response);
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
  return response;
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await apiResponse(path, init);
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

/** Binary downloads share the same authentication, refresh, CSRF and error handling as JSON requests. */
export async function apiDownload(path: string, init: RequestInit = {}): Promise<Blob> {
  const headers = new Headers(init.headers);
  if (!headers.has("Accept")) headers.set("Accept", "application/octet-stream");
  return (await apiResponse(path, { ...init, headers })).blob();
}
