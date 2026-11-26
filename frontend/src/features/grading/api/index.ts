import { fetchApi, downloadApiFile } from "@/shared/api/client";
import type { NavigationFilters } from "../model/navigation";
import type { ContributionLevel } from "../model/contributionLevels";
import type {
  AiRequest,
  AssignmentContext,
  AiEstimate,
  BatchResult,
  Detail,
  Evaluation,
  GradebookPage,
  GradebookRow,
  ItemVersion,
  Proposal,
  Save,
  StudentGrade,
  SubmissionPage,
} from "../model/types";

const API_ROOT = "/api/v1";

export type GradeDistributionVisibility = { enabled: boolean; version: number; canEdit: boolean };

// Dựng URL cài đặt theo cặp lớp và bài tập đã mã hóa.
const buildVisibilityPath = (classId: string, assignmentId: string) =>
  `/classes/${encodeURIComponent(classId)}/assignments/${encodeURIComponent(assignmentId)}/grade-distribution-visibility`;

// Chuyển bộ lọc sang query string và bỏ các trường chưa xác định.
const buildQuery = (values: Record<string, string | number | undefined>) => {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(values)) {
    if (value !== undefined) params.set(key, String(value));
  }
  return params.toString();
};

// Gửi payload JSON tới API chấm và trả kiểu phản hồi mà caller yêu cầu.
const writeRequest = <T>(path: string, body: unknown, method = "POST") =>
  fetchApi<T>(API_ROOT + path, { method, body: JSON.stringify(body) });

