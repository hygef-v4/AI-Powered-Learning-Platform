import type { Tone } from "@/shared/ui";
import type { AuditFilterForm, AuditQuery, AuditResult } from "./types";
import { msg } from "@/i18n/msg";

export const RESULT_LABELS: Record<AuditResult, { label: string; tone: Tone }> = {
  SUCCESS: { label: msg("audit.model.labels.thanhCong"), tone: "success" },
  DENIED: { label: msg("audit.model.labels.tuChoi"), tone: "warning" },
  FAILURE: { label: msg("audit.model.labels.thatBai"), tone: "danger" },
};

/** Nhãn hành động thường gặp; mã chưa có nhãn hiện nguyên mã. */
export const ACTION_LABELS: Record<string, string> = {
  LOGIN: msg("audit.model.labels.dangNhap"),
  ACCESS_DENIED: msg("audit.model.labels.truyCapBiTuChoi"),
  ROLE_CHANGED: msg("audit.model.labels.doiVaiTro"),
  ACCOUNT_CREATED: msg("audit.model.labels.taoTaiKhoan"),
  ACCOUNT_UPDATED: msg("audit.model.labels.suaTaiKhoan"),
  ACCOUNT_STATUS_CHANGED: msg("audit.model.labels.doiTrangThaiTaiKhoan"),
  SETTING_UPDATED: msg("audit.model.labels.suaCaiDat"),
  BACKUP_COMPLETED: msg("audit.model.labels.saoLuuDuLieu"),
  BACKUP_FAILED: msg("audit.model.labels.saoLuuThatBai"),
  GRADE_PUBLISHED: msg("audit.model.labels.congBoDiem"),
  AUDIT_QUERIED: msg("audit.model.labels.traCuuAudit"),
  // U06 Kết nối AI
  AI_CONNECTION_CHANGED: msg("audit.model.labels.doiKhoaApiAi"),
  AI_CONNECTION_TESTED: msg("audit.model.labels.kiemTraKhoaApiAi"),
  QUOTA_CHANGED: msg("audit.model.labels.doiHanMucAi"),
  MCP_CREDENTIAL_CREATED: msg("audit.model.labels.taoTokenAgentMcp"),
  MCP_REVOKED: msg("audit.model.labels.thuHoiTokenAgentMcp"),
  MCP_TOOL_CALLED: msg("audit.model.labels.agentMcpGoiCongCu"),
  // U12 Nhóm và bài nhóm
  GROUP_CREATED: msg("audit.model.labels.taoNhom"),
  GROUP_RENAMED: msg("audit.model.labels.doiTenNhom"),
  GROUP_UPDATED: msg("audit.model.labels.suaNhom"),
  GROUP_DELETED: msg("audit.model.labels.xoaNhom"),
  GROUP_LEADER_CHANGED: msg("audit.model.labels.doiTruongNhom"),
  LEADER_REQUEST_SUBMITTED: msg("audit.model.labels.guiYeuCauDoiTruongNhom"),
  LEADER_REQUEST_CANCELLED: msg("audit.model.labels.huyYeuCauDoiTruongNhom"),
  LEADER_REQUEST_APPROVED: msg("audit.model.labels.duyetYeuCauDoiTruongNhom"),
  LEADER_REQUEST_REJECTED: msg("audit.model.labels.tuChoiYeuCauDoiTruong"),
  GROUP_SECTION_ASSIGNED: msg("audit.model.labels.giaoMucTaiLieuNhom"),
  GROUP_SECTION_RELEASED: msg("audit.model.labels.nhaMucTaiLieuNhom"),
  GROUP_SUBMITTED: msg("audit.model.labels.nopBaiNhom"),
  GROUP_FORMATION_UPDATED: msg("audit.model.labels.caiDatTuChiaNhom"),
  GROUP_PROPOSAL_SUBMITTED: msg("audit.model.labels.guiDeXuatNhom"),
  GROUP_PROPOSAL_APPROVED: msg("audit.model.labels.duyetDeXuatNhom"),
  GROUP_PROPOSAL_REJECTED: msg("audit.model.labels.tuChoiDeXuatNhom"),
  ANNOUNCEMENT_CREATED: msg("audit.model.labels.taoThongBao"),
  QUIZ_REOPENED: msg("audit.model.labels.moLaiQuiz"),
  QUIZ_PENALTY_APPLIED: msg("audit.model.labels.truDiemLuotQuiz"),
  QUIZ_PENALTY_DISMISSED: msg("audit.model.labels.quyetDinhKhongTruDiemLuot"),
  QUESTION_PROPOSED: msg("audit.model.labels.deXuatCauVaoNganHang"),
  QUESTION_APPROVED: msg("audit.model.labels.duyetCauDeXuat"),
  QUESTION_REJECTED: msg("audit.model.labels.tuChoiCauDeXuat"),
  QUIZ_RETIRED: msg("audit.model.labels.luuTruQuiz"),
  QUIZ_ATTEMPT_SUBMITTED: msg("audit.model.labels.nopBaiQuiz"),
  QUESTION_CREATED: msg("audit.model.labels.taoCauHoi"),
  QUESTION_ACTIVATED: msg("audit.model.labels.kichHoatCauHoi"),
  QUESTION_RETIRED: msg("audit.model.labels.ngungDungCauHoi"),
  QUESTION_DELETED: msg("audit.model.labels.xoaCauHoi"),
  QUESTIONS_IMPORTED: msg("audit.model.labels.nhapCauHoi"),
  QUIZ_CREATED: msg("audit.model.labels.taoQuiz"),
  QUIZ_PUBLISHED: msg("audit.model.labels.phatHanhQuiz"),
  QUIZ_DELETED: msg("audit.model.labels.xoaQuiz"),
  QUIZ_CLONED: msg("audit.model.labels.nhanBanQuiz"),
  QUIZ_COPIED: msg("audit.model.labels.saoChepQuiz"),
  QUIZ_VERSION_CREATED: msg("audit.model.labels.taoPhienBanQuiz"),
  QUIZ_AI_DRAFT_ACCEPTED: msg("audit.model.labels.chapNhanDeXuatAiCho"),
  // U01 Tài khoản
  ACCOUNT_ACTIVATED: msg("audit.model.labels.kichHoatTaiKhoan"),
  ACCOUNT_DISABLED: msg("audit.model.labels.voHieuHoaTaiKhoan"),
  ACCOUNT_ENABLED: msg("audit.model.labels.batLaiTaiKhoan"),
  ACCOUNT_TEMP_LOCKED: msg("audit.model.labels.khoaTamTaiKhoan"),
  ACCOUNTS_IMPORTED: msg("audit.model.labels.nhapDanhSachTaiKhoan"),
  LOGIN_FAILED: msg("audit.model.labels.dangNhapThatBai"),
  LOGIN_BLOCKED_LOCKED: msg("audit.model.labels.chanDangNhapDoDangKhoa"),
  AUTH_FAILED: msg("audit.model.labels.xacThucThatBai"),
  PASSWORD_CHANGED: msg("audit.model.labels.doiMatKhau"),
  PASSWORD_RESET: msg("audit.model.labels.datLaiMatKhau"),
  PROFILE_UPDATED: msg("audit.model.labels.suaHoSo"),
  // U04 Môn học
  SUBJECT_CREATED: msg("audit.model.labels.taoMon"),
  SUBJECT_UPDATED: msg("audit.model.labels.suaMon"),
  SUBJECT_ARCHIVED: msg("audit.model.labels.luuTruMon"),
  SUBJECT_REOPENED: msg("audit.model.labels.moLaiMon"),
  SUBJECT_MANAGER_CHANGED: msg("audit.model.labels.doiChuNhiemMon"),
  // U05 Lớp và ghi danh
  CLASS_CREATED: msg("audit.model.labels.taoLop"),
  CLASS_UPDATED: msg("audit.model.labels.suaLop"),
  CLASS_STATUS_CHANGED: msg("audit.model.labels.doiTrangThaiLop"),
  CLASS_TEACHER_CHANGED: msg("audit.model.labels.doiGiangVienCuaLop"),
  STUDENT_ENROLLED: msg("audit.model.labels.ghiDanhSinhVien"),
  STUDENT_REMOVED: msg("audit.model.labels.xoaSinhVienKhoiLop"),
  // U06 Khóa AI và token agent
  AI_CONNECTION_KEY_REVEALED: msg("audit.model.labels.xemKhoaApiAi"),
  MCP_CREDENTIAL_UPDATED: msg("audit.model.labels.suaTokenAgentMcp"),
  MCP_TOKEN_REVEALED: "Xem token agent MCP",
  // U07 Soạn bài tập
  ASSIGNMENT_CREATED: msg("audit.model.labels.taoBaiTap"),
  ASSIGNMENT_UPDATED: msg("audit.model.labels.suaBaiTap"),
  ASSIGNMENT_DELETED: msg("audit.model.labels.xoaBaiTap"),
  ASSIGNMENT_REVIEWED: msg("audit.model.labels.duyetBaiTap"),
  ASSIGNMENT_PUBLISHED: msg("audit.model.labels.phatHanhBaiTap"),
  ASSIGNMENT_RETIRED: msg("audit.model.labels.luuTruBaiTap"),
  ASSIGNMENT_CLONED: msg("audit.model.labels.nhanBanBaiTap"),
  ASSIGNMENT_COPIED: msg("audit.model.labels.saoChepBaiTap"),
  ASSIGNMENT_VERSION_CREATED: msg("audit.model.labels.taoPhienBanBaiTap"),
  ASSIGNMENT_SCHEDULE_CHANGED: msg("audit.model.labels.doiLichBaiTap"),
  ASSIGNMENT_CLOSED: msg("audit.model.labels.dongNhanBai"),
  ASSIGNMENT_DOCX_IMPORTED: msg("audit.model.labels.nhapBaiTapTuDocx"),
  ASSIGNMENT_SKELETON_SAVED: msg("audit.model.labels.luuKhungBaiLam"),
  ASSIGNMENT_AI_DRAFT_REQUESTED: msg("audit.model.labels.yeuCauAiSoanBaiTap"),
  ASSIGNMENT_AI_DRAFT_SAVED: msg("audit.model.labels.luuDeXuatAiChoBai"),
  ASSIGNMENT_AI_DRAFT_APPLIED: msg("audit.model.labels.apDungDeXuatAiCho"),
  ASSIGNMENT_AI_DRAFT_DISCARDED: msg("audit.model.labels.boDeXuatAiChoBai"),
  RUBRIC_UPDATED: msg("audit.model.labels.suaRubric"),
  // U08 Làm bài
  ATTEMPT_STARTED: msg("audit.model.labels.batDauLamBai"),
  ATTEMPT_SAVED: msg("audit.model.labels.luuBaiLam"),
  ASSIGNMENT_SUBMITTED: msg("audit.model.labels.nopBai"),
  SUBMIT_REJECTED: msg("audit.model.labels.tuChoiNopBai"),
  ATTEMPT_DOCX_IMPORTED: msg("audit.model.labels.nhapBaiLamTuDocx"),
  ATTEMPT_DOCX_EXPORTED: msg("audit.model.labels.xuatBaiLamRaDocx"),
  PRACTICE_FEEDBACK_REQUESTED: msg("audit.model.labels.yeuCauGopYBaiLuyen"),
  // U09 Chấm điểm
  GRADE_SAVED: msg("audit.model.labels.luuDiem"),
  GRADE_CHANGED: msg("audit.model.labels.doiDiemChinhThuc"),
  GRADE_FINALIZED: msg("audit.model.labels.chotDiem"),
  GRADE_DISTRIBUTION_VISIBILITY_CHANGED: msg("audit.model.labels.doiHienThiPhanBoDiem"),
  GRADEBOOK_EXPORTED: msg("audit.model.labels.xuatSoDiem"),
  // U12 Tài liệu nhóm
  GROUP_DOC_IMPORTED: msg("audit.model.labels.nhapTaiLieuNhom"),
  GROUP_PEER_REVIEW_SAVED: msg("audit.model.labels.luuDanhGiaCheo"),
  GROUP_ASSIGNMENT_LOCKED: msg("audit.model.labels.chotPhanCongTaiLieuNhom"),
  GROUP_ASSIGNMENT_UNLOCKED: msg("audit.model.labels.moChotPhanCongTaiLieu"),
  GROUP_SECTION_LOCKED: msg("audit.model.labels.khoaMucTaiLieuNhom"),
  GROUP_SECTION_UNLOCKED: msg("audit.model.labels.moKhoaMucTaiLieuNhom"),
  GROUP_SECTION_RESTORED: msg("audit.model.labels.khoiPhucMucTaiLieuNhom"),
  GROUP_SECTION_TAKEN_OVER: msg("audit.model.labels.nhanLaiMucTaiLieuNhom"),
  // U13 Học liệu
  MATERIAL_CREATED: msg("audit.model.labels.taoHocLieu"),
  MATERIAL_UPDATED: msg("audit.model.labels.suaHocLieu"),
  MATERIAL_ARCHIVED: msg("audit.model.labels.luuTruHocLieu"),
  MATERIAL_VERSION_CREATED: msg("audit.model.labels.taoPhienBanHocLieu"),
  MATERIAL_MODULE_CREATED: msg("audit.model.labels.taoModule"),
  MATERIAL_MODULE_UPDATED: msg("audit.model.labels.suaModule"),
  MATERIAL_MODULE_DELETED: msg("audit.model.labels.xoaModule"),
  MATERIAL_MODULE_QUIZ_ATTACHED: msg("audit.model.labels.ganQuizVaoModule"),
  MATERIAL_MODULE_QUIZ_DETACHED: msg("audit.model.labels.goQuizKhoiModule"),
  // U14 Thông báo
  ANNOUNCEMENT_UPDATED: msg("audit.model.labels.suaThongBao"),
  ANNOUNCEMENT_DELETED: msg("audit.model.labels.xoaThongBao"),
};

