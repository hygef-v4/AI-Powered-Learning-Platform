export type Announcement = { id: string; classId: string; className: string; title: string; body: string; important: boolean; sendEmail: boolean; createdBy: string; authorName: string; createdAt: string; updatedAt: string; version: number; canManage: boolean };
export type ClassOption = { classId: string; code: string; name: string; canManage: boolean; studentCount: number };
export type AnnouncementFeed = { items: Announcement[]; classes: ClassOption[]; page: number; size: number; hasMore: boolean };
export type AnnouncementInput = { title: string; body: string; important: boolean; sendEmail: boolean };
