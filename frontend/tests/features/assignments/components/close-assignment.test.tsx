import { cleanup, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { AssignmentFormPage } from "@/features/assignments/pages/AssignmentFormPage";
import type { AssignmentDetail } from "@/features/assignments/model/types";

const mocks = vi.hoisted(() => ({ detail: vi.fn(), rubric: vi.fn(), closeAssignment: vi.fn() }));
vi.mock("@/features/assignments/api", async (original) => ({
  ...await original<typeof import("@/features/assignments/api")>(), assignmentsApi: mocks,
}));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }),
  useSearchParams: () => new URLSearchParams() }));
vi.mock("@/shared/document", async (original) => ({
  ...await original<typeof import("@/shared/document")>(), DocumentEditor: () => <div>Đề bài</div>,
}));

const assignment: AssignmentDetail = {
  id: "a", scope: "CLASS", classId: "c", subjectId: "s", ownerName: "Lớp",
  sourceAssignmentId: null, origin: "MANUAL", versionNo: 1, submissionMode: "INDIVIDUAL",
  diagramEnabled: false, gradingMode: "GRADED", title: "Bài tập", instructions: "",
  skeleton: { blocks: [] }, parts: [], totalPoints: 0, requiredDiagrams: {}, referenceMaterialIds: [],
  activityMonitoringEnabled: false, activityNotice: null, status: "OPEN", opensAt: null,
  closesAt: "2026-12-01T10:00:00Z", lateUntil: null, maxAttempts: null, retiredReason: null,
  createdAt: "2026-11-15T00:00:00Z", updatedAt: "2026-11-15T00:00:00Z", version: 7,
  permissions: { canManage: true, canEditContent: false, canChangeSchedule: true,
    canRetire: true, canDelete: false, canReview: false, canPublish: false,
    canClone: false, canCreateVersion: false, ownerArchived: false }, groupSupported: false, warnings: [],
};
beforeEach(() => {
  vi.resetAllMocks();
  mocks.detail.mockResolvedValue(assignment);
  mocks.rubric.mockResolvedValue({ partId: "ASSIGNMENT", criteria: [] });
  mocks.closeAssignment.mockResolvedValue({ ...assignment, status: "CLOSED", version: 8 });
});
afterEach(cleanup);

it.each([
  { status: "CLOSED" as const }, { status: "SCHEDULED" as const }, { scope: "SUBJECT" as const },
  { permissions: { ...assignment.permissions, canRetire: false } },
  { permissions: { ...assignment.permissions, ownerArchived: true } },
])("does not offer closing outside an active manageable class assignment: %j", async (changes) => {
  mocks.detail.mockResolvedValue({ ...assignment, ...changes });
  render(<AssignmentFormPage id="a" />);
  await screen.findByText("Đề bài");
  expect(screen.queryByRole("button", { name: "Đóng nhận bài" })).not.toBeInTheDocument();
});
