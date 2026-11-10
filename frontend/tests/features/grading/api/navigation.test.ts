import { beforeEach, expect, it, vi } from "vitest";
import { apiFetch } from "@/shared/api/client";
import { gradingApi } from "@/features/grading/api";

vi.mock("@/shared/api/client", () => ({ apiFetch: vi.fn(), apiDownload: vi.fn() }));
beforeEach(() => vi.clearAllMocks());

it("keeps direct/MCP evaluation reads free of navigation parameters", () => {
  gradingApi.loadEvaluation("e");
  expect(apiFetch).toHaveBeenCalledWith("/api/v1/evaluations/e");
});

it("serializes both true and false late filters with the originating status", () => {
  gradingApi.loadEvaluation("e", { status: "DRAFT", late: "false" });
  expect(apiFetch).toHaveBeenLastCalledWith("/api/v1/evaluations/e?status=DRAFT&late=false");
  gradingApi.loadEvaluation("e", { status: "PENDING", late: "true" });
  expect(apiFetch).toHaveBeenLastCalledWith("/api/v1/evaluations/e?status=PENDING&late=true");
});
