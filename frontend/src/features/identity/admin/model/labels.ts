import type { Tone } from "@/shared/ui/tones";
import { STUDENT_CODE_ERROR_LABELS } from "@/shared/studentCode";
import type { AccountStatus } from "../../model/types";
import { msg } from "@/i18n/msg";

// Nhãn và màu trạng thái theo README của design: Hoạt động xanh lá, Chờ kích hoạt vàng, Vô hiệu hóa đỏ.
export const STATUS_LABELS: Record<AccountStatus, string> = {
  PENDING: msg("identity.admin.model.labels.awaitingActivation"),
  ACTIVE: msg("identity.admin.model.labels.active"),
  DISABLED: msg("identity.admin.model.labels.disabled"),
};

export const STATUS_TONES: Record<AccountStatus, Tone> = {
  PENDING: "warning",
  ACTIVE: "success",
  DISABLED: "danger",
};

/** Lý do từ chối dòng CSV (AccountImportService), hiện sau "Lỗi: " ở cột Kết quả kiểm tra (design `a02b`). */
export const IMPORT_REASON_LABELS: Record<string, string> = {
  MISSING_FIELD: msg("identity.admin.model.labels.missingEmailFullNameOr"),
  INVALID_EMAIL: msg("identity.admin.model.labels.invalidEmail"),
  DUPLICATE_IN_FILE: msg("identity.admin.model.labels.emailDuplicatesAnotherRowIn"),
  EMAIL_EXISTS: msg("identity.admin.model.labels.emailAlreadyExists"),
  INVALID_DISPLAY_NAME: msg("identity.admin.model.labels.fullNameTooLong"),
  INVALID_ROLE: msg("identity.admin.model.labels.invalidRole"),
  MISSING_STUDENT_CODE: msg("identity.admin.model.labels.missingStudentId"),
  INVALID_STUDENT_CODE: msg("identity.admin.model.labels.invalidStudentIdFormat"),
  STUDENT_CODE_NOT_ALLOWED: msg("identity.admin.model.labels.onlyStudentsCanHaveA"),
  DUPLICATE_STUDENT_CODE_IN_FILE: msg("identity.admin.model.labels.studentIdDuplicatesAnotherRow"),
  STUDENT_CODE_EXISTS: msg("identity.admin.model.labels.studentIdAlreadyBelongsTo"),
};

/** Lỗi nghiệp vụ khi Admin thao tác tài khoản. */
export const ADMIN_ERROR_LABELS: Record<string, string> = {
  EMAIL_EXISTS: msg("identity.admin.model.labels.thisEmailAlreadyHasAn"),
  INVALID_EMAIL: msg("identity.admin.model.labels.invalidEmail2"),
  VERSION_CONFLICT: msg("identity.admin.model.labels.theDataWasChangedIn"),
  LAST_ADMIN: msg("identity.admin.model.labels.thereMustBeAtLeast"),
  SELF_CHANGE_BLOCKED: msg("identity.admin.model.labels.youCannotChangeYourOwn"),
  ROLE_CHANGE_BLOCKED: msg("identity.admin.model.labels.theAccountStillManagesSubjects"),
  STUDENT_ROLE_CHANGE_NOT_ALLOWED: msg("identity.admin.model.labels.cannotChangeTheRoleTo"),
  IMPORT_CONFLICT: msg("identity.admin.model.labels.theDataJustChangedPlease"),
  ...STUDENT_CODE_ERROR_LABELS,
};

export { formatDate } from "@/shared/dateTime";
