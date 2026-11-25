import { msg } from "@/i18n/msg";
import type { NotificationItem } from "./types";

type NotificationText = { title: string; summary: string };

/**
 * Tiêu đề và tóm tắt cố định theo loại thông báo (U14 lưu sẵn câu tiếng Việt cho từng loại), dịch theo ngôn ngữ giao diện.
 * Loại chưa biết giữ câu server gửi.
 */
const TEXT_BY_TYPE: Record<string, NotificationText> = {
  ENROLLED: { title: msg("notifications.model.labels.enrolledTitle"), summary: msg("notifications.model.labels.enrolledSummary") },
  ASSIGNMENT_OPENED: {
    title: msg("notifications.model.labels.assignmentOpenedTitle"),
    summary: msg("notifications.model.labels.assignmentOpenedSummary"),
  },
  QUIZ_PUBLISHED: {
    title: msg("notifications.model.labels.quizPublishedTitle"),
    summary: msg("notifications.model.labels.quizPublishedSummary"),
  },
  GRADE_PUBLISHED: {
    title: msg("notifications.model.labels.gradePublishedTitle"),
    summary: msg("notifications.model.labels.gradePublishedSummary"),
  },
  DEADLINE_REMINDER: {
    title: msg("notifications.model.labels.deadlineReminderTitle"),
    summary: msg("notifications.model.labels.deadlineReminderSummary"),
  },
  ANNOUNCEMENT: {
    title: msg("notifications.model.labels.announcementTitle"),
    summary: msg("notifications.model.labels.announcementSummary"),
  },
  BACKUP_FAILED: {
    title: msg("notifications.model.labels.backupFailedTitle"),
    summary: msg("notifications.model.labels.backupFailedSummary"),
  },
  LEADER_CHANGE_REQUESTED: {
    title: msg("notifications.model.labels.leaderChangeRequestedTitle"),
    summary: msg("notifications.model.labels.leaderChangeRequestedSummary"),
  },
  LEADER_CHANGED: {
    title: msg("notifications.model.labels.leaderChangedTitle"),
    summary: msg("notifications.model.labels.leaderChangedSummary"),
  },
  LEADER_REQUEST_REJECTED: {
    title: msg("notifications.model.labels.leaderRequestRejectedTitle"),
    summary: msg("notifications.model.labels.leaderRequestRejectedSummary"),
  },
  GROUP_MEMBERSHIP_CHANGED: {
    title: msg("notifications.model.labels.groupMembershipChangedTitle"),
    summary: msg("notifications.model.labels.groupMembershipChangedSummary"),
  },
  GROUP_SUBMITTED: {
    title: msg("notifications.model.labels.groupSubmittedTitle"),
    summary: msg("notifications.model.labels.groupSubmittedSummary"),
  },
  GROUP_PROPOSAL_INVITED: {
    title: msg("notifications.model.labels.groupProposalInvitedTitle"),
    summary: msg("notifications.model.labels.groupProposalInvitedSummary"),
  },
  GROUP_PROPOSAL_SUBMITTED: {
    title: msg("notifications.model.labels.groupProposalSubmittedTitle"),
    summary: msg("notifications.model.labels.groupProposalSubmittedSummary"),
  },
  GROUP_PROPOSAL_APPROVED: {
    title: msg("notifications.model.labels.groupProposalApprovedTitle"),
    summary: msg("notifications.model.labels.groupProposalApprovedSummary"),
  },
  GROUP_PROPOSAL_REJECTED: {
    title: msg("notifications.model.labels.groupProposalRejectedTitle"),
    summary: msg("notifications.model.labels.groupProposalRejectedSummary"),
  },
  QUESTION_PROPOSED: {
    title: msg("notifications.model.labels.questionProposedTitle"),
    summary: msg("notifications.model.labels.questionProposedSummary"),
  },
  QUESTION_REVIEWED: {
    title: msg("notifications.model.labels.questionReviewedTitle"),
    summary: msg("notifications.model.labels.questionReviewedSummary"),
  },
};

export function describeNotification(item: Pick<NotificationItem, "type" | "title" | "summary">): NotificationText {
  return TEXT_BY_TYPE[item.type] ?? { title: item.title, summary: item.summary };
}
