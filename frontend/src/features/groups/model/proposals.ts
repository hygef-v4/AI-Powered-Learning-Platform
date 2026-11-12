import { ApiError } from "@/shared/api/client";
import { formatDateTime } from "@/shared/dateTime";
import type { FormationSummary, ProposalStatus, ProposalView, ProposalWarning } from "./types";

export const MIN_GROUP_SIZE = 2;
export const MAX_GROUP_SIZE = 20;
export const MAX_PROPOSAL_NAME = 100;
export const MAX_REJECT_REASON = 1000;

const VN_OFFSET_MS = 7 * 60 * 60 * 1000;

export const PROPOSAL_STATUS_LABELS: Record<ProposalStatus, string> = {
  DRAFT: "Nháp",
  SUBMITTED: "Chờ duyệt",
  APPROVED: "Đã duyệt",
  REJECTED: "Bị từ chối",
  CANCELLED: "Đã hủy",
};

export const WARNING_LABELS: Record<ProposalWarning, string> = {
  LEADER_INACTIVE: "Trưởng nhóm đã rời lớp hoặc đã có nhóm",
  BELOW_MIN: "Chưa đủ sĩ số tối thiểu",
  ABOVE_MAX: "Vượt sĩ số tối đa",
};

/** Một câu tóm tắt đợt tự chia nhóm cho sinh viên và giảng viên. */
export function describeFormation(formation: FormationSummary): string {
  if (formation.open) {
    const size = `từ ${formation.minSize} đến ${formation.maxSize} người`;
    return formation.deadline
      ? `Đang mở đến ${formatDateTime(formation.deadline)}, nhóm ${size}.`
      : `Đang mở, nhóm ${size}.`;
  }
  switch (formation.closedReason) {
    case "CLASS_NOT_OPEN":
      return "Lớp chưa mở nên chưa tự chia nhóm được.";
    case "DEADLINE_PASSED":
      return `Đã hết hạn lúc ${formatDateTime(formation.deadline)}.`;
    default:
      return "Đang tắt.";
  }
}

/** Trưởng nhóm gửi duyệt được khi đợt mở, đề xuất nháp và đủ sĩ số tối thiểu. */
export function canSubmit(proposal: ProposalView, formation: FormationSummary): boolean {
  return formation.open && proposal.status === "DRAFT" && proposal.members.length >= formation.minSize;
}

/** Tên đề xuất đã cắt khoảng trắng, hoặc lỗi để hiện dưới ô. */
export function validateProposalName(raw: string): { name: string; error: string | null } {
  const name = raw.trim();
  if (!name || name.length > MAX_PROPOSAL_NAME) return { name, error: "Tên nhóm cần 1–100 ký tự." };
  return { name, error: null };
}

export type FormationErrors = { minSize?: string; maxSize?: string; deadline?: string };

/** Kiểm sớm form cài đặt đợt; backend vẫn kiểm lại (BR-U12-70). */
export function validateFormation(input: { enabled: boolean; minSize: string; maxSize: string; deadline: string },
  now: Date = new Date()): FormationErrors {
  const errors: FormationErrors = {};
  const min = Number(input.minSize);
  const max = Number(input.maxSize);
  const inRange = (value: number) => Number.isInteger(value) && value >= MIN_GROUP_SIZE && value <= MAX_GROUP_SIZE;
  if (!inRange(min)) errors.minSize = "Nhập số nguyên từ 2 đến 20.";
  if (!inRange(max)) errors.maxSize = "Nhập số nguyên từ 2 đến 20.";
  else if (inRange(min) && min > max) errors.maxSize = "Tối đa phải lớn hơn hoặc bằng tối thiểu.";
  const deadline = fromLocalInput(input.deadline);
  if (input.enabled && deadline && new Date(deadline).getTime() <= now.getTime()) {
    errors.deadline = "Hạn chót phải ở tương lai.";
  }
  return errors;
}

/** ISO → giá trị ô datetime-local theo giờ Việt Nam. */
export function toLocalInput(iso: string | null | undefined): string {
  if (!iso) return "";
  return new Date(new Date(iso).getTime() + VN_OFFSET_MS).toISOString().slice(0, 16);
}

/** Giá trị ô datetime-local (giờ Việt Nam) → ISO; rỗng thì null. */
export function fromLocalInput(value: string): string | null {
  if (!value) return null;
  const date = new Date(`${value}:00+07:00`);
  return Number.isNaN(date.getTime()) ? null : date.toISOString();
}

/** Lời báo lỗi cho người dùng: lấy thông điệp tiếng Việt của server, 404 thì báo chung. */
export function proposalErrorText(error: unknown, fallback = "Không thực hiện được, thử lại sau."): string {
  if (error instanceof ApiError) {
    if (error.problem.status === 404) return "Không tìm thấy đề xuất hoặc bạn không có quyền.";
    if (error.problem.status === 409 && error.problem.code?.endsWith("VERSION_CONFLICT")) {
      return "Đề xuất vừa thay đổi. Đã tải lại, hãy thử lại.";
    }
    return error.problem.detail ?? fallback;
  }
  return fallback;
}

export function isVersionConflict(error: unknown): boolean {
  return error instanceof ApiError && error.problem.status === 409 && !!error.problem.code?.endsWith("VERSION_CONFLICT");
}
