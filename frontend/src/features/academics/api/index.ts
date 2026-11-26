import { ApiError, fetchApi } from "@/shared/api/client";
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
import { msg } from "@/i18n/msg";

export type Scope = "me" | "teaching" | "manager";
export type ListQuery = {
  page?: number;
  size?: number;
  q?: string;
  status?: string;
  subjectId?: string;
  term?: string;
};
export type ManagedSubject = SubjectView & {
  status?: string;
  classCounts?: Partial<Record<ClassStatus, number>>;
};

const API_BASE = "/api/v1/";
const encodeId = encodeURIComponent;

// Chuẩn hóa trang, giới hạn tối đa 100 dòng và đưa bộ lọc vào query string.
function buildQuery(input: ListQuery = {}) {
  const page = Math.max(1, Math.floor(input.page || 1));
  const size = Math.max(1, Math.min(100, Math.floor(input.size || 100)));
  const params = new URLSearchParams({ page: String(page), size: String(size) });

  for (const key of ["q", "status", "subjectId", "term"] as const) {
    const value = input[key];
    if (value) params.set(key, value);
  }
  return params.toString();
}

// Gửi lệnh ghi qua API chung với payload JSON và kiểu phản hồi của caller.
function writeRequest<T>(path: string, method: string, body: unknown) {
  return fetchApi<T>(API_BASE + path, { method, body: JSON.stringify(body) });
}

export const academicsApi = {
  // Đọc một trang danh sách lớp theo phạm vi sinh viên, giảng dạy hoặc quản lý.
  listClasses: (scope: Scope, input: ListQuery = {}, signal?: AbortSignal) =>
    fetchApi<CourseClassView[]>(`${API_BASE}${scope}/classes?${buildQuery(input)}`, { signal }),
  // Đọc chi tiết lớp theo đúng phạm vi của màn hình hiện tại.
  loadClass: (scope: Scope, id: string, signal?: AbortSignal) =>
    fetchApi<CourseClassView>(`${API_BASE}${scope}/classes/${encodeId(id)}`, { signal }),
  // Đọc một trang dữ liệu ghi danh của lớp với bộ lọc được truyền vào.
  listStudents: (
    scope: "teaching" | "manager",
    id: string,
    input: ListQuery = {},
    signal?: AbortSignal,
  ) =>
    fetchApi<EnrollmentView[]>(
      `${API_BASE}${scope}/classes/${encodeId(id)}/students?${buildQuery(input)}`,
      { signal },
    ),
  // Đọc một sinh viên trong lớp từ endpoint giảng dạy.
  loadStudent: (id: string, accountId: string, signal?: AbortSignal) =>
    fetchApi<EnrollmentView>(
      `${API_BASE}teaching/classes/${encodeId(id)}/students/${encodeId(accountId)}`,
      { signal },
    ),
  // Đọc các môn thuộc phạm vi quản lý của tài khoản hiện tại.
  listSubjects: (signal?: AbortSignal) =>
    fetchApi<ManagedSubject[]>(`${API_BASE}manager/subjects`, { signal }),
  // Đọc chi tiết môn trong phạm vi quản lý theo ID.
  loadSubject: (id: string, signal?: AbortSignal) =>
    fetchApi<ManagedSubject>(`${API_BASE}manager/subjects/${encodeId(id)}`, { signal }),
  // Tìm tối đa 20 ứng viên ghi danh theo từ khóa và lớp đích.
  listStudentCandidates: (id: string, q: string, signal?: AbortSignal) => {
    const params = new URLSearchParams({ q, limit: "20" });
    return fetchApi<PersonView[]>(
      `${API_BASE}manager/classes/${encodeId(id)}/student-candidates?${params}`,
      { signal },
    );
  },
  // Tìm tối đa 20 tài khoản ứng viên theo loại và từ khóa.
  listCandidates: (kind: "teacher" | "student", q: string, signal?: AbortSignal) => {
    const params = new URLSearchParams({ q, limit: "20" });
    return fetchApi<PersonView[]>(`${API_BASE}manager/${kind}-candidates?${params}`, { signal });
  },
  // Gửi dữ liệu tạo lớp và nhận bản lớp ban đầu từ máy chủ.
  createClass: (input: CreateClassInput) =>
    writeRequest<CourseClassView>("manager/classes", "POST", input),
  // Gửi thay đổi thông tin cho lớp hiện tại và nhận bản cập nhật.
  updateClassInfo: (id: string, input: ClassInfoInput) =>
    writeRequest<CourseClassView>(`manager/classes/${encodeId(id)}`, "PATCH", input),
  // Dùng version của lớp để phát hiện ghi đè khi đổi giảng viên.
  assignTeacher: (id: string, teacherId: string | null, version: number) =>
    writeRequest<CourseClassView>(`manager/classes/${encodeId(id)}/teacher`, "PUT", {
      teacherId,
      version,
    }),
  // Yêu cầu đổi trạng thái lớp với version đang hiển thị.
  changeClassStatus: (id: string, status: ClassStatus, version: number) =>
    writeRequest<CourseClassView>(`manager/classes/${encodeId(id)}/status`, "PUT", {
      status,
      version,
    }),
  // Ghi danh theo ID tài khoản và đổi kết quả không thành công thành lỗi UI.
  enrollStudent: async (id: string, accountId: string) => {
    const response = await writeRequest<{ result: EnrollmentResult }>(
      `manager/classes/${encodeId(id)}/enrollments`,
      "POST",
      { accountId },
    );
    if (!["ENROLLED", "RESTORED", "ALREADY_ENROLLED"].includes(response.result)) {
      throw new Error(ENROLLMENT_RESULT_LABELS[response.result] ?? msg("academics.api.index.couldNotEnrollTheStudent"));
    }
    return response;
  },
  // Yêu cầu ngừng hoặc khôi phục ghi danh của tài khoản trong lớp.
  changeEnrollmentStatus: (id: string, accountId: string, status: "ACTIVE" | "REMOVED") =>
    writeRequest<unknown>(
      `manager/classes/${encodeId(id)}/enrollments/${encodeId(accountId)}/status`,
      "PUT",
      { status },
    ),
  // Gửi danh sách nguyên văn và nhận kết quả riêng cho từng dòng.
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
