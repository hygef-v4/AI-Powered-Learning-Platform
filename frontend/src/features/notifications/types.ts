export type NotificationItem = { id: string; type: string; title: string; summary: string; link: string; readAt: string | null; createdAt: string };
export type NotificationFeed = { items: NotificationItem[]; page: number; size: number; hasMore: boolean };
export type EmailPreferences = Record<string, boolean>;
export const preferenceLabels: Record<string, string> = { ENROLLED: "Ghi danh vào lớp", ASSIGNMENT_OPENED: "Bài tập mới mở", DEADLINE_REMINDER: "Nhắc hạn nộp", GRADE_PUBLISHED: "Công bố điểm", ANNOUNCEMENT_IMPORTANT: "Thông báo quan trọng của lớp" };
