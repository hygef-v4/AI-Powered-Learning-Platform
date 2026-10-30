export type MaterialKind = "FILE" | "YOUTUBE_LINK";
export type TextStatus = "PENDING" | "EXTRACTED" | "NO_TEXT" | "FAILED";
export type MaterialVersion = {
  versionNo: number; fileName: string | null; mimeType: string | null; byteSize: number | null;
  videoUrl: string | null; videoId: string | null; textStatus: TextStatus | null;
  createdBy: string; createdAt: string;
};
export type Material = {
  id: string; scope: "SUBJECT" | "CLASS"; subjectId: string; classId: string | null; moduleId: string;
  kind: MaterialKind; title: string; description: string; orderNo: number;
  status: "ACTIVE" | "ARCHIVED"; currentVersionNo: number; version: number;
  updatedAt: string; canManage: boolean; current: MaterialVersion; versions: MaterialVersion[];
};
export type MaterialSource = { fileRef?: string; videoUrl?: string };
export type CreateMaterial = { kind: MaterialKind; title: string; description: string; orderNo: number; source: MaterialSource; moduleId: string };
export type QuizStatus = "DRAFT" | "REVIEWED" | "OPEN" | "RETIRED";
/** Quiz gắn trong module; `canManage`: người xem gỡ được khỏi module. */
export type ModuleQuiz = { quizId: string; title: string; status: QuizStatus; scope: "SUBJECT" | "CLASS"; classId: string | null; canManage: boolean };
/** Module của môn (U13); học liệu thuộc module qua `Material.moduleId`. */
export type MaterialModule = { id: string; title: string; orderNo: number; version: number; canManage: boolean; quizzes: ModuleQuiz[] };
/** Nơi xem học liệu: trang môn (Chủ nhiệm môn), lớp dạy (giảng viên) hay lớp học (sinh viên). */
export type ModuleScope = { kind: "subject"; subjectId: string } | { kind: "class"; classId: string } | { kind: "student"; classId: string };
export type DownloadToken = { url: string; expiresAt: string };
export const textLabels: Record<TextStatus, string> = {
  PENDING: "Đang xử lý", EXTRACTED: "Đã trích", NO_TEXT: "Không có chữ", FAILED: "Lỗi",
};
export function fileDescription(m: Material): string {
  if (m.kind === "YOUTUBE_LINK") return "YouTube";
  const type = m.current.fileName?.split(".").pop()?.toUpperCase() ?? "Tệp";
  const bytes = m.current.byteSize;
  if (bytes && bytes < 1024 * 1024) return `${type} · ${Math.max(1, Math.round(bytes / 1024))} KB`;
  return bytes ? `${type} · ${(bytes / 1024 / 1024).toLocaleString("vi-VN", { maximumFractionDigits: 1 })} MB` : type;
}
export function materialHref(id: string, classId?: string, view = false) {
  return `/materials/${encodeURIComponent(id)}${view ? "/view" : ""}${classId ? `?classId=${encodeURIComponent(classId)}` : ""}`;
}
