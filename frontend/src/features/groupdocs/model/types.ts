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
  /** Trưởng nhóm đã chốt mục (BR-U12-39). */
  lockedBy?: string | null;
  lockedAt?: string | null;
};

export type RevisionKind = "DONE" | "TAKEOVER" | "IMPORT";

/** Một bản lịch sử của mục (BR-U12-35), mới nhất trước. */
export type RevisionView = { id: string; authorId: string; kind: RevisionKind; createdAt: string; blocks: DocumentBlock[] };

/** Nhập file cho cả tài liệu (BR-U12-46): nội dung dự kiến của từng mục. */
export type SectionPlan = {
  sectionId: string;
  title: string;
  version: number;
  blocks: DocumentBlock[];
  skipReason: string | null;
  replaces: boolean;
};

export type DocumentImport = { sections: SectionPlan[]; unmatched: string[]; warnings: { code: string; message: string }[] };

export type ApplyResult = { applied: number; skipped: string[] };

/** Đánh giá chéo (BR-U12-47). */
export type PeerGiven = { revieweeId: string; score: number; comment: string | null };

export type PeerSummary = {
  accountId: string;
  average: number | null;
  outOfTen: number | null;
  count: number;
  reviews: { reviewerId: string; score: number; comment: string | null; updatedAt: string }[];
};

export type PeerReviewView = {
  open: boolean;
  closedReason: string | null;
  closesAt: string | null;
  mine: PeerGiven[];
  summary: PeerSummary[];
  reviewers: number;
  members: number;
};

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
  lastSubmission: { submittedBy: string | null; mode: string; submittedAt: string; late: boolean; receiptHash: string } | null;
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

export type StreamEvent = { type: string; documentId: string; sectionId?: string; holderId?: string | null; receiptHash?: string };