export const OBJECT_TYPE_LABELS: Record<string, string> = {
  ACCOUNT: msg("audit.model.labels.taiKhoan"),
  SETTING: msg("audit.model.labels.caiDat"),
  SETTINGS: msg("audit.model.labels.trangCaiDat"),
  BACKUP: msg("audit.model.labels.banSaoLuu"),
  SUBJECT: msg("audit.model.labels.monHoc"),
  CLASS: msg("audit.model.labels.lop"),
  COURSE_CLASS: msg("audit.model.labels.lop"),
  BACKUP_RUN: msg("audit.model.labels.lanSaoLuu"),
  MATERIAL: msg("audit.model.labels.hocLieu"),
  MATERIAL_MODULE: msg("audit.model.labels.moduleHocLieu"),
  NOTIFICATION: msg("audit.model.labels.thongBaoCaNhan"),
  QUESTION: msg("audit.model.labels.cauHoi"),
  QUIZ_AI_DRAFT: msg("audit.model.labels.deXuatAiChoQuiz"),
  QUIZ_ATTEMPT: msg("audit.model.labels.luotLamQuiz"),
  ASSIGNMENT_ATTEMPT: msg("audit.model.labels.luotLamBaiTap"),
  ASSIGNMENT: msg("audit.model.labels.baiTap"),
  SUBMISSION: msg("audit.model.labels.baiNop"),
  AUDIT_LOG: msg("audit.model.labels.nhatKyAudit"),
  AI_CONNECTION: msg("audit.model.labels.khoaApiAi"),
  AI_QUOTA: msg("audit.model.labels.hanMucAi"),
  QUOTA_POLICY: msg("audit.model.labels.hanMucAiChung"),
  MCP_CREDENTIAL: "Token agent MCP",
  MCP_TOOL: msg("audit.model.labels.congCuMcp"),
  CLASS_GROUP: msg("audit.model.labels.nhomCuaLop"),
  LEADER_CHANGE_REQUEST: msg("audit.model.labels.yeuCauDoiTruongNhom"),
  GROUP_DOCUMENT: msg("audit.model.labels.taiLieuNhom"),
  GROUP_SECTION: msg("audit.model.labels.mucTaiLieuNhom"),
  ANNOUNCEMENT: msg("audit.model.labels.thongBao"),
  QUIZ: "Quiz",
  ATTEMPT: msg("audit.model.labels.luotLamBai"),
};

