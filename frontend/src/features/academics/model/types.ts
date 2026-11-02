export type ClassStatus = "DRAFT" | "OPEN" | "ARCHIVED";
export type Audience = "student" | "teacher" | "manager";
export type PersonView = { id: string; name: string; email: string };
export type SubjectView = { id: string; code: string; name: string; description?: string };
export type CourseClassView = {
  id: string;
  subject: SubjectView;
  code: string;
  name: string;
  description: string;
  term: string;
  status: ClassStatus;
  teacher: PersonView | null;
  activeStudentCount: number;
  removedStudentCount: number;
  createdAt: string;
  showGradeDistribution: boolean;
  version: number;
};
export type EnrollmentView = {
  accountId: string;
  name: string;
  email: string;
  enrolledAt: string;
  status: "ACTIVE" | "REMOVED";
};
export type ClassInfoInput = Pick<CourseClassView, "name" | "description" | "term" | "version">;
export type CreateClassInput = Omit<ClassInfoInput, "version"> & { subjectId: string; code: string; teacherId: string | null };
export type EnrollmentResult = "ENROLLED" | "RESTORED" | "ALREADY_ENROLLED" | "NOT_FOUND" | "NOT_STUDENT" | "DISABLED" | "IN_OTHER_CLASS" | "INVALID_EMAIL" | "DUPLICATE_IN_LIST";
export type EnrollmentRowResult = { line: number; email: string; result: EnrollmentResult };
export const statusLabels: Record<ClassStatus, string> = { DRAFT: "Nháp", OPEN: "Đang mở", ARCHIVED: "Lưu trữ" };
export const resultLabels: Record<EnrollmentResult, string> = {
  ENROLLED: "Đã thêm", RESTORED: "Đã thêm lại", ALREADY_ENROLLED: "Đã có trong lớp", NOT_FOUND: "Không tìm thấy tài khoản",
  NOT_STUDENT: "Không phải sinh viên", DISABLED: "Tài khoản vô hiệu hóa", IN_OTHER_CLASS: "Đang ở lớp khác cùng môn",
  INVALID_EMAIL: "Email không hợp lệ", DUPLICATE_IN_LIST: "Trùng trong danh sách",
};
