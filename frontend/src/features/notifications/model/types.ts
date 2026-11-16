import { msg } from "@/i18n/msg";

export type NotificationItem = {
  id: string;
  type: string;
  title: string;
  summary: string;
  link: string;
  readAt: string | null;
  createdAt: string;
};
export type NotificationFeed = {
  items: NotificationItem[];
  page: number;
  size: number;
  hasMore: boolean;
};
export type EmailPreferences = Record<string, boolean>;
export const preferenceLabels: Record<string, string> = {
  ENROLLED: msg("notifications.model.types.ghiDanhVaoLop"),
  ASSIGNMENT_OPENED: msg("notifications.model.types.baiTapMoiMo"),
  DEADLINE_REMINDER: msg("notifications.model.types.nhacHanNop"),
  GRADE_PUBLISHED: msg("notifications.model.types.congBoDiem"),
  ANNOUNCEMENT_IMPORTANT: msg("notifications.model.types.thongBaoQuanTrongCuaLop"),
  LEADER_CHANGE_REQUESTED: msg("notifications.model.types.yeuCauDoiTruongNhom"),
  QUESTION_PROPOSED: msg("notifications.model.types.deXuatCauHoiChoDuyet"),
  QUESTION_REVIEWED: msg("notifications.model.types.ketQuaDuyetCauHoi"),
  BACKUP_FAILED: msg("notifications.model.types.saoLuuThatBai"),
};