export const actionLabel = (code: string) => ACTION_LABELS[code] ?? code;
export const objectTypeLabel = (code: string) => OBJECT_TYPE_LABELS[code] ?? code;

/** Mã dài (UUID) rút gọn "3f9c2a…e81" trong bảng; popup chi tiết hiện đầy đủ. */
export function shortId(id: string): string {
  return id.length > 14 ? `${id.slice(0, 6)}…${id.slice(-3)}` : id;
}

const VN_TIME = "Asia/Ho_Chi_Minh";

function parts(iso: string): Record<string, string> {
  const list = new Intl.DateTimeFormat("vi-VN", {
    timeZone: VN_TIME,
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hourCycle: "h23",
  }).formatToParts(new Date(iso));
  return Object.fromEntries(list.map((part) => [part.type, part.value]));
}

/** Ngày giờ hiển thị dùng chung từ shared/dateTime (giờ Việt Nam). */
export { formatDateTime, formatFull, formatShort } from "@/shared/dateTime";

/** Ngày yyyy-mm-dd theo giờ Việt Nam của một thời điểm. */
export function vnDate(date: Date): string {
  const p = parts(date.toISOString());
  return `${p.year}-${p.month}-${p.day}`;
}

/** Mặc định 7 ngày gần nhất: từ 6 ngày trước tới hôm nay (theo giờ Việt Nam). */
export function defaultFilter(now: Date = new Date()): AuditFilterForm {
  const from = new Date(now.getTime() - 6 * 24 * 60 * 60 * 1000);
  return { actorEmail: "", action: "", objectType: "", objectId: "", result: "", fromDate: vnDate(from), toDate: vnDate(now) };
}

