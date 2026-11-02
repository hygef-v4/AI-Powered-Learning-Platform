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
  ENROLLED: "Ghi danh vào lớp",
  ASSIGNMENT_OPENED: "Bài tập mới mở",
  DEADLINE_REMINDER: "Nhắc hạn nộp",
  GRADE_PUBLISHED: "Công bố điểm",
  ANNOUNCEMENT_IMPORTANT: "Thông báo quan trọng của lớp",
  LEADER_CHANGE_REQUESTED: "Yêu cầu đổi trưởng nhóm",
  QUESTION_PROPOSED: "Đề xuất câu hỏi chờ duyệt",
  QUESTION_REVIEWED: "Kết quả duyệt câu hỏi",
  BACKUP_FAILED: "Sao lưu thất bại",
};
