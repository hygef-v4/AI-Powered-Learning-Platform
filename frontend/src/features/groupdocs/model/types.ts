import type { DocumentBlock, DocumentModel } from "@/shared/document";

// Block dùng chung kiểu của trình soạn U07/U08 (`@/shared/document`).
export type { DocumentBlock, DocumentModel };

export type SectionStatus = "OPEN" | "CLAIMED" | "DONE";

export type SectionView = {
  id: string;
  partId: string;
  title: string;
  orderNo: number;
  status: SectionStatus;
  holderId: string | null;
  assignedBy: string | null;
  publishedBlocks: DocumentBlock[] | null;
  /** Chỉ người giữ thấy (BR-U12-34). */
  draftBlocks: DocumentBlock[] | null;
  draftSavedAt: string | null;
  lastAuthorId: string | null;
  version: number;
  /** Mục đang giữ: lúc nhận và lần lưu nháp cuối của người giữ (chỉ mốc giờ, mọi người thấy). */
  claimedAt?: string | null;
  holderSavedAt?: string | null;
};

export type RevisionKind = "DONE" | "TAKEOVER" | "IMPORT";

/** Một bản lịch sử của mục (BR-U12-35), mới nhất trước. */
export type RevisionView = { id: string; authorId: string; kind: RevisionKind; createdAt: string; blocks: DocumentBlock[] };

export type WorkspaceView = {
  documentId: string;
  assignmentId: string;
  assignmentTitle: string | null;
  closesAt: string | null;
  lateUntil: string | null;
  classId: string;
  groupId: string;
  groupName: string;
  leaderId: string | null;
  members: { accountId: string; name: string | null; leader: boolean }[];
  status: "IN_PROGRESS" | "CLOSED";
  window: "ON_TIME" | "LATE" | "CLOSED";
  sharedBlocks: DocumentModel;
  sections: SectionView[];
  role: { member: boolean; leader: boolean; teacher: boolean };
  lastSubmission: {
    submittedBy: string | null;
    mode: string;
    submittedAt: string;
    late: boolean;
    receiptHash: string;
    /** Đã sửa sau lần nộp này: bài lại là Đang làm, trưởng nhóm nộp lại được. */
    changedSince?: boolean;
    /** Tác giả từng mục lúc nộp (BR-U12-44); mục chưa Xong thì `done` false, không vào bản nộp. */
    authors?: { partId: string; title: string; accountId: string | null; done: boolean }[];
  } | null;
  activityMonitoring: boolean;
  /** Bài cho phép Draw.io (U07): trình soạn mục có nút chèn sơ đồ. */
  diagramEnabled: boolean;
  version: number;
  viewerId: string;
};

export type Receipt = {
  documentId: string;
  mode: string;
  submittedAt: string;
  late: boolean;
  receiptHash: string;
  submittedBy: string | null;
  warnings: string[];
};

export type StreamEvent = { type: string; documentId: string; sectionId?: string; receiptHash?: string };
