import { formatStudentLabel } from "@/shared/studentCode";
import type { Tone } from "@/shared/ui";
import type { SectionView, WorkspaceView } from "./types";
import { msg } from "@/i18n/msg";

/** Nhãn và màu trạng thái mục, dùng chung cho trang tài liệu nhóm và thẻ Tiến độ. */
export const SECTION_STATUS: Record<SectionView["status"], { text: string; tone: Tone }> = {
  OPEN: { text: msg("groupdocs.model.status.notStarted"), tone: "neutral" },
  CLAIMED: { text: msg("groupdocs.model.status.inProgress"), tone: "info" },
  DONE: { text: "Xong", tone: "success" },
};

/** Người đang giữ mục, hoặc người viết bản Xong; mục chưa làm thì chưa có ai. */
export function resolveSectionPerson(section: SectionView): string | null {
  if (section.status === "CLAIMED") return section.holderId;
  if (section.status === "DONE") return section.lastAuthorId;
  return null;
}

/** Tên thành viên để hiện: chính mình là "Bạn". */
export function resolveMemberName(view: Pick<WorkspaceView, "members" | "viewerId">, id: string | null): string {
  if (id === null) return msg("groupdocs.model.status.unclaimed");
  const member = view.members.find((m) => m.accountId === id);
  return formatStudentLabel({ name: id === view.viewerId ? msg("groupdocs.model.status.you") : member?.name ?? msg("groupdocs.model.status.member"), studentCode: member?.studentCode });
}

/** Mục của mình: đang giữ, hoặc đã Xong do mình viết. */
export function isOwnSection(section: SectionView, viewerId: string): boolean {
  return resolveSectionPerson(section) === viewerId;
}