export const gradingApi = {
  // Đọc thông tin bài tập để đặt bài nộp và rubric vào đúng ngữ cảnh.
  loadAssignmentContext: (assignmentId: string) =>
    fetchApi<AssignmentContext>(`${API_ROOT}/assignments/${encodeURIComponent(assignmentId)}`),
  // Đọc một trang 20 bài nộp theo trạng thái và bộ lọc nộp muộn.
  listSubmissions: (classId: string, assignmentId: string, status = "ALL", page = 1, late?: string) => {
    const params = buildQuery({ status, page, size: 20, late: late || undefined });
    return fetchApi<SubmissionPage>(
      `${API_ROOT}/classes/${classId}/assignments/${assignmentId}/submissions?${params}`,
    );
  },
  // Đọc tiến độ từng bài của lớp trong cùng một yêu cầu.
  loadSubmissionProgress: (classId: string) =>
    fetchApi<Record<string, SubmissionPage>>(`${API_ROOT}/classes/${classId}/submission-progress`),

  // Đọc bản chấm và giữ bộ lọc dùng để dựng điều hướng trước/sau.
  loadEvaluation: (id: string, filters: NavigationFilters = {}) => {
    const params = buildQuery(filters);
    return fetchApi<Detail>(`${API_ROOT}/evaluations/${id}${params ? `?${params}` : ""}`);
  },
  // Gửi rubric, phản hồi và version của lần sửa lên endpoint bản chấm.
  saveEvaluation: (id: string, body: Save) => writeRequest<Evaluation>(`/evaluations/${id}`, body, "PUT"),
  // Gửi các bản chấm cần chốt cùng version và nhận kết quả từng mục.
  finalizeGrades: (items: ItemVersion[]) => writeRequest<BatchResult>("/evaluations:finalize", { items }),
  // Công bố theo lô và nhận trạng thái riêng cho từng bản chấm.
  publishGrades: (items: ItemVersion[]) => writeRequest<BatchResult>("/evaluations:publish", { items }),
  // Lưu điểm riêng, lý do và mức đóng góp của thành viên bằng version hiện có.
  saveMemberGrade: (id: string, accountId: string, version: number, score: string,
    reason: string | null, contributionLevel?: ContributionLevel | null) =>
    writeRequest<Evaluation>(`/evaluations/${id}/members/${accountId}`,
      { version, score, reason, contributionLevel }, "PUT"),

  // Đọc trạng thái và lỗi an toàn của tác vụ AI để hiển thị tiến độ.
  loadTask: (id: string) =>
    fetchApi<{ state: string; safeError: string | null }>(
      `${API_ROOT}/ai/tasks/${encodeURIComponent(id)}`,
    ),
  // Gửi yêu cầu hủy tác vụ AI theo ID đã mã hóa.
  cancelTask: (id: string) =>
    fetchApi<void>(`${API_ROOT}/ai/tasks/${encodeURIComponent(id)}:cancel`, { method: "POST" }),
  // Đọc các đề xuất đã lưu cho bản chấm hiện tại.
  listProposals: (id: string) => fetchApi<Proposal[]>(`${API_ROOT}/evaluations/${id}/ai-proposals`),
  // Gửi yêu cầu chấm AI cho một bản chấm và nhận đề xuất theo dõi được.
  requestProposal: (id: string, input: AiRequest) =>
    writeRequest<Proposal>(`/evaluations/${id}/ai-proposals`, input),
  // Yêu cầu đề xuất theo lô với cấu hình AI dùng chung.
  requestProposals: (items: ItemVersion[], request: AiRequest) =>
    writeRequest<BatchResult>("/evaluations:ai-proposals", { items, request }),
  // Ước lượng token trước khi gửi các bản chấm đã chọn.
  estimateProposalTokens: (items: ItemVersion[]) =>
    writeRequest<AiEstimate>("/evaluations:ai-proposal-estimate", { items }),

  // Tải một trang sổ điểm lớp với từ khóa và kích thước trang yêu cầu.
  loadGradebook: (classId: string, q = "", page = 1, size = 20) =>
    fetchApi<GradebookPage>(`${API_ROOT}/classes/${classId}/gradebook?${buildQuery({ q, page, size })}`),
  // Tải tệp sổ điểm cho cả lớp hoặc một bài theo định dạng đã chọn.
  exportGrades: (classId: string, format: string, assignmentId?: string) =>
    downloadApiFile(`${API_ROOT}/classes/${classId}/gradebook/export?${buildQuery({ format, assignmentId })}`),
  // Đọc hàng điểm của một sinh viên từ phạm vi giảng dạy của lớp.
  loadStudentGrades: (classId: string, studentId: string) =>
    fetchApi<GradebookRow>(`${API_ROOT}/classes/${classId}/students/${studentId}/grades`),
  // Đọc điểm của tài khoản đang đăng nhập trong lớp được chọn.
  listMyGrades: (classId: string) => fetchApi<StudentGrade[]>(`${API_ROOT}/me/grades?classId=${classId}`),
  // Đọc điểm thuộc lần nộp cụ thể của tài khoản đang đăng nhập.
  loadAttemptGrade: (attemptId: string) => fetchApi<StudentGrade>(`${API_ROOT}/me/grades/attempts/${attemptId}`),

  // Đọc quyền hiển thị của phân phối theo đúng cặp lớp và bài.
  loadDistributionVisibility: (classId: string, assignmentId: string, signal?: AbortSignal) =>
    fetchApi<GradeDistributionVisibility>(
      API_ROOT + buildVisibilityPath(classId, assignmentId),
      { signal },
    ),
  // Gửi trạng thái hiển thị cùng version để phát hiện cập nhật trùng.
  saveDistributionVisibility: (
    classId: string,
    assignmentId: string,
    enabled: boolean,
    version: number,
  ) =>
    writeRequest<GradeDistributionVisibility>(
      buildVisibilityPath(classId, assignmentId),
      { enabled, version },
      "PUT",
    ),
  // Đọc histogram ở góc nhìn sinh viên khi bài và lớp đủ điều kiện hiển thị.
  loadMyGradeDistribution: (classId: string, assignmentId: string) =>
    fetchApi<{ label: string; count: number }[]>(
      `${API_ROOT}/me/grade-distributions?${buildQuery({ classId, assignmentId })}`,
    ),
  // Đọc histogram ở phạm vi lớp dành cho phía giảng dạy.
  loadClassGradeDistribution: (classId: string, assignmentId: string) =>
    fetchApi<{ label: string; count: number }[]>(
      `${API_ROOT}/classes/${classId}/grade-distribution?${buildQuery({ assignmentId })}`,
    ),
};
