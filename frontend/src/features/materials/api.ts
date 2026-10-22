import { apiFetch, ApiError } from "@/shared/api/client";
import type { CreateMaterial, DownloadToken, Material, MaterialSource } from "./types";
const enc = encodeURIComponent;
export const scopePath = (id: string, kind: "subjects" | "classes") => `/api/v1/${kind}/${enc(id)}/materials`;
export const materialPath = (id: string) => `/api/v1/materials/${enc(id)}`;
export function contextQuery(classId?: string) { return classId ? `?classId=${enc(classId)}` : ""; }
function write<T>(path: string, method: string, body?: unknown) {
  return apiFetch<T>(path, { method, ...(body === undefined ? {} : { body: JSON.stringify(body) }) });
}
export const materialApi = {
  create: (path: string, input: CreateMaterial) => write<Material>(path, "POST", input),
  update: (m: Material, title: string, description: string) => write<Material>(materialPath(m.id), "PATCH", { title, description, orderNo: m.orderNo, version: m.version }),
  replace: (m: Material, source: MaterialSource) => write<Material>(`${materialPath(m.id)}/versions`, "POST", { version: m.version, source }),
  archive: (m: Material) => write<Material>(`${materialPath(m.id)}/archive`, "POST", { version: m.version }),
  reorder: (path: string, items: Material[]) => write<void>(`${path}:reorder`, "PUT", { items: items.map(({ id, version }) => ({ id, version })) }),
  token: (id: string, classId?: string, versionNo?: number) => {
    const params = new URLSearchParams(); if (classId) params.set("classId", classId); if (versionNo !== undefined) params.set("versionNo", String(versionNo));
    return write<DownloadToken>(`${materialPath(id)}/download-token${params.size ? `?${params}` : ""}`, "POST");
  },
};
export function materialError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.problem.status === 409) return "Học liệu đã thay đổi hoặc chỉ đọc. Tải lại dữ liệu trước khi tiếp tục.";
    if (error.problem.status === 404) return "Không tìm thấy học liệu hoặc bạn không có quyền truy cập.";
    if (error.problem.status === 401) return "Phiên đăng nhập đã hết hạn.";
    return error.message;
  }
  return "Không tải được học liệu, vui lòng thử lại.";
}
export function validYoutube(value: string): boolean {
  try {
    const url = new URL(value.trim());
    if (url.protocol !== "https:" || url.username || url.password || url.port || url.searchParams.has("list") || url.searchParams.has("index")) return false;
    const keys = [...url.searchParams.keys()]; if (new Set(keys).size !== keys.length) return false;
    const id = url.hostname === "youtu.be" ? url.pathname.slice(1)
      : ["youtube.com", "www.youtube.com"].includes(url.hostname) && url.pathname === "/watch" ? url.searchParams.get("v") : null;
    return !!id && /^[A-Za-z0-9_-]{11}$/.test(id);
  } catch { return false; }
}
