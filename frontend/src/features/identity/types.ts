import type { Role } from "@/shared/auth/roles";

export type AccountStatus = "PENDING" | "ACTIVE" | "DISABLED";

export type OtpPurpose = "ACTIVATION" | "PASSWORD_RESET";

/** Trả về sau đăng nhập hoặc kích hoạt (domain-entities U01 mục 14). */
export type SessionInfo = {
  accountId: string;
  displayName: string;
  role: Role;
  homePath: string;
};

/** GET /api/v1/me/profile (UC 06). */
export type Profile = {
  accountId: string;
  email: string;
  displayName: string;
  phoneNumber: string | null;
  role: Role;
  status: AccountStatus;
  homePath: string;
};

export const ROLE_LABELS: Record<Role, string> = {
  STUDENT: "Sinh viên",
  TEACHER: "Giảng viên",
  SUBJECT_MANAGER: "Chủ nhiệm môn",
  ADMIN: "Quản trị viên",
};
