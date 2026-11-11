import { apiFetch } from "@/shared/api/client";
import type {
  ApproveResult,
  ClassProposalsView,
  Draft,
  DraftView,
  FormationView,
  GroupAssignment,
  GroupProgress,
  GroupSetView,
  LeaderRequestView,
  MyGroupView,
  MyProposalView,
  StudentGroup,
} from "../model/types";

const classBase = (classId: string) => `/api/v1/classes/${encodeURIComponent(classId)}`;

export const getGroups = (classId: string) => apiFetch<GroupSetView>(`${classBase(classId)}/groups`);

export const saveGroups = (classId: string, version: number, groups: Draft[]) =>
  apiFetch<GroupSetView>(`${classBase(classId)}/groups`, { method: "PUT", body: JSON.stringify({ version, groups }) });

export const randomPreview = (classId: string, maxSize: number) =>
  apiFetch<DraftView[]>(`${classBase(classId)}/groups:random-preview`, { method: "POST", body: JSON.stringify({ maxSize }) });

export const getLeaderRequests = (classId: string) => apiFetch<LeaderRequestView[]>(`${classBase(classId)}/leader-requests`);

export const approveRequest = (id: string, newLeaderId: string | null) =>
  apiFetch(`/api/v1/leader-requests/${encodeURIComponent(id)}:approve`, {
    method: "POST",
    body: JSON.stringify({ newLeaderId }),
  });

/** Từ chối không kèm lý do. */
export const rejectRequest = (id: string) =>
  apiFetch(`/api/v1/leader-requests/${encodeURIComponent(id)}:reject`, { method: "POST", body: JSON.stringify({}) });

export const getGroupAssignments = (classId: string) => apiFetch<GroupAssignment[]>(`${classBase(classId)}/group-assignments`);

export const getProgress = (classId: string, assignmentId: string) =>
  apiFetch<GroupProgress[]>(`${classBase(classId)}/group-assignments/${encodeURIComponent(assignmentId)}/progress`);

export const releaseSection = (documentId: string, sectionId: string) =>
  apiFetch(`/api/v1/group-docs/${encodeURIComponent(documentId)}/sections/${encodeURIComponent(sectionId)}:release`, {
    method: "POST",
  });

export const getStudentGroup = (classId: string, studentId: string) =>
  apiFetch<StudentGroup>(`${classBase(classId)}/students/${encodeURIComponent(studentId)}/group`);

export const getMyGroup = (classId: string) => apiFetch<MyGroupView>(`/api/v1/me/classes/${encodeURIComponent(classId)}/group`);

/** Gửi yêu cầu đổi trưởng nhóm, không kèm lý do. */
export const submitLeaderRequest = (groupId: string, proposedLeaderId: string | null) =>
  apiFetch(`/api/v1/groups/${encodeURIComponent(groupId)}/leader-requests`, {
    method: "POST",
    body: JSON.stringify({ proposedLeaderId }),
  });

/** Trưởng nhóm đổi tên nhóm, không cần giảng viên duyệt. */
export const renameGroup = (groupId: string, name: string) =>
  apiFetch<{ groupId: string; name: string }>(`/api/v1/groups/${encodeURIComponent(groupId)}/name`, {
    method: "PUT",
    body: JSON.stringify({ name }),
  });

export const cancelLeaderRequest = (id: string) =>
  apiFetch(`/api/v1/leader-requests/${encodeURIComponent(id)}:cancel`, { method: "POST" });

// ---- Sinh viên tự chia nhóm (US-GRP-007…010) ----------------------------------------------------------------

const proposalBase = (proposalId: string) => `/api/v1/group-proposals/${encodeURIComponent(proposalId)}`;

export const getFormation = (classId: string) => apiFetch<FormationView>(`${classBase(classId)}/group-formation`);

export type FormationInput = { enabled: boolean; minSize: number; maxSize: number; deadline: string | null; version: number };

export const updateFormation = (classId: string, input: FormationInput) =>
  apiFetch<FormationView>(`${classBase(classId)}/group-formation`, { method: "PUT", body: JSON.stringify(input) });

export const getClassProposals = (classId: string) => apiFetch<ClassProposalsView>(`${classBase(classId)}/group-proposals`);

export const approveProposals = (classId: string, proposalIds: string[]) =>
  apiFetch<ApproveResult>(`${classBase(classId)}/group-proposals:approve`, {
    method: "POST",
    body: JSON.stringify({ proposalIds }),
  });

/** Lý do tùy chọn, tối đa 1.000 ký tự; trống thì gửi null. */
export const rejectProposal = (proposalId: string, reason: string | null) =>
  apiFetch<ClassProposalsView>(`${proposalBase(proposalId)}:reject`, { method: "POST", body: JSON.stringify({ reason }) });

export const getMyProposal = (classId: string) =>
  apiFetch<MyProposalView>(`/api/v1/me/classes/${encodeURIComponent(classId)}/group-proposal`);

export const createProposal = (classId: string, name: string) =>
  apiFetch<MyProposalView>(`${classBase(classId)}/group-proposals`, { method: "POST", body: JSON.stringify({ name }) });

export const renameProposal = (proposalId: string, name: string, version: number) =>
  apiFetch<MyProposalView>(`${proposalBase(proposalId)}/name`, { method: "PUT", body: JSON.stringify({ name, version }) });

export const inviteToProposal = (proposalId: string, accountIds: string[], version: number) =>
  apiFetch<MyProposalView>(`${proposalBase(proposalId)}/invitations`, {
    method: "POST",
    body: JSON.stringify({ accountIds, version }),
  });

export const revokeInvitation = (proposalId: string, accountId: string, version: number) =>
  apiFetch<MyProposalView>(`${proposalBase(proposalId)}/invitations/${encodeURIComponent(accountId)}:revoke`, {
    method: "POST",
    body: JSON.stringify({ version }),
  });

/** Thao tác của người được mời hoặc thành viên, không cần version. */
export const answerProposal = (proposalId: string, action: "accept" | "decline" | "leave") =>
  apiFetch<MyProposalView>(`${proposalBase(proposalId)}:${action}`, { method: "POST" });

/** Thao tác của trưởng nhóm, kèm version của đề xuất. */
export const changeProposal = (proposalId: string, action: "submit" | "withdraw" | "cancel", version: number) =>
  apiFetch<MyProposalView>(`${proposalBase(proposalId)}:${action}`, { method: "POST", body: JSON.stringify({ version }) });
