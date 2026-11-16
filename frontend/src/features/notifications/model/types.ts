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
  ENROLLED: msg("notifications.model.types.enrolledInAClass"),
  ASSIGNMENT_OPENED: msg("notifications.model.types.newAssignmentOpened"),
  DEADLINE_REMINDER: msg("notifications.model.types.deadlineReminder"),
  GRADE_PUBLISHED: msg("notifications.model.types.gradesPublished"),
  ANNOUNCEMENT_IMPORTANT: msg("notifications.model.types.importantClassAnnouncement"),
  LEADER_CHANGE_REQUESTED: msg("notifications.model.types.leaderChangeRequest"),
  QUESTION_PROPOSED: msg("notifications.model.types.questionProposalsAwaitingApproval"),
  QUESTION_REVIEWED: msg("notifications.model.types.questionReviewResults"),
  BACKUP_FAILED: msg("notifications.model.types.backupFailed"),
};
