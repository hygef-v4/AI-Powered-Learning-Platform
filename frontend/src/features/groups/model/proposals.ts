import { ApiError } from "@/shared/api/client";
import { formatDateTime } from "@/shared/dateTime";
import type { FormationSummary, ProposalStatus, ProposalView, ProposalWarning } from "./types";
import { msg } from "@/i18n/msg";

export const MIN_GROUP_SIZE = 2;
export const MAX_GROUP_SIZE = 20;
export const MAX_PROPOSAL_NAME = 100;
export const MAX_REJECT_REASON = 1000;

const VN_OFFSET_MS = 7 * 60 * 60 * 1000;

export const PROPOSAL_STATUS_LABELS: Record<ProposalStatus, string> = {
  DRAFT: msg("groups.model.proposals.draft"),
  SUBMITTED: msg("groups.model.proposals.awaitingApproval"),
  APPROVED: msg("groups.model.proposals.approved"),
  REJECTED: msg("groups.model.proposals.rejected"),
  CANCELLED: msg("groups.model.proposals.cancelled"),
};

export const WARNING_LABELS: Record<ProposalWarning, string> = {
  LEADER_INACTIVE: msg("groups.model.proposals.theLeaderHasLeftThe"),
  BELOW_MIN: msg("groups.model.proposals.belowTheMinimumSize"),
  ABOVE_MAX: msg("groups.model.proposals.overTheMaximumSize"),
};

/** Một câu tóm tắt đợt tự chia nhóm cho sinh viên và giảng viên. */
export function describeFormation(formation: FormationSummary): string {
  if (formation.open) {
    const size = msg("groups.model.proposals.toPeople", { minSize: String(formation.minSize), maxSize: String(formation.maxSize) });
    return formation.deadline
      ? msg("groups.model.proposals.openUntilGroupsOf", { value: String(formatDateTime(formation.deadline)), size: String(size) })
      : msg("groups.model.proposals.openGroupsOf", { size: String(size) });
  }
  switch (formation.closedReason) {
    case "CLASS_NOT_OPEN":
      return msg("groups.model.proposals.theClassIsNotOpen");
    case "DEADLINE_PASSED":
      return msg("groups.model.proposals.expiredAt", { value: String(formatDateTime(formation.deadline)) });
    default:
      return msg("groups.model.proposals.off");
  }
}

/** Trưởng nhóm gửi duyệt được khi đợt mở, đề xuất nháp và đủ sĩ số tối thiểu. */
export function canSubmit(proposal: ProposalView, formation: FormationSummary): boolean {
  return formation.open && proposal.status === "DRAFT" && proposal.members.length >= formation.minSize;
}

/** Tên đề xuất đã cắt khoảng trắng, hoặc lỗi để hiện dưới ô. */
export function validateProposalName(raw: string): { name: string; error: string | null } {
  const name = raw.trim();
  if (!name || name.length > MAX_PROPOSAL_NAME) return { name, error: msg("groups.model.proposals.theGroupNameMustBe") };
  return { name, error: null };
}

export type FormationErrors = { minSize?: string; maxSize?: string; deadline?: string };

/** Kiểm sớm form cài đặt đợt; backend vẫn kiểm lại (BR-U12-70). */
export function validateFormation(input: { enabled: boolean; minSize: string; maxSize: string; deadline: string },
  now: Date = new Date()): FormationErrors {
  const errors: FormationErrors = {};
  const min = Number(input.minSize);
  const max = Number(input.maxSize);
  const isInRange = (value: number) => Number.isInteger(value) && value >= MIN_GROUP_SIZE && value <= MAX_GROUP_SIZE;
  if (!isInRange(min)) errors.minSize = msg("groups.model.proposals.enterAWholeNumberFrom");
  if (!isInRange(max)) errors.maxSize = msg("groups.model.proposals.enterAWholeNumberFrom");
  else if (isInRange(min) && min > max) errors.maxSize = msg("groups.model.proposals.theMaximumMustBeGreater");
  const deadline = convertFromLocalInput(input.deadline);
  if (input.enabled && deadline && new Date(deadline).getTime() <= now.getTime()) {
    errors.deadline = msg("groups.model.proposals.theDeadlineMustBeIn");
  }
  return errors;
}

/** ISO → giá trị ô datetime-local theo giờ Việt Nam. */
export function convertToLocalInput(iso: string | null | undefined): string {
  if (!iso) return "";
  return new Date(new Date(iso).getTime() + VN_OFFSET_MS).toISOString().slice(0, 16);
}

/** Giá trị ô datetime-local (giờ Việt Nam) → ISO; rỗng thì null. */
export function convertFromLocalInput(value: string): string | null {
  if (!value) return null;
  const date = new Date(`${value}:00+07:00`);
  return Number.isNaN(date.getTime()) ? null : date.toISOString();
}

/** Lời báo lỗi cho người dùng: lấy thông điệp tiếng Việt của server, 404 thì báo chung. */
export function describeProposalError(error: unknown, fallback = msg("groups.model.proposals.couldNotCompleteItPlease")): string {
  if (error instanceof ApiError) {
    if (error.problem.status === 404) return msg("groups.model.proposals.proposalNotFoundOrYou");
    if (error.problem.status === 409 && error.problem.code?.endsWith("VERSION_CONFLICT")) {
      return msg("groups.model.proposals.theProposalJustChangedIt");
    }
    return error.problem.detail ?? fallback;
  }
  return fallback;
}

export function isVersionConflict(error: unknown): boolean {
  return error instanceof ApiError && error.problem.status === 409 && !!error.problem.code?.endsWith("VERSION_CONFLICT");
}
