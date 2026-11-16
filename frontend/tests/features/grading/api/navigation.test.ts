import { beforeEach, expect, it, vi } from "vitest";
import { fetchApi, downloadApiFile } from "@/shared/api/client";
import { gradingApi } from "@/features/grading/api";

vi.mock("@/shared/api/client", () => ({ fetchApi: vi.fn(), downloadApiFile: vi.fn() }));
beforeEach(() => vi.clearAllMocks());

it("sends the optional contribution level atomically with the member grade and supports clearing it", () => {
  gradingApi.saveMemberGrade("group", "student", 3, "7", "Thiếu luồng lỗi", "PARTIALLY_MEETS");
  expect(fetchApi).toHaveBeenLastCalledWith("/api/v1/evaluations/group/members/student", {
    method: "PUT",
    body: JSON.stringify({ version: 3, score: "7", reason: "Thiếu luồng lỗi", contributionLevel: "PARTIALLY_MEETS" }),
  });
  gradingApi.saveMemberGrade("group", "student", 4, "7", null, null);
  expect(fetchApi).toHaveBeenLastCalledWith("/api/v1/evaluations/group/members/student", {
    method: "PUT",
    body: JSON.stringify({ version: 4, score: "7", reason: null, contributionLevel: null }),
  });
});

it("exports the selected scope with server filename metadata", () => {
  gradingApi.exportGrades("c", "xlsx", "a");
  expect(downloadApiFile).toHaveBeenLastCalledWith(
    "/api/v1/classes/c/gradebook/export?format=xlsx&assignmentId=a",
  );
  gradingApi.exportGrades("c", "csv");
  expect(downloadApiFile).toHaveBeenLastCalledWith("/api/v1/classes/c/gradebook/export?format=csv");
});

it("keeps direct/MCP evaluation reads free of navigation parameters", () => {
  gradingApi.loadEvaluation("e");
  expect(fetchApi).toHaveBeenCalledWith("/api/v1/evaluations/e");
});

it("serializes both true and false late filters with the originating status", () => {
  gradingApi.loadEvaluation("e", { status: "DRAFT", late: "false" });
  expect(fetchApi).toHaveBeenLastCalledWith("/api/v1/evaluations/e?status=DRAFT&late=false");
  gradingApi.loadEvaluation("e", { status: "PENDING", late: "true" });
  expect(fetchApi).toHaveBeenLastCalledWith("/api/v1/evaluations/e?status=PENDING&late=true");
});
