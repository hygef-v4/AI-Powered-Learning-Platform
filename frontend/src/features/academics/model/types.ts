import { msg } from "@/i18n/msg";

export type ClassStatus = "DRAFT" | "OPEN" | "ARCHIVED";
export type Audience = "student" | "teacher" | "manager";
export type PersonView = { id: string; name: string; email: string; studentCode?: string | null };
export type SubjectView = { id: string; code: string; name: string; description?: string };
export type CourseClassView = {
  id: string;
  subject: SubjectView;
  code: string;
  name: string;
  description: string;
  /** Ngày bắt đầu `yyyy-MM-dd` (BR-U05-61). */
  startsOn: string;
  /** Học kỳ suy ra từ ngày bắt đầu, ví dụ `SP27-BL5` (D-U04-05). */
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
  studentCode?: string | null;
};
export type ClassInfoInput = Pick<CourseClassView, "name" | "description" | "startsOn" | "version">;
export type CreateClassInput = Omit<ClassInfoInput, "version"> & { subjectId: string; code: string; teacherId: string | null };
export type EnrollmentResult = "ENROLLED" | "RESTORED" | "ALREADY_ENROLLED" | "NOT_FOUND" | "NOT_STUDENT" | "DISABLED" | "IN_OTHER_CLASS" | "INVALID_IDENTIFIER" | "DUPLICATE_IN_LIST";
/** `email` là giá trị đã nhập: email hoặc MSSV (BR-U05-58). */
export type EnrollmentRowResult = { line: number; email: string; result: EnrollmentResult };
export const CLASS_STATUS_LABELS: Record<ClassStatus, string> = { DRAFT: msg("academics.model.types.nhap"), OPEN: msg("academics.model.types.dangMo"), ARCHIVED: msg("academics.model.types.luuTru") };
export const ENROLLMENT_RESULT_LABELS: Record<EnrollmentResult, string> = {
  ENROLLED: msg("academics.model.types.daThem"), RESTORED: msg("academics.model.types.daThemLai"), ALREADY_ENROLLED: msg("academics.model.types.daCoTrongLop"), NOT_FOUND: msg("academics.model.types.khongTimThayTaiKhoan"),
  NOT_STUDENT: msg("academics.model.types.khongPhaiSinhVien"), DISABLED: msg("academics.model.types.taiKhoanVoHieuHoa"), IN_OTHER_CLASS: msg("academics.model.types.dangOLopKhacCungMon"),
  INVALID_IDENTIFIER: msg("academics.model.types.khongPhaiEmailHayMssvHop"), DUPLICATE_IN_LIST: msg("academics.model.types.trungTrongDanhSach"),
};
