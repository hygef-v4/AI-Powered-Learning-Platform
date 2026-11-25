import { act, renderHook, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, expect, it, vi } from "vitest";
import * as filesApi from "@/features/files/api";
import { assignmentsApi } from "@/features/assignments/api";
import { useAiChatAttachments } from "@/features/assignments/hooks/useAiChatAttachments";

const docx = (name: string, bytes = 5) => new File(["x".repeat(bytes)], name, { type: "application/vnd.openxmlformats-officedocument.wordprocessingml.document" });
const document = { document: { blocks: [{ id: "p", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "Template" }] } }] } };
let counter = 0;
beforeEach(() => {
  counter = 0;
  vi.spyOn(filesApi, "getPolicies").mockResolvedValue([{ purpose: "ASSIGNMENT_SOURCE", maxSizeMb: 2,
    allowedTypes: ["DOCX", "PNG", "PDF"], extensions: ["docx", "png", "pdf"], mediaTypes: [] }]);
  vi.spyOn(filesApi, "uploadFile").mockImplementation(file => ({ abort: vi.fn(), promise: Promise.resolve({ fileRef: { value: `ref${++counter}` }, originalFileName: file.name, expiresAt: "later", byteSize: file.size, mediaType: file.type }) }));
  vi.spyOn(assignmentsApi, "previewAiSources").mockResolvedValue([{ fileName: "ref", previewText: "Reference", extractedChars: 9, truncated: false }]);
  vi.spyOn(assignmentsApi, "importDocx").mockResolvedValue(document as never);
});
afterEach(() => vi.restoreAllMocks());

it("reserves at most three slots synchronously and keeps different files with the same name", async () => {
  const error = vi.fn();
  const { result } = renderHook(() => useAiChatAttachments("a1", error, true));
  await waitFor(() => expect(result.current.sources.canUpload).toBe(true));
  act(() => { result.current.addFiles([docx("same.docx"), docx("same.docx")]); result.current.addFiles([docx("third.docx"), docx("fourth.docx")]); });
  await waitFor(() => expect(result.current.sources.uploads.filter(item => item.status === "READY")).toHaveLength(3));
  expect(filesApi.uploadFile).toHaveBeenCalledTimes(3);
  expect(error).toHaveBeenCalledWith(expect.stringContaining("3"));
});

it("keeps the previous template and reference on failed conversion; oversized DOCX can still become a template", async () => {
  const error = vi.fn();
  const { result } = renderHook(() => useAiChatAttachments("a1", error, true));
  await waitFor(() => expect(result.current.sources.canUpload).toBe(true));
  act(() => result.current.addFiles([docx("large.docx", 2 * 1024 * 1024 + 1)]));
  expect(result.current.sources.uploads[0]?.status).toBe("ERROR");
  expect(filesApi.uploadFile).not.toHaveBeenCalled();
  act(() => result.current.chooseExisting("old"));
  vi.mocked(assignmentsApi.importDocx).mockRejectedValueOnce(new Error("Import failed"));
  act(() => { void result.current.promote(result.current.sources.uploads[0]!); });
  await act(async () => result.current.confirmReplacement());
  expect(result.current.templateId).toBe("old");
  expect(result.current.sources.uploads).toHaveLength(1);
  expect(error).toHaveBeenCalled();
  act(() => { void result.current.promote(result.current.sources.uploads[0]!); });
  await act(async () => result.current.confirmReplacement());
  expect(result.current.template?.file.name).toBe("large.docx");
  expect(result.current.sources.uploads).toHaveLength(0);
});

it("ignores late upload and template responses after removal or unmount", async () => {
  let finishUpload!: (value: never) => void;
  let finishTemplate!: (value: never) => void;
  const abort = vi.fn();
  vi.mocked(filesApi.uploadFile).mockReturnValueOnce({ abort, promise: new Promise(resolve => { finishUpload = resolve; }) });
  const { result, unmount } = renderHook(() => useAiChatAttachments("a1", vi.fn(), true));
  await waitFor(() => expect(result.current.sources.canUpload).toBe(true));
  act(() => result.current.addFiles([docx("late.docx")]));
  act(() => result.current.sources.remove(result.current.sources.uploads[0]!));
  await act(async () => finishUpload({ fileRef: { value: "late" } } as never));
  expect(abort).toHaveBeenCalledOnce();
  expect(result.current.sources.uploads).toHaveLength(0);
  expect(assignmentsApi.previewAiSources).not.toHaveBeenCalled();
  act(() => result.current.addFiles([docx("template.docx")]));
  await waitFor(() => expect(result.current.sources.uploads[0]?.status).toBe("READY"));
  vi.mocked(assignmentsApi.importDocx).mockImplementationOnce(() => new Promise(resolve => { finishTemplate = resolve; }));
  act(() => { void result.current.promote(result.current.sources.uploads[0]!); });
  unmount();
  await act(async () => finishTemplate(document as never));
});
