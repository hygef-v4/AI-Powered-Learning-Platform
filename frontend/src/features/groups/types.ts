export type Person = { accountId: string; name: string | null; email: string | null };

/** `active` sai là "Đã rời lớp" (BR-U12-07). */
export type Member = Person & { active: boolean; leader: boolean };

export type GroupView = { id: string; name: string; leaderId: string | null; members: Member[]; used: boolean };

export type LeaderRequestView = {
  id: string;
  groupId: string;
  groupName: string;
  requestedBy: Person;
  proposedLeader: Person | null;
  reason: string;
  createdAt: string;
};

export type GroupSetView = {
  classId: string;
  classStatus: "DRAFT" | "OPEN" | "ARCHIVED";
  version: number;
  groups: GroupView[];
  ungrouped: Person[];
  pendingRequests: LeaderRequestView[];
};

/** Một nhóm trong bản sửa; `key` là id nhóm cũ hoặc khóa tạm của nhóm mới. */
export type Draft = { key: string; id: string | null; name: string; leaderId: string | null; memberIds: string[] };

export type DraftView = { id: string | null; name: string; leaderId: string | null; memberIds: string[]; generated: boolean };

export type Issue = { groupKey: string; code: string; message: string };

export type GroupAssignment = { assignmentId: string; title: string; status: string; opensAt: string | null; closesAt: string | null };

export type SectionProgress = { id: string; title: string; status: "OPEN" | "CLAIMED" | "DONE"; holderId: string | null };

export type GroupProgress = {
  groupId: string;
  groupName: string;
  leaderId: string | null;
  documentId: string | null;
  documentStatus: string | null;
  done: number;
  claimed: number;
  open: number;
  sections: SectionProgress[];
  submitted: boolean;
  submittedAt: string | null;
  late: boolean;
  submitMode: string | null;
};

export type RequestStatus = "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";

export type MyGroupView = {
  classId: string;
  classStatus: string;
  groupId: string | null;
  name: string | null;
  leaderId: string | null;
  members: { accountId: string; name: string | null; email: string | null; leader: boolean }[];
  works: {
    assignmentId: string;
    title: string;
    closesAt: string | null;
    documentId: string | null;
    doneSections: number;
    totalSections: number;
    submitted: boolean;
    submittedAt: string | null;
    late: boolean;
  }[];
  pendingRequest: boolean;
  myRequests: {
    id: string;
    status: RequestStatus;
    proposedLeaderId: string | null;
    reason: string;
    decisionReason: string | null;
    createdAt: string;
    decidedAt: string | null;
  }[];
};

export type StudentGroup = { groupId: string | null; name: string | null; leader: boolean; memberCount: number };
