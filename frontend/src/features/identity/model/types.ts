import type { Role } from "@/shared/auth/roles";
import { msg } from "@/i18n/msg";

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
  /** MSSV chỉ đọc của sinh viên; null khi chưa có (BR-U01-106). */
  studentCode?: string | null;
};

export const ROLE_LABELS: Record<Role, string> = {
  STUDENT: msg("identity.model.types.sinhVien"),
  TEACHER: msg("identity.model.types.giangVien"),
  SUBJECT_MANAGER: msg("identity.model.types.chuNhiemMon"),
  ADMIN: msg("identity.model.types.quanTriVien"),
};
