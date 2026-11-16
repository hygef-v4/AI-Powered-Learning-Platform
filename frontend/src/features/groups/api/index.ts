import { fetchApi } from "@/shared/api/client";
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

const buildClassBase = (classId: string) => `/api/v1/classes/${encodeURIComponent(classId)}`;

export const getGroups = (classId: string) => fetchApi<GroupSetView>(`${buildClassBase(classId)}/groups`);

export const saveGroups = (classId: string, version: number, groups: Draft[]) =>
  fetchApi<GroupSetView>(`${buildClassBase(classId)}/groups`, { method: "PUT", body: JSON.stringify({ version, groups }) });

export const previewRandomGroups = (classId: string, maxSize: number) =>
  fetchApi<DraftView[]>(`${buildClassBase(classId)}/groups:random-preview`, { method: "POST", body: JSON.stringify({ maxSize }) });

export const getLeaderRequests = (classId: string) => fetchApi<LeaderRequestView[]>(`${buildClassBase(classId)}/leader-requests`);

export const approveRequest = (id: string, newLeaderId: string | null) =>
  fetchApi(`/api/v1/leader-requests/${encodeURIComponent(id)}:approve`, {
    method: "POST",
    body: JSON.stringify({ newLeaderId }),
  });

/** Từ chối không kèm lý do. */
export const rejectRequest = (id: string) =>
  fetchApi(`/api/v1/leader-requests/${encodeURIComponent(id)}:reject`, { method: "POST", body: JSON.stringify({}) });

export const getGroupAssignments = (classId: string) => fetchApi<GroupAssignment[]>(`${buildClassBase(classId)}/group-assignments`);

export const getProgress = (classId: string, assignmentId: string) =>
  fetchApi<GroupProgress[]>(`${buildClassBase(classId)}/group-assignments/${encodeURIComponent(assignmentId)}/progress`);

export const releaseSection = (documentId: string, sectionId: string) =>
  fetchApi(`/api/v1/group-docs/${encodeURIComponent(documentId)}/sections/${encodeURIComponent(sectionId)}:release`, {
    method: "POST",
  });

export const getStudentGroup = (classId: string, studentId: string) =>
  fetchApi<StudentGroup>(`${buildClassBase(classId)}/students/${encodeURIComponent(studentId)}/group`);

export const getMyGroup = (classId: string) => fetchApi<MyGroupView>(`/api/v1/me/classes/${encodeURIComponent(classId)}/group`);

/** Gửi yêu cầu đổi trưởng nhóm, không kèm lý do. */
export const submitLeaderRequest = (groupId: string, proposedLeaderId: string | null) =>
  fetchApi(`/api/v1/groups/${encodeURIComponent(groupId)}/leader-requests`, {
    method: "POST",
    body: JSON.stringify({ proposedLeaderId }),
  });

/** Trưởng nhóm đổi tên nhóm, không cần giảng viên duyệt. */
export const renameGroup = (groupId: string, name: string) =>
  fetchApi<{ groupId: string; name: string }>(`/api/v1/groups/${encodeURIComponent(groupId)}/name`, {
    method: "PUT",
    body: JSON.stringify({ name }),
  });

export const cancelLeaderRequest = (id: string) =>
  fetchApi(`/api/v1/leader-requests/${encodeURIComponent(id)}:cancel`, { method: "POST" });

// ---- Sinh viên tự chia nhóm (US-GRP-007…010) ----------------------------------------------------------------

const buildProposalBase = (proposalId: string) => `/api/v1/group-proposals/${encodeURIComponent(proposalId)}`;

export const getFormation = (classId: string) => fetchApi<FormationView>(`${buildClassBase(classId)}/group-formation`);

export type FormationInput = { enabled: boolean; minSize: number; maxSize: number; deadline: string | null; version: number };

export const updateFormation = (classId: string, input: FormationInput) =>
  fetchApi<FormationView>(`${buildClassBase(classId)}/group-formation`, { method: "PUT", body: JSON.stringify(input) });

export const getClassProposals = (classId: string) => fetchApi<ClassProposalsView>(`${buildClassBase(classId)}/group-proposals`);

export const approveProposals = (classId: string, proposalIds: string[]) =>
  fetchApi<ApproveResult>(`${buildClassBase(classId)}/group-proposals:approve`, {
    method: "POST",
    body: JSON.stringify({ proposalIds }),
  });

/** Lý do tùy chọn, tối đa 1.000 ký tự; trống thì gửi null. */
export const rejectProposal = (proposalId: string, reason: string | null) =>
  fetchApi<ClassProposalsView>(`${buildProposalBase(proposalId)}:reject`, { method: "POST", body: JSON.stringify({ reason }) });

export const getMyProposal = (classId: string) =>
  fetchApi<MyProposalView>(`/api/v1/me/classes/${encodeURIComponent(classId)}/group-proposal`);

export const createProposal = (classId: string, name: string) =>
  fetchApi<MyProposalView>(`${buildClassBase(classId)}/group-proposals`, { method: "POST", body: JSON.stringify({ name }) });

export const renameProposal = (proposalId: string, name: string, version: number) =>
  fetchApi<MyProposalView>(`${buildProposalBase(proposalId)}/name`, { method: "PUT", body: JSON.stringify({ name, version }) });

export const inviteToProposal = (proposalId: string, accountIds: string[], version: number) =>
  fetchApi<MyProposalView>(`${buildProposalBase(proposalId)}/invitations`, {
    method: "POST",
    body: JSON.stringify({ accountIds, version }),
  });

export const revokeInvitation = (proposalId: string, accountId: string, version: number) =>
  fetchApi<MyProposalView>(`${buildProposalBase(proposalId)}/invitations/${encodeURIComponent(accountId)}:revoke`, {
    method: "POST",
    body: JSON.stringify({ version }),
  });

/** Thao tác của người được mời hoặc thành viên, không cần version. */
export const answerProposal = (proposalId: string, action: "accept" | "decline" | "leave") =>
  fetchApi<MyProposalView>(`${buildProposalBase(proposalId)}:${action}`, { method: "POST" });

/** Thao tác của trưởng nhóm, kèm version của đề xuất. */
export const changeProposal = (proposalId: string, action: "submit" | "withdraw" | "cancel", version: number) =>
  fetchApi<MyProposalView>(`${buildProposalBase(proposalId)}:${action}`, { method: "POST", body: JSON.stringify({ version }) });