/** Đầu ngày và cuối ngày theo giờ Việt Nam (UTC+7) đổi sang ISO-8601. */
export function dayStart(date: string): string {
  return new Date(`${date}T00:00:00+07:00`).toISOString();
}

export function dayEnd(date: string): string {
  return new Date(`${date}T23:59:59.999+07:00`).toISOString();
}

/** Form bộ lọc → tham số API; có actorId (mở từ Account Detail) thì lọc đúng tài khoản đó, không thì gửi chữ đã gõ. */
export function toQuery(form: AuditFilterForm, actorId: string | undefined, page: number, size: number): AuditQuery {
  return {
    actorId,
    actor: actorId ? undefined : form.actorEmail.trim() || undefined,
    action: form.action.trim() || undefined,
    objectType: form.objectType.trim() || undefined,
    objectId: form.objectId.trim() || undefined,
    result: form.result || undefined,
    from: form.fromDate ? dayStart(form.fromDate) : undefined,
    to: form.toDate ? dayEnd(form.toDate) : undefined,
    page,
    size,
  };
}

/** Lý do từ chối hay gặp, ghi gọn 2–3 chữ. */
const REASON_LABELS: Record<string, string> = {
  ROLE_DENIED: msg("audit.model.labels.saiVaiTro"),
  UNKNOWN_EMAIL: "Sai email",
  BAD_PASSWORD: msg("audit.model.labels.saiMatKhau"),
  LOCKED: msg("audit.model.labels.taiKhoanKhoa"),
  ACCOUNT_INACTIVE: msg("audit.model.labels.taiKhoanKhoa"),
  ACCOUNT_NOT_ACTIVE: msg("audit.model.labels.taiKhoanKhoa"),
  TOO_MANY_FAILURES: msg("audit.model.labels.thuQuaNhieu"),
  RATE_LIMITED: msg("audit.model.labels.quaGioiHan"),
  OUT_OF_SCOPE: msg("audit.model.labels.ngoaiPhamVi"),
  CLASS_OUT_OF_SCOPE: msg("audit.model.labels.ngoaiPhamVi"),
  GRADING_SCOPE: msg("audit.model.labels.ngoaiPhamVi"),
  NOT_OWNER: msg("audit.model.labels.khongSoHuu"),
  ENROLLMENT_REQUIRED: msg("audit.model.labels.chuaGhiDanh"),
  TOKEN_ACCOUNT_MISMATCH: msg("audit.model.labels.saiTaiKhoan"),
  TOOL_ERROR: msg("audit.model.labels.loiCongCu"),
  ARGS_TOO_LARGE: msg("audit.model.labels.duLieuQuaLon"),
  DRIVE_ABUSE_BLOCKED: msg("audit.model.labels.biChan"),
  API_KEY_INVALID: msg("audit.model.labels.saiKhoaApi"),
  AI_SERVICE_UNAVAILABLE: msg("audit.model.labels.aiQuaTai"),
  ACCESS_DENIED: msg("audit.model.labels.khongCoQuyen"),
};

