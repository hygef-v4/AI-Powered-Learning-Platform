import { apiFetch } from "@/shared/api/client";
import type { Draft, DraftView, GroupAssignment, GroupProgress, GroupSetView, LeaderRequestView, MyGroupView, StudentGroup } from "./types";

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

export const rejectRequest = (id: string, reason: string) =>
  apiFetch(`/api/v1/leader-requests/${encodeURIComponent(id)}:reject`, { method: "POST", body: JSON.stringify({ reason }) });

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

export const submitLeaderRequest = (groupId: string, reason: string, proposedLeaderId: string | null) =>
  apiFetch(`/api/v1/groups/${encodeURIComponent(groupId)}/leader-requests`, {
    method: "POST",
    body: JSON.stringify({ reason, proposedLeaderId }),
  });

export const cancelLeaderRequest = (id: string) =>
  apiFetch(`/api/v1/leader-requests/${encodeURIComponent(id)}:cancel`, { method: "POST" });
