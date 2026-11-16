import { formatStudentLabel } from "@/shared/studentCode";
import type { Tone } from "@/shared/ui";
import type { SectionView, WorkspaceView } from "./types";

/** Nhãn và màu trạng thái mục, dùng chung cho trang tài liệu nhóm và thẻ Tiến độ. */
export const SECTION_STATUS: Record<SectionView["status"], { text: string; tone: Tone }> = {
  OPEN: { text: "Chưa làm", tone: "neutral" },
  CLAIMED: { text: "Đang làm", tone: "info" },
  DONE: { text: "Xong", tone: "success" },
};

/** Người đang giữ mục, hoặc người viết bản Xong; mục chưa làm thì chưa có ai. */
export function sectionPerson(section: SectionView): string | null {
  if (section.status === "CLAIMED") return section.holderId;
  if (section.status === "DONE") return section.lastAuthorId;
  return null;
}

/** Tên thành viên để hiện: chính mình là "Bạn". */
export function memberName(view: Pick<WorkspaceView, "members" | "viewerId">, id: string | null): string {
  if (id === null) return "Chưa ai nhận";
  const member = view.members.find((m) => m.accountId === id);
  return formatStudentLabel({ name: id === view.viewerId ? "Bạn" : member?.name ?? "Thành viên", studentCode: member?.studentCode });
}

/** Mục của mình: đang giữ, hoặc đã Xong do mình viết. */
export function isOwnSection(section: SectionView, viewerId: string): boolean {
  return sectionPerson(section) === viewerId;
}
