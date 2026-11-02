import { apiFetch, ApiError } from "@/shared/api/client";
import type { CreateMaterial, DownloadToken, Material, MaterialModule, MaterialSource, ModuleScope } from "../model/types";
const enc = encodeURIComponent;
export const scopePath = (id: string, kind: "subjects" | "classes") => `/api/v1/${kind}/${enc(id)}/materials`;
export const materialPath = (id: string) => `/api/v1/materials/${enc(id)}`;
const modulePath = (id: string) => `/api/v1/material-modules/${enc(id)}`;
/** Danh sách module và danh sách học liệu theo nơi xem. */
export function outlinePaths(scope: ModuleScope): { modules: string; materials: string } {
  if (scope.kind === "subject") return { modules: `/api/v1/subjects/${enc(scope.subjectId)}/modules`, materials: scopePath(scope.subjectId, "subjects") };
  const base = scope.kind === "student" ? `/api/v1/me/classes/${enc(scope.classId)}` : `/api/v1/classes/${enc(scope.classId)}`;
  return { modules: `${base}/modules`, materials: `${base}/materials` };
}
export function contextQuery(classId?: string) { return classId ? `?classId=${enc(classId)}` : ""; }
function write<T>(path: string, method: string, body?: unknown) {
  return apiFetch<T>(path, { method, ...(body === undefined ? {} : { body: JSON.stringify(body) }) });
}
export const materialApi = {
  create: (path: string, input: CreateMaterial) => write<Material>(path, "POST", input),
  update: (m: Material, title: string, description: string, moduleId?: string) => write<Material>(materialPath(m.id), "PATCH", { title, description, orderNo: m.orderNo, version: m.version, moduleId: moduleId ?? m.moduleId }),
  replace: (m: Material, source: MaterialSource) => write<Material>(`${materialPath(m.id)}/versions`, "POST", { version: m.version, source }),
  archive: (m: Material) => write<Material>(`${materialPath(m.id)}/archive`, "POST", { version: m.version }),
  reorder: (path: string, items: Material[]) => write<void>(`${path}:reorder`, "PUT", { items: items.map(({ id, version }) => ({ id, version })) }),
  token: (id: string, classId?: string, versionNo?: number) => {
    const params = new URLSearchParams(); if (classId) params.set("classId", classId); if (versionNo !== undefined) params.set("versionNo", String(versionNo));
    return write<DownloadToken>(`${materialPath(id)}/download-token${params.size ? `?${params}` : ""}`, "POST");
  },
};
export const moduleApi = {
  create: (subjectId: string, title: string) => write<MaterialModule>(`/api/v1/subjects/${enc(subjectId)}/modules`, "POST", { title }),
  rename: (m: MaterialModule, title: string) => write<void>(modulePath(m.id), "PATCH", { title, version: m.version }),
  remove: (m: MaterialModule) => write<void>(`${modulePath(m.id)}?version=${m.version}`, "DELETE"),
  reorder: (subjectId: string, items: MaterialModule[]) => write<void>(`/api/v1/subjects/${enc(subjectId)}/modules:reorder`, "PUT", { items: items.map(({ id, version }) => ({ id, version })) }),
  attachQuiz: (moduleId: string, quizId: string) => write<void>(`${modulePath(moduleId)}/quizzes`, "POST", { quizId }),
  detachQuiz: (moduleId: string, quizId: string) => write<void>(`${modulePath(moduleId)}/quizzes/${enc(quizId)}`, "DELETE"),
};
export function materialError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.problem.status === 409) return error.problem.code === "MATERIAL_VERSION_CONFLICT" || !error.problem.detail ? "Học liệu đã thay đổi hoặc chỉ đọc. Tải lại dữ liệu trước khi tiếp tục." : error.problem.detail;
    if (error.problem.status === 404) return "Không tìm thấy học liệu hoặc bạn không có quyền truy cập.";
    if (error.problem.status === 401) return "Phiên đăng nhập đã hết hạn.";
    return error.message;
  }
  return "Không tải được học liệu, vui lòng thử lại.";
}
const WATCH_HOSTS = ["youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com", "youtube-nocookie.com", "www.youtube-nocookie.com"];
/**
 * Link chỉ ra một video YouTube (cùng luật với backend): watch, youtu.be, Shorts, embed, live, bản di động, thiếu "https://"
 * hay dùng http đều nhận; phần danh sách phát, thời điểm phát bị bỏ qua. Link chỉ là danh sách phát thì không nhận.
 */
export function validYoutube(value: string): boolean {
  try {
    const text = value.trim();
    const url = new URL(text.includes("://") ? text : `https://${text}`);
    if (!["https:", "http:"].includes(url.protocol) || url.username || url.password || url.port) return false;
    if (url.searchParams.getAll("v").length > 1) return false;
    const host = url.hostname.toLowerCase();
    const id = host === "youtu.be" ? url.pathname.replace(/^\/|\/$/g, "")
      : WATCH_HOSTS.includes(host) ? (/^\/watch\/?$/.test(url.pathname) ? url.searchParams.get("v") : /^\/(?:shorts|embed|live|v)\/([A-Za-z0-9_-]{11})\/?$/.exec(url.pathname)?.[1] ?? null)
      : null;
    return !!id && /^[A-Za-z0-9_-]{11}$/.test(id);
  } catch { return false; }
}
