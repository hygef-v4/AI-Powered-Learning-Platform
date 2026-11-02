export type NotificationItem = { id: string; type: string; title: string; summary: string; link: string; readAt: string | null; createdAt: string };
export type NotificationFeed = { items: NotificationItem[]; page: number; size: number; hasMore: boolean };
export type EmailPreferences = Record<string, boolean>;
/** Tên các loại email; mỗi vai trò chỉ thấy các loại server trả về cho mình (yêu cầu 2026-11-02). */
export const preferenceLabels: Record<string, string> = {
  ENROLLED: "Ghi danh vào lớp", ASSIGNMENT_OPENED: "Bài tập mới mở", DEADLINE_REMINDER: "Nhắc hạn nộp", GRADE_PUBLISHED: "Công bố điểm",
  ANNOUNCEMENT_IMPORTANT: "Thông báo quan trọng của lớp", LEADER_CHANGE_REQUESTED: "Yêu cầu đổi trưởng nhóm",
  QUESTION_PROPOSED: "Câu hỏi được đề xuất", QUESTION_REVIEWED: "Kết quả duyệt câu hỏi đề xuất", BACKUP_FAILED: "Sao lưu thất bại",
};
