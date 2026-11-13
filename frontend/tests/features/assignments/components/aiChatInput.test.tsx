import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { AiReviseChat } from "@/features/assignments/components/AiReviseChat";
import { assignmentsApi } from "@/features/assignments/api";
import * as files from "@/features/files/api";

beforeEach(() => {
  vi.spyOn(files, "getPolicies").mockResolvedValue([{ purpose: "ASSIGNMENT_SOURCE", maxSizeMb: 2,
    allowedTypes: ["PNG", "DOCX", "PDF"], extensions: ["png", "docx", "pdf"], mediaTypes: [] }]);
  vi.spyOn(files, "uploadFile").mockImplementation(file => ({ abort: vi.fn(), promise: Promise.resolve({
    fileRef: { value: `ref:${file.name}` }, originalFileName: file.name, mediaType: file.type, byteSize: file.size, expiresAt: "later" }) }));
  vi.spyOn(assignmentsApi, "previewAiSources").mockResolvedValue([{ fileName: "source", previewText: "source text", extractedChars: 11, truncated: false }]);
  vi.spyOn(assignmentsApi, "materials").mockResolvedValue([]);
  vi.spyOn(assignmentsApi, "listAiTemplates").mockResolvedValue([]);
});
afterEach(() => vi.restoreAllMocks());

async function open(prepare = vi.fn().mockResolvedValue(3)) {
  render(<AiReviseChat assignmentId="a1" prepare={prepare} onApplied={vi.fn()} />);
  fireEvent.click(screen.getByTestId("ai-revise-toggle"));
  fireEvent.click(screen.getByTestId("ai-revise-attach"));
  await waitFor(() => expect(screen.getByRole("menuitem", { name: "Tải từ máy" })).toBeEnabled());
  fireEvent.click(screen.getByTestId("ai-revise-attach"));
  return screen.getByTestId("ai-revise-input") as HTMLTextAreaElement;
}

it("pastes images and plain text into the selection through the same reference upload", async () => {
  const input = await open();
  const image = new File(["png"], "clipboard.png", { type: "image/png" });
  fireEvent.change(input, { target: { value: "Before after" } });
  input.setSelectionRange(7, 7);
  fireEvent.paste(input, { clipboardData: { items: [{ kind: "file", getAsFile: () => image }], getData: () => "insert " } });
  expect(input).toHaveValue("Before insert after");
  expect(await screen.findByText("Sẵn sàng")).toBeInTheDocument();
  expect(files.uploadFile).toHaveBeenCalledOnce();
  expect(files.uploadFile).toHaveBeenCalledWith(image, "ASSIGNMENT_SOURCE", expect.any(Function));
  expect(screen.getByTestId("ai-revise-attachments")).toHaveTextContent("Tham khảo");
});

it("keeps URL paste as text, with no network reading or paste interception", async () => {
  const input = await open();
  expect(fireEvent.paste(input, { clipboardData: { items: [], getData: () => "https://example.org" } })).toBe(true);
  expect(files.uploadFile).not.toHaveBeenCalled();
  expect(assignmentsApi.previewAiSources).not.toHaveBeenCalled();
});

it("keeps the drop overlay across children and accepts files, not text drags or directories", async () => {
  const input = await open();
  const panel = screen.getByTestId("ai-revise-chat");
  const pdf = new File(["pdf"], "reference.pdf", { type: "application/pdf" });
  const transfer = { types: ["Files"], files: [pdf], items: [], dropEffect: "" };
  fireEvent.dragEnter(panel, { dataTransfer: transfer });
  fireEvent.dragEnter(input, { dataTransfer: transfer });
  fireEvent.dragLeave(input, { dataTransfer: transfer });
  expect(screen.getByText("Thả tệp để đính kèm")).toBeVisible();
  fireEvent.drop(panel, { dataTransfer: transfer });
  expect(screen.queryByText("Thả tệp để đính kèm")).not.toBeInTheDocument();
  expect(await screen.findByText("reference.pdf")).toBeInTheDocument();
  expect(fireEvent.dragOver(input, { dataTransfer: { types: ["text/plain"] } })).toBe(true);
  fireEvent.drop(panel, { dataTransfer: { types: ["Files"], files: [], items: [{ webkitGetAsEntry: () => ({ isDirectory: true }) }] } });
  expect(files.uploadFile).toHaveBeenCalledOnce();
});

it("keeps text and files on prepare/request failure and reuses the request ref on an uncertain retry", async () => {
  const request = vi.spyOn(assignmentsApi, "requestAiRevision").mockRejectedValue(new Error("Network lost"));
  const input = await open();
  fireEvent.change(screen.getByTestId("ai-revise-reference-file"), { target: { files: [new File(["pdf"], "ref.pdf", { type: "application/pdf" })] } });
  await screen.findByText("Sẵn sàng");
  fireEvent.change(input, { target: { value: "Sửa đề theo tài liệu" } });
  fireEvent.keyDown(input, { key: "Enter" });
  await waitFor(() => expect(request).toHaveBeenCalledTimes(1));
  await waitFor(() => expect(input).not.toBeDisabled());
  expect(input).toHaveValue("Sửa đề theo tài liệu");
  expect(screen.getByTestId("ai-revise-attachments")).toHaveTextContent("ref.pdf");
  expect(screen.getByRole("log")).not.toHaveTextContent("Sửa đề theo tài liệu");
  fireEvent.keyDown(input, { key: "Enter" });
  await waitFor(() => expect(request).toHaveBeenCalledTimes(2));
  expect(request.mock.calls[1]![2]).toBe(request.mock.calls[0]![2]);
});

