import { rememberTemplate } from "@/features/assignments/model/templateStore";
import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import * as ui from "@/shared/ui";
import { afterEach, describe, expect, it, vi } from "vitest";
import { assignmentsApi } from "@/features/assignments/api";
import { AiDraftDialog } from "@/features/assignments/components/AiDraftDialog";
import type { AiDraft } from "@/features/assignments/model/types";
import * as filesApi from "@/features/files/api";

const MATERIAL = [{ materialId: "m1", title: "UML basics", hasText: true }] as never;

/** Chọn một tài liệu tham khảo (bắt buộc) qua ô tìm. */
async function pickReference(title = "UML basics") {
  const search = screen.getByRole("combobox", { name: "Tài liệu tham khảo" });
  fireEvent.focus(search);
  fireEvent.mouseDown(await within(await screen.findByRole("listbox")).findByRole("option", { name: new RegExp(title) }));
  fireEvent.blur(search);
}

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

  it("defaults to English, requires a question count and lets the system pick the AI source", async () => {
    vi.spyOn(assignmentsApi, "materials").mockResolvedValue(MATERIAL);
    const requestAiDraft = vi.spyOn(assignmentsApi, "requestAiDraft").mockResolvedValue({
      draftId: "d1", assignmentId: "a1", origin: "AI_TASK", status: "PENDING", taskId: "t1", proposal: null,
      provenance: {}, safeError: null, baseVersion: 1, createdAt: "now", decidedAt: null,
    });
    render(<AiDraftDialog assignmentId="a1" version={1} currentPartCount={0} currentTitle="" currentInstructions=""
      gradingMode="GRADED" onClose={vi.fn()} onApplied={vi.fn()} />);

    expect(screen.getByLabelText("Ngôn ngữ")).toHaveValue("EN");
    expect(screen.getByLabelText("Số câu hỏi")).toHaveValue(null);
    expect(screen.queryByLabelText("Tổng điểm mục tiêu")).not.toBeInTheDocument();
    expect(screen.queryByLabelText("Nguồn AI")).not.toBeInTheDocument();
    fireEvent.change(screen.getByRole("textbox", { name: /Yêu cầu/ }), { target: { value: "Pharmacy management system" } });
    // Chưa có số câu hỏi, chưa có tài liệu tham khảo: chưa tạo đề được.
    expect(screen.getByTestId("ai-send")).toBeDisabled();
    await pickReference();
    expect(screen.getByTestId("ai-send")).toBeDisabled();
    fireEvent.change(screen.getByLabelText("Số câu hỏi"), { target: { value: "11" } });
    expect(screen.getByText("Số câu hỏi từ 1 đến 10.")).toBeInTheDocument();
    expect(screen.queryByRole("alert", { name: /Nhập/ })).toBeNull();
    expect(screen.getByTestId("ai-send")).toBeDisabled();
    fireEvent.change(screen.getByLabelText("Số câu hỏi"), { target: { value: "3" } });
    expect(screen.queryByText("Số câu hỏi từ 1 đến 10.")).toBeNull();
    fireEvent.click(screen.getByTestId("ai-send"));

    await waitFor(() => expect(requestAiDraft).toHaveBeenCalledWith("a1", expect.objectContaining({
      request: "Pharmacy management system",
      options: { profile: "AUTO", language: "EN", questionCount: 3 },
      materialIds: ["m1"],
      sourceFileRefs: [],
    })));
    expect(requestAiDraft.mock.calls[0]![1]).not.toHaveProperty("source");
  });

  it("writes a valid proposal straight onto the page with safe title/instruction defaults", async () => {
    vi.spyOn(assignmentsApi, "materials").mockResolvedValue(MATERIAL);
    const ready: AiDraft = {
      draftId: "d1", assignmentId: "a1", origin: "AI_TASK", status: "READY", taskId: null,
      proposal: { title: "New exam", businessContext: "An event booking platform.", instructions: "Work individually.",
        sections: [{ level: 1, title: "Design the model", guidance: "Describe the domain.", rubric: [] }],
        validation: { status: "VALID", questionCount: 1, rubricLeafCount: 1, totalPoints: 10, issues: [] } },
      provenance: {}, safeError: null, baseVersion: 1, createdAt: "now", decidedAt: null,
    };
    vi.spyOn(assignmentsApi, "requestAiDraft").mockResolvedValue(ready);
    const applyDraft = vi.spyOn(assignmentsApi, "applyAiDraft").mockResolvedValue({} as never);
    const onApplied = vi.fn();
    render(<AiDraftDialog assignmentId="a1" version={4} currentPartCount={0} currentTitle="My existing exam"
      currentInstructions="" gradingMode="GRADED" onClose={vi.fn()} onApplied={onApplied} />);
    fireEvent.change(screen.getByRole("textbox", { name: "Yêu cầu" }), { target: { value: "Create an event exam" } });
    await pickReference();
    fireEvent.change(screen.getByLabelText("Số câu hỏi"), { target: { value: "2" } });
    fireEvent.click(screen.getByTestId("ai-send"));

    await waitFor(() => expect(onApplied).toHaveBeenCalled());
    expect(applyDraft).toHaveBeenCalledWith("a1", "d1", 4, true, false);
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
    const input = screen.getByLabelText("Tải tài liệu lên");
    await waitFor(() => expect(screen.getByLabelText("Upload đề mẫu")).toBeEnabled());
    fireEvent.change(input, { target: { files: [new File(["Event booking business case"], "brief.txt", { type: "text/plain" })] } });
    expect(await within(screen.getByRole("list", { name: "Tài liệu đã chọn" })).findByText("brief.txt")).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByText("Đang đọc…")).toBeNull());
    expect(preview).toHaveBeenCalledWith("a1", ["source-ref"], expect.any(AbortSignal));
    fireEvent.change(screen.getByLabelText("Số câu hỏi"), { target: { value: "2" } });
    expect(screen.getByTestId("ai-send")).toBeEnabled();
    fireEvent.click(screen.getByTestId("ai-send"));
    await waitFor(() => expect(request).toHaveBeenCalledWith("a1", expect.objectContaining({ request: "", sourceFileRefs: ["source-ref"] })));
  });

  it("searches reference materials, shows suggestions below and offers upload as the last suggestion", async () => {
    vi.spyOn(assignmentsApi, "materials").mockResolvedValue([
      { materialId: "m1", title: "UML basics", hasText: true },
      { materialId: "m2", title: "Database design", hasText: true },
    ] as never);
    const requestAiDraft = vi.spyOn(assignmentsApi, "requestAiDraft").mockResolvedValue({
      draftId: "d1", assignmentId: "a1", origin: "AI_TASK", status: "PENDING", taskId: null, proposal: null,
      provenance: {}, safeError: null, baseVersion: 1, createdAt: "now", decidedAt: null,
    });
    render(<AiDraftDialog assignmentId="a1" version={1} currentPartCount={0} currentTitle="" currentInstructions=""
      gradingMode="GRADED" onClose={vi.fn()} onApplied={vi.fn()} />);
    const search = screen.getByRole("combobox", { name: "Tài liệu tham khảo" });
    await waitFor(() => expect(search).toBeEnabled());
    fireEvent.focus(search);
    fireEvent.change(search, { target: { value: "data" } });
    const options = within(await screen.findByRole("listbox")).getAllByRole("option");
    expect(options.map((o) => o.textContent)).toEqual(["Database design", "Tải tài liệu lên"]);
    fireEvent.mouseDown(options[0]!);
    fireEvent.change(search, { target: { value: "" } });
    fireEvent.mouseDown(within(screen.getByRole("listbox")).getByRole("option", { name: /UML basics/ }));
    expect(within(screen.getByRole("list", { name: "Tài liệu đã chọn" })).getAllByRole("listitem")).toHaveLength(2);
    fireEvent.change(screen.getByLabelText("Số câu hỏi"), { target: { value: "2" } });
    fireEvent.click(screen.getByTestId("ai-send"));
    await waitFor(() => expect(requestAiDraft).toHaveBeenCalledWith("a1", expect.objectContaining({ materialIds: ["m2", "m1"] })));
  });

  it("shows a separate waiting popup, then returns to the untouched form with a corner toast on failure", async () => {
    vi.spyOn(assignmentsApi, "materials").mockResolvedValue(MATERIAL);
    const toast = vi.spyOn(ui, "showErrorToast").mockImplementation(() => {});
    let fail: (error: Error) => void = () => {};
    vi.spyOn(assignmentsApi, "requestAiDraft").mockImplementation(() => new Promise((_, reject) => { fail = reject; }));
    render(<AiDraftDialog assignmentId="a1" version={1} currentPartCount={0} currentTitle="" currentInstructions=""
      gradingMode="GRADED" onClose={vi.fn()} onApplied={vi.fn()} />);
    fireEvent.change(screen.getByRole("textbox", { name: "Yêu cầu" }), { target: { value: "Create an event exam" } });
    await pickReference();
    fireEvent.change(screen.getByLabelText("Số câu hỏi"), { target: { value: "4" } });
    fireEvent.change(screen.getByLabelText("Ngôn ngữ"), { target: { value: "VI" } });
    fireEvent.click(screen.getByTestId("ai-send"));

    expect(await screen.findByTestId("ai-progress")).toHaveTextContent("Đang tạo bản nháp");
    expect(screen.queryByRole("textbox", { name: "Yêu cầu" })).toBeNull();
    await act(async () => { fail(new Error("Máy chủ AI không phản hồi")); });

    expect(await screen.findByRole("textbox", { name: "Yêu cầu" })).toHaveValue("Create an event exam");
    expect(screen.queryByTestId("ai-progress")).toBeNull();
    expect(screen.getByLabelText("Số câu hỏi")).toHaveValue(4);
    expect(screen.getByLabelText("Ngôn ngữ")).toHaveValue("VI");
    expect(within(screen.getByRole("list", { name: "Tài liệu đã chọn" })).getByText("UML basics")).toBeInTheDocument();
    expect(screen.getByTestId("ai-send")).toHaveTextContent("Tạo đề");
    expect(screen.queryByRole("alert")).toBeNull();
    expect(toast).toHaveBeenCalledWith(expect.any(String));
  });

  it("defaults the template to the file uploaded at creation and takes language and count from it", async () => {
    vi.spyOn(assignmentsApi, "materials").mockResolvedValue(MATERIAL);
    const importDocx = vi.spyOn(assignmentsApi, "importDocx").mockResolvedValue({ document: { blocks: [
      { id: "h1", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "Phân tích" } },
      { id: "h2", origin: "TEACHER", type: "HEADING", content: { style: "H2", text: "Use case" } },
      { id: "h3", origin: "TEACHER", type: "HEADING", content: { style: "H1", text: "Thiết kế" } },
    ] }, warnings: [], headingCount: 3, imageCount: 0, diagramCount: 0 } as never);
    const requestAiDraft = vi.spyOn(assignmentsApi, "requestAiDraft").mockResolvedValue({
      draftId: "d1", assignmentId: "a1", origin: "AI_TASK", status: "PENDING", taskId: null, proposal: null,
      provenance: {}, safeError: null, baseVersion: 1, createdAt: "now", decidedAt: null,
    });
    // Tệp lớn hơn 2 MB vẫn dùng được làm mẫu đề.
    const file = new File([new Uint8Array(3 * 1024 * 1024)], "De-mau.docx");
    await rememberTemplate("with-template", file);
    render(<AiDraftDialog assignmentId="with-template" version={1} currentPartCount={0} currentTitle="" currentInstructions=""
      gradingMode="GRADED" onClose={vi.fn()} onApplied={vi.fn()} />);
    expect(await screen.findByTestId("docx-file")).toHaveTextContent("De-mau.docx");
    expect(await screen.findByText("2 câu hỏi")).toBeInTheDocument();
    expect(importDocx).toHaveBeenCalledWith("with-template", file);
    expect(screen.getByText("Cấu hình")).toBeInTheDocument();
    expect(screen.getByText("hoặc")).toBeInTheDocument();
    expect(screen.getByLabelText("Ngôn ngữ")).toBeDisabled();
    expect(screen.getByLabelText("Ngôn ngữ")).toHaveValue("AUTO");
    expect(screen.getByLabelText("Số câu hỏi")).toBeDisabled();
    expect(screen.getByLabelText("Số câu hỏi")).toHaveValue(2);
    await pickReference();
    fireEvent.click(screen.getByTestId("ai-send"));
    await waitFor(() => expect(requestAiDraft).toHaveBeenCalledWith("with-template", expect.objectContaining({
      options: { profile: "AUTO", language: "AUTO", questionCount: 2 },
      formatText: ["1. Phân tích", "1.1. Use case", "2. Thiết kế"].join("\n"),
    })));
  });

  it("locks language and question count as soon as a template is chosen, even while it is still being read", async () => {
    vi.spyOn(assignmentsApi, "materials").mockResolvedValue([]);
    vi.spyOn(assignmentsApi, "importDocx").mockImplementation(() => new Promise(() => {}));
    render(<AiDraftDialog assignmentId="a1" version={1} currentPartCount={0} currentTitle="" currentInstructions=""
      gradingMode="GRADED" onClose={vi.fn()} onApplied={vi.fn()} />);
    fireEvent.change(screen.getByLabelText("Số câu hỏi"), { target: { value: "3" } });
    fireEvent.change(screen.getByTestId("ai-format"), { target: { files: [new File(["x"], "Mau.docx")] } });
    expect(await screen.findByText("Đang đọc…")).toBeInTheDocument();
    expect(screen.getByLabelText("Ngôn ngữ")).toBeDisabled();
    expect(screen.getByLabelText("Số câu hỏi")).toBeDisabled();
    expect(screen.getByTestId("ai-send")).toBeDisabled();
  });

  it("blocks empty drafts and lets instructors revise without automatically calling AI again", async () => {
    vi.spyOn(assignmentsApi, "materials").mockResolvedValue(MATERIAL);
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
    await pickReference();
    fireEvent.change(screen.getByLabelText("Số câu hỏi"), { target: { value: "2" } });
    fireEvent.click(screen.getByTestId("ai-send"));
    expect(await screen.findByTestId("ai-proposal")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Sửa yêu cầu và tạo lại" }));
    await waitFor(() => expect(discard).toHaveBeenCalledWith("a1", "d1"));
    expect(await screen.findByRole("textbox", { name: "Yêu cầu" })).toHaveValue("Create an event exam");
    expect(request).toHaveBeenCalledTimes(1);
  });
});
