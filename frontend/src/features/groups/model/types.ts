/** `studentCode`: MSSV, null khi chưa có (BR-U12-97). */
export type Person = { accountId: string; name: string | null; email: string | null; studentCode?: string | null };

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
  members: { accountId: string; name: string | null; email: string | null; leader: boolean; studentCode?: string | null }[];
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
  /** Người đang xem là trưởng nhóm: chỉ trưởng nhóm đổi được tên nhóm. */
  amLeader: boolean;
};

export type StudentGroup = { groupId: string | null; name: string | null; leader: boolean; memberCount: number };

// ---- Sinh viên tự chia nhóm (US-GRP-007…010) ----------------------------------------------------------------

/** Lý do đợt tự chia nhóm đang đóng; `null` khi đợt mở. */
export type FormationClosedReason = "DISABLED" | "CLASS_NOT_OPEN" | "DEADLINE_PASSED";

export type FormationView = {
  classId: string;
  enabled: boolean;
  minSize: number;
  maxSize: number;
  deadline: string | null;
  version: number;
  open: boolean;
  closedReason: FormationClosedReason | null;
};

export type FormationSummary = Pick<FormationView, "enabled" | "minSize" | "maxSize" | "deadline" | "open" | "closedReason">;

export type ProposalStatus = "DRAFT" | "SUBMITTED" | "APPROVED" | "REJECTED" | "CANCELLED";

/** Thành viên hoặc lời mời của đề xuất; `expired` chỉ dùng cho lời mời khi đợt đóng. */
export type ProposalMemberView = Person & { leader: boolean; expired: boolean };

export type ProposalView = {
  id: string;
  name: string;
  status: ProposalStatus;
  leaderId: string;
  leader: boolean;
  version: number;
  members: ProposalMemberView[];
  invitations: ProposalMemberView[];
  freeSeats: number;
  submittedAt: string | null;
};

export type InvitationView = {
  proposalId: string;
  proposalName: string;
  status: ProposalStatus;
  leaderName: string | null;
  memberCount: number;
  expired: boolean;
  canAccept: boolean;
};

export type RejectedProposalView = { proposalId: string; name: string; reason: string | null; decidedAt: string | null };

/** Bạn mời được: chỉ id, họ tên, email và cờ đang ở đề xuất khác (BR-U12-92). */
export type CandidateView = Person & { inOtherProposal: boolean };

/** Màn đề xuất của sinh viên; `grouped` đúng thì sinh viên đã có nhóm chính thức. */
export type MyProposalView = {
  classId: string;
  classStatus: string;
  grouped: boolean;
  formation: FormationSummary;
  proposal: ProposalView | null;
  invitations: InvitationView[];
  lastRejected: RejectedProposalView | null;
  candidates: CandidateView[];
};

export type ProposalWarning = "LEADER_INACTIVE" | "BELOW_MIN" | "ABOVE_MAX";

export type ReviewProposalView = {
  id: string;
  name: string;
  status: ProposalStatus;
  leader: Person;
  members: Person[];
  pendingInvitations: number;
  submittedAt: string | null;
  warnings: ProposalWarning[];
};

export type UngroupedView = { person: Person; proposalId: string | null; proposalName: string | null; proposalStatus: ProposalStatus | null };

export type ClassProposalsView = {
  classId: string;
  formation: FormationView;
  proposals: ReviewProposalView[];
  ungrouped: UngroupedView[];
};

export type ApproveResult = {
  approved: { proposalId: string; groupId: string }[];
  failed: { proposalId: string; code: string; message: string }[];
  groupSetVersion: number;
};