it("does not duplicate rapid submits or send while composing Vietnamese", async () => {
  let reject!: (error: Error) => void;
  const request = vi.spyOn(assignmentsApi, "requestAiRevision").mockImplementation(() => new Promise((_, fail) => { reject = fail; }));
  const input = await open();
  fireEvent.change(input, { target: { value: "Thêm câu hỏi" } });
  fireEvent.keyDown(input, { key: "Enter", isComposing: true });
  expect(request).not.toHaveBeenCalled();
  fireEvent.keyDown(input, { key: "Enter" });
  fireEvent.keyDown(input, { key: "Enter" });
  await waitFor(() => expect(request).toHaveBeenCalledOnce());
  await act(async () => reject(new Error("Stop test")));
});

it("does not lose the composer when saving before the AI request fails", async () => {
  const request = vi.spyOn(assignmentsApi, "requestAiRevision");
  const input = await open(vi.fn().mockRejectedValue(new Error("Save conflict")));
  fireEvent.change(input, { target: { value: "Giữ nội dung" } });
  fireEvent.keyDown(input, { key: "Enter" });
  await waitFor(() => expect(input).not.toBeDisabled());
  expect(input).toHaveValue("Giữ nội dung");
  expect(request).not.toHaveBeenCalled();
});

it("restores accepted text and reference snapshots after AI failure without calling AI again", async () => {
  const request = vi.spyOn(assignmentsApi, "requestAiRevision").mockResolvedValue({ status: "FAILED", safeError: "Quota exhausted" } as never);
  const input = await open();
  fireEvent.change(screen.getByTestId("ai-revise-reference-file"), { target: { files: [new File(["pdf"], "restore.pdf", { type: "application/pdf" })] } });
  await screen.findByText("Sẵn sàng");
  fireEvent.change(input, { target: { value: "Dựa theo tài liệu này" } });
  fireEvent.keyDown(input, { key: "Enter" });
  const restore = await screen.findByRole("button", { name: "Khôi phục yêu cầu" });
  await waitFor(() => expect(restore).toBeEnabled());
  expect(input).toHaveValue("");
  expect(screen.queryByTestId("ai-revise-attachments")).not.toBeInTheDocument();
  fireEvent.click(restore);
  expect(input).toHaveValue("Dựa theo tài liệu này");
  expect(screen.getByTestId("ai-revise-attachments")).toHaveTextContent("restore.pdf");
  expect(request).toHaveBeenCalledOnce();
});

it("does not report an applied revision as unchanged when refreshing the page fails", async () => {
  vi.spyOn(assignmentsApi, "requestAiRevision").mockResolvedValue({ status: "READY", draftId: "d1", proposal: { validation: { status: "VALID", issues: [] } } } as never);
  vi.spyOn(assignmentsApi, "applyAiDraft").mockResolvedValue({} as never);
  render(<AiReviseChat assignmentId="a1" prepare={vi.fn().mockResolvedValue(3)} onApplied={vi.fn().mockRejectedValue(new Error("Refresh failed"))} />);
  const input = screen.getByTestId("ai-revise-input");
  fireEvent.change(input, { target: { value: "Sửa câu hỏi" } });
  fireEvent.keyDown(input, { key: "Enter" });
  expect(await screen.findByText(/Đề đã được cập nhật nhưng chưa tải lại được/)).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Khôi phục yêu cầu" })).not.toBeInTheDocument();
});

it("does not repeat or discard an apply whose response was lost", async () => {
  vi.spyOn(assignmentsApi, "requestAiRevision").mockResolvedValue({ status: "READY", draftId: "d1", proposal: { validation: { status: "VALID", issues: [] } } } as never);
  vi.spyOn(assignmentsApi, "applyAiDraft").mockRejectedValue(new Error("Response lost"));
  const discard = vi.spyOn(assignmentsApi, "discardAiDraft");
  render(<AiReviseChat assignmentId="a1" prepare={vi.fn().mockResolvedValue(3)} onApplied={vi.fn()} />);
  const input = screen.getByTestId("ai-revise-input");
  fireEvent.change(input, { target: { value: "Sửa câu hỏi" } });
  fireEvent.keyDown(input, { key: "Enter" });
  expect(await screen.findByText(/Chưa xác nhận được kết quả áp dụng/)).toBeInTheDocument();
  expect(screen.queryByRole("button", { name: "Khôi phục yêu cầu" })).not.toBeInTheDocument();
  expect(discard).not.toHaveBeenCalled();
});
