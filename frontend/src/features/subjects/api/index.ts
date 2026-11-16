import { fetchApi } from "@/shared/api/client";
import type {
  ChangeSubjectManagerRequest,
  ChangeSubjectStatusRequest,
  CreateSubjectRequest,
  SubjectClasses,
  SubjectDetail,
  SubjectListQuery,
  SubjectPage,
  UpdateSubjectRequest,
} from "../model/types";

const BASE = "/api/v1/admin/subjects";
const buildSubjectPath = (id: string) => `${BASE}/${encodeURIComponent(id)}`;

export function listSubjects(query: SubjectListQuery = {}): Promise<SubjectPage> {
  const params = new URLSearchParams();
  if (query.q?.trim()) params.set("q", query.q.trim());
  if (query.status) params.set("status", query.status);
  if (query.page !== undefined) params.set("page", String(query.page));
  if (query.size !== undefined) params.set("size", String(query.size));
  return fetchApi<SubjectPage>(`${BASE}${params.size ? `?${params}` : ""}`);
}

export function loadSubject(id: string): Promise<SubjectDetail> {
  return fetchApi<SubjectDetail>(buildSubjectPath(id));
}

export function listSubjectClasses(id: string): Promise<SubjectClasses> {
  return fetchApi<SubjectClasses>(`${buildSubjectPath(id)}/classes`);
}

export function createSubject(request: CreateSubjectRequest): Promise<SubjectDetail> {
  // Whitelist field ở boundary client; server B11 vẫn phải validate/reject field lạ độc lập.
  const { code, name, description, managerId } = request;
  return fetchApi<SubjectDetail>(BASE, {
    method: "POST",
    body: JSON.stringify({ code, name, description, managerId }),
  });
}

export function updateSubject(id: string, request: UpdateSubjectRequest): Promise<SubjectDetail> {
  const { name, description, version } = request;
  return fetchApi<SubjectDetail>(buildSubjectPath(id), {
    method: "PATCH",
    body: JSON.stringify({ name, description, version }),
  });
}

export function changeSubjectManager(
  id: string,
  request: ChangeSubjectManagerRequest,
): Promise<SubjectDetail> {
  const { managerId, version } = request;
  return fetchApi<SubjectDetail>(`${buildSubjectPath(id)}/manager`, {
    method: "PUT",
    body: JSON.stringify({ managerId, version }),
  });
}

export function changeSubjectStatus(
  id: string,
  request: ChangeSubjectStatusRequest,
): Promise<SubjectDetail> {
  const { status, version } = request;
  return fetchApi<SubjectDetail>(`${buildSubjectPath(id)}/status`, {
    method: "PUT",
    body: JSON.stringify({ status, version }),
  });
}
