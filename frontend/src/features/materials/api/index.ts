import { fetchApi, ApiError } from "@/shared/api/client";
import type { FormattedText } from "@/shared/document/formattedText";
import type {
  CreateMaterial,
  DownloadToken,
  Material,
  MaterialModule,
  MaterialSource,
  ModuleScope,
} from "../model/types";
import { msg } from "@/i18n/msg";
const encodePathSegment = encodeURIComponent;
export const buildScopePath = (id: string, kind: "subjects" | "classes") =>
  `/api/v1/${kind}/${encodePathSegment(id)}/materials`;
export const buildMaterialPath = (id: string) => `/api/v1/materials/${encodePathSegment(id)}`;
const buildModulePath = (id: string) => `/api/v1/material-modules/${encodePathSegment(id)}`;
/** Danh sách module và danh sách học liệu theo nơi xem. */
export function buildOutlinePaths(scope: ModuleScope): { modules: string; materials: string } {
  if (scope.kind === "subject")
    return {
      modules: `/api/v1/subjects/${encodePathSegment(scope.subjectId)}/modules`,
      materials: buildScopePath(scope.subjectId, "subjects"),
    };
  const base =
    scope.kind === "student"
      ? `/api/v1/me/classes/${encodePathSegment(scope.classId)}`
      : `/api/v1/classes/${encodePathSegment(scope.classId)}`;
  return { modules: `${base}/modules`, materials: `${base}/materials` };
}
export function buildContextQuery(classId?: string) {
  return classId ? `?classId=${encodePathSegment(classId)}` : "";
}
function sendWrite<T>(path: string, method: string, body?: unknown) {
  return fetchApi<T>(path, {
    method,
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
}
export const materialApi = {
  createMaterial: (path: string, input: CreateMaterial) => sendWrite<Material>(path, "POST", input),
  updateMaterial: (m: Material, title: string, description: string, moduleId?: string,
    options?: { enabled?: boolean; descriptionDocument?: FormattedText | null }) =>
    sendWrite<Material>(buildMaterialPath(m.id), "PATCH", {
      title,
      description,
      orderNo: m.orderNo,
      version: m.version,
      moduleId: moduleId ?? m.moduleId,
      ...options,
    }),
  replaceMaterialVersion: (m: Material, source: MaterialSource) =>
    sendWrite<Material>(`${buildMaterialPath(m.id)}/versions`, "POST", { version: m.version, source }),
  archiveMaterial: (m: Material) =>
    sendWrite<Material>(`${buildMaterialPath(m.id)}/archive`, "POST", { version: m.version }),
  reorderMaterials: (path: string, items: Material[]) =>
    sendWrite<void>(`${path}:reorder`, "PUT", {
      items: items.map(({ id, version }) => ({ id, version })),
    }),
  issueDownloadToken: (id: string, classId?: string, versionNo?: number) => {
    const params = new URLSearchParams();
    if (classId) params.set("classId", classId);
    if (versionNo !== undefined) params.set("versionNo", String(versionNo));
    return sendWrite<DownloadToken>(
      `${buildMaterialPath(id)}/download-token${params.size ? `?${params}` : ""}`,
      "POST",
    );
  },
};
export const moduleApi = {
  createModule: (subjectId: string, title: string) =>
    sendWrite<MaterialModule>(`/api/v1/subjects/${encodePathSegment(subjectId)}/modules`, "POST", { title }),
  renameModule: (m: MaterialModule, title: string) =>
    sendWrite<void>(buildModulePath(m.id), "PATCH", { title, version: m.version }),
  deleteModule: (m: MaterialModule) => sendWrite<void>(`${buildModulePath(m.id)}?version=${m.version}`, "DELETE"),
  reorderModules: (subjectId: string, items: MaterialModule[]) =>
    sendWrite<void>(`/api/v1/subjects/${encodePathSegment(subjectId)}/modules:reorder`, "PUT", {
      items: items.map(({ id, version }) => ({ id, version })),
    }),
  attachQuiz: (moduleId: string, quizId: string) =>
    sendWrite<void>(`${buildModulePath(moduleId)}/quizzes`, "POST", { quizId }),
  detachQuiz: (moduleId: string, quizId: string) =>
    sendWrite<void>(`${buildModulePath(moduleId)}/quizzes/${encodePathSegment(quizId)}`, "DELETE"),
};
export function describeMaterialError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.problem.status === 409)
      return error.problem.code === "MATERIAL_VERSION_CONFLICT" || !error.problem.detail
        ? msg("materials.api.index.hocLieuDaThayDoiHoac")
        : error.problem.detail;
    if (error.problem.status === 404)
      return msg("materials.api.index.khongTimThayHocLieuHoac");
    if (error.problem.status === 401) return msg("materials.api.index.phienDangNhapDaHetHan");
    return error.message;
  }
  return msg("materials.api.index.khongTaiDuocHocLieuVui");
}
const WATCH_HOSTS = [
  "youtube.com",
  "www.youtube.com",
  "m.youtube.com",
  "music.youtube.com",
  "youtube-nocookie.com",
  "www.youtube-nocookie.com",
];
/**
 * Link chỉ ra một video YouTube (cùng luật với backend): watch, youtu.be, Shorts, embed, live, bản di động, thiếu "https://"
 * hay dùng http đều nhận; phần danh sách phát, thời điểm phát bị bỏ qua. Link chỉ là danh sách phát thì không nhận.
 */
export function isValidYoutubeUrl(value: string): boolean {
  try {
    const text = value.trim();
    const url = new URL(text.includes("://") ? text : `https://${text}`);
    if (!["https:", "http:"].includes(url.protocol) || url.username || url.password || url.port)
      return false;
    if (url.searchParams.getAll("v").length > 1) return false;
    const host = url.hostname.toLowerCase();
    const id =
      host === "youtu.be"
        ? url.pathname.replace(/^\/|\/$/g, "")
        : WATCH_HOSTS.includes(host)
          ? /^\/watch\/?$/.test(url.pathname)
            ? url.searchParams.get("v")
            : (/^\/(?:shorts|embed|live|v)\/([A-Za-z0-9_-]{11})\/?$/.exec(url.pathname)?.[1] ??
              null)
          : null;
    return !!id && /^[A-Za-z0-9_-]{11}$/.test(id);
  } catch {
    return false;
  }
}
