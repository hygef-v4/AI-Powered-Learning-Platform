export type DocumentBlock = {
  id: string;
  origin: "TEACHER" | "STUDENT";
  type: "HEADING" | "PARAGRAPH" | "LIST" | "TABLE" | "IMAGE" | "DIAGRAM";
  content: Record<string, unknown>;
};

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
  sharedBlocks: { blocks: DocumentBlock[] };
  sections: SectionView[];
  role: { member: boolean; leader: boolean; teacher: boolean };
  lastSubmission: { submittedBy: string | null; mode: string; submittedAt: string; late: boolean; receiptHash: string } | null;
  activityMonitoring: boolean;
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
