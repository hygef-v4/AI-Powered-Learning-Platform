import type { Tone } from "@/shared/ui/tones";
import { STUDENT_CODE_ERROR_LABELS } from "@/shared/studentCode";
import type { AccountStatus } from "../../model/types";
import { msg } from "@/i18n/msg";

// Nhãn và màu trạng thái theo README của design: Hoạt động xanh lá, Chờ kích hoạt vàng, Vô hiệu hóa đỏ.
export const STATUS_LABELS: Record<AccountStatus, string> = {
  PENDING: msg("identity.admin.model.labels.choKichHoat"),
  ACTIVE: msg("identity.admin.model.labels.hoatDong"),
  DISABLED: msg("identity.admin.model.labels.voHieuHoa"),
};

export const STATUS_TONES: Record<AccountStatus, Tone> = {
  PENDING: "warning",
  ACTIVE: "success",
  DISABLED: "danger",
};

/** Lý do từ chối dòng CSV (AccountImportService), hiện sau "Lỗi: " ở cột Kết quả kiểm tra (design `a02b`). */
export const IMPORT_REASON_LABELS: Record<string, string> = {
  MISSING_FIELD: msg("identity.admin.model.labels.thieuEmailHoTenHoacVai"),
  INVALID_EMAIL: msg("identity.admin.model.labels.emailKhongHopLe"),
  DUPLICATE_IN_FILE: msg("identity.admin.model.labels.emailTrungDongKhacTrongTep"),
  EMAIL_EXISTS: msg("identity.admin.model.labels.emailDaTonTai"),
  INVALID_DISPLAY_NAME: msg("identity.admin.model.labels.hoTenQuaDai"),
  INVALID_ROLE: msg("identity.admin.model.labels.vaiTroKhongHopLe"),
  MISSING_STUDENT_CODE: msg("identity.admin.model.labels.thieuMssvCuaSinhVien"),
  INVALID_STUDENT_CODE: msg("identity.admin.model.labels.mssvSaiDinhDang"),
  STUDENT_CODE_NOT_ALLOWED: msg("identity.admin.model.labels.chiSinhVienMoiCoMssv"),
  DUPLICATE_STUDENT_CODE_IN_FILE: msg("identity.admin.model.labels.mssvTrungDongKhacTrongTep"),
  STUDENT_CODE_EXISTS: msg("identity.admin.model.labels.mssvDaThuocTaiKhoanKhac"),
};

/** Lỗi nghiệp vụ khi Admin thao tác tài khoản. */
export const ADMIN_ERROR_LABELS: Record<string, string> = {
  EMAIL_EXISTS: msg("identity.admin.model.labels.emailDaCoTaiKhoan"),
  INVALID_EMAIL: msg("identity.admin.model.labels.emailKhongHopLe2"),
  VERSION_CONFLICT: msg("identity.admin.model.labels.duLieuDaDuocThayDoi"),
  LAST_ADMIN: msg("identity.admin.model.labels.phaiConItNhatMotQuan"),
  SELF_CHANGE_BLOCKED: msg("identity.admin.model.labels.khongTheTuDoiVaiTro"),
  ROLE_CHANGE_BLOCKED: msg("identity.admin.model.labels.taiKhoanConPhuTrachMon"),
  STUDENT_ROLE_CHANGE_NOT_ALLOWED: msg("identity.admin.model.labels.khongDoiDuocVaiTroVao"),
  IMPORT_CONFLICT: msg("identity.admin.model.labels.duLieuVuaThayDoiHay"),
  ...STUDENT_CODE_ERROR_LABELS,
};

export { formatDate } from "@/shared/dateTime";
