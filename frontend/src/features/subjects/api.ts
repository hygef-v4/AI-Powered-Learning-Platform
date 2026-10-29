import { apiFetch } from "@/shared/api/client";
import type {
  ChangeSubjectManagerRequest,
  ChangeSubjectStatusRequest,
  CreateSubjectRequest,
  SubjectClasses,
  SubjectDetail,
  SubjectListQuery,
  SubjectPage,
  UpdateSubjectRequest,
} from "./types";

const BASE = "/api/v1/admin/subjects";
const subjectPath = (id: string) => `${BASE}/${encodeURIComponent(id)}`;

export function getSubjects(query: SubjectListQuery = {}): Promise<SubjectPage> {
  const params = new URLSearchParams();
  if (query.q?.trim()) params.set("q", query.q.trim());
  if (query.status) params.set("status", query.status);
  if (query.page !== undefined) params.set("page", String(query.page));
  if (query.size !== undefined) params.set("size", String(query.size));
  return apiFetch<SubjectPage>(`${BASE}${params.size ? `?${params}` : ""}`);
}

export function getSubject(id: string): Promise<SubjectDetail> {
  return apiFetch<SubjectDetail>(subjectPath(id));
}

export function getSubjectClasses(id: string): Promise<SubjectClasses> {
  return apiFetch<SubjectClasses>(`${subjectPath(id)}/classes`);
}

export function createSubject(request: CreateSubjectRequest): Promise<SubjectDetail> {
  // Whitelist field ở boundary client; server B11 vẫn phải validate/reject field lạ độc lập.
  const { code, name, description, managerId } = request;
  return apiFetch<SubjectDetail>(BASE, {
    method: "POST",
    body: JSON.stringify({ code, name, description, managerId }),
  });
}

export function updateSubject(id: string, request: UpdateSubjectRequest): Promise<SubjectDetail> {
  const { name, description, version } = request;
  return apiFetch<SubjectDetail>(subjectPath(id), {
    method: "PATCH",
    body: JSON.stringify({ name, description, version }),
  });
}

export function changeSubjectManager(id: string, request: ChangeSubjectManagerRequest): Promise<SubjectDetail> {
  const { managerId, version } = request;
  return apiFetch<SubjectDetail>(`${subjectPath(id)}/manager`, {
    method: "PUT",
    body: JSON.stringify({ managerId, version }),
  });
}

export function changeSubjectStatus(id: string, request: ChangeSubjectStatusRequest): Promise<SubjectDetail> {
  const { status, version } = request;
  return apiFetch<SubjectDetail>(`${subjectPath(id)}/status`, {
    method: "PUT",
    body: JSON.stringify({ status, version }),
  });
}
