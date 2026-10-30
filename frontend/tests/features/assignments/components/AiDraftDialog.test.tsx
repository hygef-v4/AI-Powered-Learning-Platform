import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { assignmentsApi } from "@/features/assignments/api";
import { AiDraftDialog } from "@/features/assignments/components/AiDraftDialog";
import type { AiDraft } from "@/features/assignments/model/types";
import * as filesApi from "@/features/files/api";

describe("AiDraftDialog", () => {
  afterEach(() => vi.restoreAllMocks());

  it("opens an existing MCP proposal for review without submitting a provider request", async () => {
    vi.spyOn(assignmentsApi, "materials").mockResolvedValue([]);
    vi.spyOn(filesApi, "getPolicies").mockResolvedValue([]);
    const generate = vi.spyOn(assignmentsApi, "requestAiDraft");
    const load = vi.spyOn(assignmentsApi, "aiDraft").mockResolvedValue({
      draftId: "mcp-proposal", assignmentId: "a1", origin: "MCP", status: "READY", taskId: null,
      proposal: { title: "MCP review title", businessContext: "A QA system", instructions: "Review before applying", sections: [
        { level: 1, title: "MCP section", guidance: "Explain the design", rubric: [] },
      ] }, provenance: {}, safeError: null, baseVersion: 1, createdAt: "now", decidedAt: null,
    });
    render(<AiDraftDialog assignmentId="a1" initialDraftId="mcp-proposal" version={1} currentPartCount={0}
      currentTitle="" currentInstructions="" gradingMode="GRADED" onClose={vi.fn()} onApplied={vi.fn()} />);
    expect(await screen.findByText("MCP review title")).toBeInTheDocument();
    expect(load).toHaveBeenCalledWith("a1", "mcp-proposal", expect.any(AbortSignal));
    expect(generate).not.toHaveBeenCalled();
  });

  it("lets the AI infer assessment style and sends language, question count and target points", async () => {
    vi.spyOn(assignmentsApi, "materials").mockResolvedValue([]);
    const requestAiDraft = vi.spyOn(assignmentsApi, "requestAiDraft").mockResolvedValue({
      draftId: "d1", assignmentId: "a1", origin: "AI_TASK", status: "PENDING", taskId: "t1", proposal: null,
      provenance: {}, safeError: null, baseVersion: 1, createdAt: "now", decidedAt: null,
    });
    render(<AiDraftDialog assignmentId="a1" version={1} currentPartCount={0} currentTitle="" currentInstructions=""
      gradingMode="GRADED" onClose={vi.fn()} onApplied={vi.fn()} />);

    fireEvent.change(screen.getByLabelText("Ngôn ngữ đề"), { target: { value: "EN" } });
    fireEvent.change(screen.getByLabelText("Số yêu cầu chính"), { target: { value: "3" } });
    fireEvent.change(screen.getByLabelText("Tổng điểm mục tiêu"), { target: { value: "10" } });
    fireEvent.change(screen.getByRole("textbox", { name: /Yêu cầu/ }), { target: { value: "Pharmacy management system" } });
    fireEvent.click(screen.getByTestId("ai-send"));

    await waitFor(() => expect(requestAiDraft).toHaveBeenCalledWith("a1", expect.objectContaining({
      request: "Pharmacy management system",
      options: { profile: "AUTO", language: "EN", questionCount: 3, targetPoints: 10 },
      sourceFileRefs: [],
    })));
  });

  it.each([
    ["Bài tập chưa đặt tên", "", true],
    ["My existing exam", "Submit a PDF report.", false],
  ] as const)("uses safe title/instruction defaults for %s", async (currentTitle, currentInstructions, replace) => {
    vi.spyOn(assignmentsApi, "materials").mockResolvedValue([]);
    const ready: AiDraft = {
      draftId: "d1", assignmentId: "a1", origin: "AI_TASK", status: "READY", taskId: null,
      proposal: { title: "New exam", businessContext: "An event booking platform.", instructions: "Work individually.",
        sections: [{ level: 1, title: "Design the model", guidance: "Describe the domain.", rubric: [] }] },
      provenance: {}, safeError: null, baseVersion: 1, createdAt: "now", decidedAt: null,
    };
    vi.spyOn(assignmentsApi, "requestAiDraft").mockResolvedValue(ready);
    render(<AiDraftDialog assignmentId="a1" version={1} currentPartCount={0} currentTitle={currentTitle}
      currentInstructions={currentInstructions} gradingMode="GRADED" onClose={vi.fn()} onApplied={vi.fn()} />);
    fireEvent.change(screen.getByRole("textbox", { name: "Yêu cầu" }), { target: { value: "Create an event exam" } });
    fireEvent.click(screen.getByTestId("ai-send"));

    const titleChoice = await screen.findByLabelText("Thay tiêu đề bài bằng tiêu đề đề xuất");
    expect(titleChoice).toHaveProperty("checked", replace);
    const instructionChoice = screen.getByLabelText(currentInstructions
      ? "Thay hướng dẫn làm bài hiện tại" : "Thêm hướng dẫn làm bài do AI tạo");
    expect(instructionChoice).toHaveProperty("checked", replace);
  });

  it("reads an uploaded source before enabling a file-only generation request", async () => {
    vi.spyOn(assignmentsApi, "materials").mockResolvedValue([]);
    vi.spyOn(filesApi, "getPolicies").mockResolvedValue([{ purpose: "ASSIGNMENT_SOURCE", maxSizeMb: 2,
      allowedTypes: ["TXT"], extensions: ["txt"], mediaTypes: ["text/plain"] }]);
    vi.spyOn(filesApi, "uploadFile").mockReturnValue({ abort: vi.fn(), promise: Promise.resolve({
      fileRef: { value: "source-ref" }, mediaType: "text/plain", byteSize: 25,
      originalFileName: "brief.txt", expiresAt: "later",
    }) });
    const preview = vi.spyOn(assignmentsApi, "previewAiSources").mockResolvedValue([
      { fileName: "brief.txt", previewText: "Event booking business case", extractedChars: 27, truncated: false },
    ]);
    const request = vi.spyOn(assignmentsApi, "requestAiDraft").mockResolvedValue({
      draftId: "d1", assignmentId: "a1", origin: "AI_TASK", status: "PENDING", taskId: null, proposal: null,
      provenance: {}, safeError: null, baseVersion: 1, createdAt: "now", decidedAt: null,
    });
    render(<AiDraftDialog assignmentId="a1" version={1} currentPartCount={0} currentTitle="" currentInstructions=""
      gradingMode="GRADED" onClose={vi.fn()} onApplied={vi.fn()} />);
    const input = screen.getByLabelText("Tệp nguồn riêng cho AI");
    await waitFor(() => expect(input).toBeEnabled());
    fireEvent.change(input, { target: { files: [new File(["Event booking business case"], "brief.txt", { type: "text/plain" })] } });
    await screen.findByText("Đã đọc · 27 ký tự");
    expect(preview).toHaveBeenCalledWith("a1", ["source-ref"], expect.any(AbortSignal));
    expect(screen.getByText("Event booking business case")).toBeInTheDocument();
    expect(screen.getByTestId("ai-send")).toBeEnabled();
    fireEvent.click(screen.getByTestId("ai-send"));
    await waitFor(() => expect(request).toHaveBeenCalledWith("a1", expect.objectContaining({ request: "", sourceFileRefs: ["source-ref"] })));
  });

  it("blocks empty drafts and lets instructors revise without automatically calling AI again", async () => {
    vi.spyOn(assignmentsApi, "materials").mockResolvedValue([]);
    const invalid: AiDraft = {
      draftId: "d1", assignmentId: "a1", origin: "AI_TASK", status: "READY", taskId: null,
      proposal: { title: "Event exam", businessContext: "A long case", instructions: "Work individually", sections: [],
        validation: { status: "INVALID", questionCount: 0, rubricLeafCount: 0, totalPoints: 0,
          issues: [{ severity: "ERROR", code: "PROPOSAL_STRUCTURE_INVALID", path: "sections", message: "No questions" }] } },
      provenance: {}, safeError: null, baseVersion: 1, createdAt: "now", decidedAt: null,
    };
    const request = vi.spyOn(assignmentsApi, "requestAiDraft").mockResolvedValue(invalid);
    const discard = vi.spyOn(assignmentsApi, "discardAiDraft").mockResolvedValue({ ...invalid, status: "DISCARDED" });
    render(<AiDraftDialog assignmentId="a1" version={1} currentPartCount={0} currentTitle="" currentInstructions=""
      gradingMode="GRADED" onClose={vi.fn()} onApplied={vi.fn()} />);
    fireEvent.change(screen.getByRole("textbox", { name: "Yêu cầu" }), { target: { value: "Create an event exam" } });
    fireEvent.click(screen.getByTestId("ai-send"));
    expect(await screen.findByTestId("ai-apply")).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "Sửa yêu cầu và tạo lại" }));
    await waitFor(() => expect(discard).toHaveBeenCalledWith("a1", "d1"));
    expect(await screen.findByRole("textbox", { name: "Yêu cầu" })).toHaveValue("Create an event exam");
    expect(request).toHaveBeenCalledTimes(1);
  });
});
