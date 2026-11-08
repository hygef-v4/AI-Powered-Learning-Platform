import { ApiError, apiFetch } from "@/shared/api/client";
import { ENROLLMENT_RESULT_LABELS } from "../model/types";
import type {
  ClassInfoInput,
  ClassStatus,
  CourseClassView,
  CreateClassInput,
  EnrollmentResult,
  EnrollmentRowResult,
  EnrollmentView,
  PersonView,
  SubjectView,
} from "../model/types";

export type Scope = "me" | "teaching" | "manager";
export type ListQuery = {
  page?: number;
  size?: number;
  q?: string;
  status?: string;
  subjectId?: string;
};
export type ManagedSubject = SubjectView & {
  status?: string;
  classCounts?: Partial<Record<ClassStatus, number>>;
};

const API_BASE = "/api/v1/";
const encodeId = encodeURIComponent;

function buildQuery(input: ListQuery = {}) {
  const page = Math.max(1, Math.floor(input.page || 1));
  const size = Math.max(1, Math.min(100, Math.floor(input.size || 100)));
  const params = new URLSearchParams({ page: String(page), size: String(size) });

  for (const key of ["q", "status", "subjectId"] as const) {
    const value = input[key];
    if (value) params.set(key, value);
  }
  return params.toString();
}

function writeRequest<T>(path: string, method: string, body: unknown) {
  return apiFetch<T>(API_BASE + path, { method, body: JSON.stringify(body) });
}

export const academicsApi = {
  listClasses: (scope: Scope, input: ListQuery = {}, signal?: AbortSignal) =>
    apiFetch<CourseClassView[]>(`${API_BASE}${scope}/classes?${buildQuery(input)}`, { signal }),
  loadClass: (scope: Scope, id: string, signal?: AbortSignal) =>
    apiFetch<CourseClassView>(`${API_BASE}${scope}/classes/${encodeId(id)}`, { signal }),
  listStudents: (
    scope: "teaching" | "manager",
    id: string,
    input: ListQuery = {},
    signal?: AbortSignal,
  ) =>
    apiFetch<EnrollmentView[]>(
      `${API_BASE}${scope}/classes/${encodeId(id)}/students?${buildQuery(input)}`,
      { signal },
    ),
  loadStudent: (id: string, accountId: string, signal?: AbortSignal) =>
    apiFetch<EnrollmentView>(
      `${API_BASE}teaching/classes/${encodeId(id)}/students/${encodeId(accountId)}`,
      { signal },
    ),
  listSubjects: (signal?: AbortSignal) =>
    apiFetch<ManagedSubject[]>(`${API_BASE}manager/subjects`, { signal }),
  loadSubject: (id: string, signal?: AbortSignal) =>
    apiFetch<ManagedSubject>(`${API_BASE}manager/subjects/${encodeId(id)}`, { signal }),
  listStudentCandidates: (id: string, q: string, signal?: AbortSignal) => {
    const params = new URLSearchParams({ q, limit: "20" });
    return apiFetch<PersonView[]>(
      `${API_BASE}manager/classes/${encodeId(id)}/student-candidates?${params}`,
      { signal },
    );
  },
  listCandidates: (kind: "teacher" | "student", q: string, signal?: AbortSignal) => {
    const params = new URLSearchParams({ q, limit: "20" });
    return apiFetch<PersonView[]>(`${API_BASE}manager/${kind}-candidates?${params}`, { signal });
  },
  createClass: (input: CreateClassInput) =>
    writeRequest<CourseClassView>("manager/classes", "POST", input),
  updateClassInfo: (id: string, input: ClassInfoInput) =>
    writeRequest<CourseClassView>(`manager/classes/${encodeId(id)}`, "PATCH", input),
  assignTeacher: (id: string, teacherId: string | null, version: number) =>
    writeRequest<CourseClassView>(`manager/classes/${encodeId(id)}/teacher`, "PUT", {
      teacherId,
      version,
    }),
  changeClassStatus: (id: string, status: ClassStatus, version: number) =>
    writeRequest<CourseClassView>(`manager/classes/${encodeId(id)}/status`, "PUT", {
      status,
      version,
    }),
  enrollStudent: async (id: string, accountId: string) => {
    const response = await writeRequest<{ result: EnrollmentResult }>(
      `manager/classes/${encodeId(id)}/enrollments`,
      "POST",
      { accountId },
    );
    if (!["ENROLLED", "RESTORED", "ALREADY_ENROLLED"].includes(response.result)) {
      throw new Error(ENROLLMENT_RESULT_LABELS[response.result] ?? "Không thể ghi danh sinh viên.");
    }
    return response;
  },
  changeEnrollmentStatus: (id: string, accountId: string, status: "ACTIVE" | "REMOVED") =>
    writeRequest<unknown>(
      `manager/classes/${encodeId(id)}/enrollments/${encodeId(accountId)}/status`,
      "PUT",
      { status },
    ),
  enrollStudents: (id: string, text: string) =>
    writeRequest<EnrollmentRowResult[]>(
      `manager/classes/${encodeId(id)}/enrollments:batch`,
      "POST",
      { text },
    ),
};

/** Resolve display mode only through successful, session-authorized endpoints. */
export async function loadClassCapability(signal?: AbortSignal) {
  try {
    return {
      audience: "student" as const,
      classes: await academicsApi.listClasses("me", {}, signal),
    };
  } catch (error) {
    if (!(error instanceof ApiError) || ![403, 404].includes(error.problem.status)) {
      throw error;
    }
  }
  return {
    audience: "teacher" as const,
    classes: await academicsApi.listClasses("teaching", {}, signal),
  };
}
