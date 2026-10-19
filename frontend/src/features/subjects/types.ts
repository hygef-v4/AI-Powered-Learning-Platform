import type { ApiProblem } from "@/shared/api/client";

// DTO web đề xuất cho B11; không thay SubjectRef của contract Java dùng chung.
export type SubjectStatus = "ACTIVE" | "ARCHIVED";
export type SubjectClassStatus = "DRAFT" | "OPEN" | "ARCHIVED";

export type SubjectManager = {
  accountId: string;
  displayName: string;
  email: string;
};

export type SubjectListItem = {
  id: string;
  code: string;
  name: string;
  status: SubjectStatus;
  managerId: string | null;
  // managerId khác null nhưng manager=null: chưa tra được contact, không phải chưa phân công.
  manager: SubjectManager | null;
  classCount: number | null; // null = không khả dụng, 0 = đã xác minh không có lớp.
};

export type SubjectDetail = SubjectListItem & {
  description: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
};

export type SubjectListQuery = {
  q?: string;
  status?: SubjectStatus;
  page?: number;
  size?: number;
};

export type SubjectPage = {
  items: SubjectListItem[];
  page: number;
  size: number;
  totalElements: number;
  statusCounts: Record<SubjectStatus, number>;
};

export type SubjectClassSummary = {
  classId: string;
  code: string;
  name: string;
  term: string;
  status: SubjectClassStatus;
  teacherName: string | null;
  activeStudentCount: number; // Giữ contract U05; UI Admin không có cột sĩ số (Q07).
};

export type SubjectClasses =
  | { availability: "AVAILABLE"; items: SubjectClassSummary[] }
  | { availability: "UNAVAILABLE"; items: null };

export type CreateSubjectRequest = {
  code: string;
  name: string;
  description?: string | null;
  managerId?: string | null;
};

export type UpdateSubjectRequest = {
  name: string;
  description: string | null;
  version: number;
};

export type ChangeSubjectManagerRequest = { managerId: string | null; version: number };
export type ChangeSubjectStatusRequest = { status: SubjectStatus; version: number };

// apiFetch đã giữ các extension JSON trong ApiError.problem; không sửa shared auth/client của U01.
export type SubjectApiProblem = ApiProblem & {
  fieldErrors?: Partial<Record<"code" | "name" | "description" | "managerId" | "version", string>>;
};