/**
 * Lý do trên bảng audit, ngắn 2–3 chữ: mã quen thì dịch, mã lạ đoán theo đuôi mã, câu chữ (thường là lỗi nhà cung cấp AI) thì
 * rút theo mã HTTP; còn lại lấy 3 chữ đầu. Lý do đầy đủ hiện khi rê chuột.
 */
export function shortReason(reason: string | null | undefined): string {
  const value = reason?.trim() ?? "";
  if (!value) return "—";
  if (REASON_LABELS[value]) return REASON_LABELS[value];
  if (/^[A-Z][A-Z0-9_]+$/.test(value)) {
    if (/NOT_FOUND$/.test(value)) return msg("audit.model.labels.khongTimThay");
    if (/(FORBIDDEN|DENIED|NOT_ALLOWED|ONLY)$/.test(value)) return msg("audit.model.labels.khongCoQuyen");
    if (/INVALID/.test(value)) return msg("audit.model.labels.khongHopLe");
    if (/TOO_LARGE/.test(value)) return msg("audit.model.labels.quaLon");
    if (/UNAVAILABLE/.test(value)) return msg("audit.model.labels.tamNgung");
    if (/CONFLICT/.test(value)) return msg("audit.model.labels.biXungDot");
    return msg("audit.model.labels.biTuChoi");
  }
  if (/key api|khóa|HTTP 40[13]/i.test(value)) return msg("audit.model.labels.saiKhoa");
  if (/HTTP 429|hạn mức|giới hạn/i.test(value)) return msg("audit.model.labels.hetHanMuc");
  if (/quá tải|HTTP 5\d\d/i.test(value)) return msg("audit.model.labels.aiQuaTai");
  if (/timeout|kịp|hết thời gian/i.test(value)) return msg("audit.model.labels.quaGio");
  if (/HTTP 400|từ chối/i.test(value)) return msg("audit.model.labels.biTuChoi");
  const words = value.split(/\s+/);
  return words.length > 3 ? `${words.slice(0, 3).join(" ")}…` : value;
}
