import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { assignmentsApi } from "@/features/assignments/api";
import { ExamImportDialog } from "@/features/assignments/components/ExamImportDialog";
import { checkImportFiles } from "@/features/assignments/model/importFiles";
import { clearPendingImport, peekPendingImport, savePendingImport } from "@/features/assignments/model/pendingImports";
import type { AssignmentDetail, ImportDraft } from "@/features/assignments/model/types";
import type { DocumentModel } from "@/shared/document";
import * as filesApi from "@/features/files/api";
import type { PurposePolicy } from "@/features/files/model/purposes";

const POLICY: PurposePolicy = {
  purpose: "ASSIGNMENT_IMPORT", maxSizeMb: 10, allowedTypes: ["PNG", "JPEG", "WEBP", "PDF"],
  extensions: ["png", "jpg", "jpeg", "webp", "pdf"], mediaTypes: [],
};
const EMPTY: DocumentModel = { blocks: [] };
const EXISTING: DocumentModel = {
  blocks: [{ id: "old", origin: "TEACHER", type: "PARAGRAPH", content: { runs: [{ text: "Đề cũ" }] } }],
};

function draft(status: ImportDraft["status"], extra: Partial<ImportDraft> = {}): ImportDraft {
  return {
    draftId: "d1", assignmentId: "a1", origin: "AI_TASK", status, taskId: "t1", proposal: null,
    provenance: { mode: "IMPORT" }, safeError: null, baseVersion: 1, createdAt: "now", decidedAt: null, ...extra,
  };
}

const READY = draft("READY", {
  proposal: {
    kind: "IMPORT", title: "SWR302_SU25_PE1_407173",
    document: { blocks: [{ id: "b1", origin: "TEACHER", type: "PARAGRAPH",
      content: { runs: [{ text: "Question 1 (0.5 point): Complete all parts." }] } }] },
    warnings: [{ kind: "HEADER_FOOTER_REMOVED", page: 1, message: "Bỏ chân trang" }],
    counts: { headings: 1, tables: 0, images: 0, illegible: 2 },
  },
});

function png(name: string) {
  return new File(["x"], name, { type: "image/png" });
}

/** Như Chrome: FileList của input là đối tượng sống, gán {@code value = ""} làm nó rỗng. */
function choose(files: File[]) {
  const input = screen.getByTestId("exam-import-input");
  const live = [...files];
  Object.defineProperty(input, "value", { configurable: true, get: () => "", set: () => { live.length = 0; } });
  fireEvent.change(input, { target: { files: live } });
}

describe("ExamImportDialog", () => {
  beforeEach(() => {
    vi.spyOn(filesApi, "getPolicies").mockResolvedValue([POLICY]);
  });
  afterEach(() => vi.restoreAllMocks());

  it("sắp lại thứ tự ảnh rồi gửi đúng thứ tự, chờ AI và hiện xem trước kèm cảnh báo", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    const start = vi.spyOn(assignmentsApi, "startImport").mockResolvedValue(draft("PENDING"));
    const poll = vi.spyOn(assignmentsApi, "importDraft").mockResolvedValue(READY);
    render(<ExamImportDialog assignmentId="a1" currentSkeleton={EMPTY} prepare={vi.fn()} onApplied={vi.fn()}
      onClose={vi.fn()} />);

    choose([png("trang-1.png"), png("trang-2.png")]);
    const list = screen.getByTestId("exam-import-files");
    expect(within(list).getAllByRole("listitem")).toHaveLength(2);
    fireEvent.click(screen.getByRole("button", { name: "Đưa trang-2.png lên trước" }));
    await waitFor(() => expect(within(list).getAllByRole("listitem")[0]).toHaveTextContent("trang-2.png"));
    fireEvent.click(screen.getByTestId("exam-import-convert"));

    expect(await screen.findByTestId("exam-import-running")).toHaveTextContent("Đang đọc đề");
    expect(start.mock.calls[0]![1].map((file) => file.name)).toEqual(["trang-2.png", "trang-1.png"]);
    await vi.advanceTimersByTimeAsync(2100);
    await waitFor(() => expect(poll).toHaveBeenCalledWith("a1", "d1"));
    expect(await screen.findByTestId("exam-import-preview")).toHaveTextContent("Question 1 (0.5 point)");
    expect(screen.getByTestId("exam-import-counts")).toHaveTextContent("2 chỗ không đọc rõ");
    expect(screen.getByTestId("exam-import-warnings")).toHaveTextContent("Trang 1: Bỏ chân trang");
    expect(screen.getByTestId("exam-import-apply")).toHaveTextContent("Ghi vào đề");
    vi.useRealTimers();
  });

  it("khung đã có nội dung thì chọn Chèn vào cuối, dùng tên bài từ đề và báo cho trang để Ctrl+Z", async () => {
    vi.spyOn(assignmentsApi, "startImport").mockResolvedValue(READY);
    const apply = vi.spyOn(assignmentsApi, "applyAiDraft").mockResolvedValue({} as AssignmentDetail);
    const prepare = vi.fn().mockResolvedValue(7);
    const onApplied = vi.fn();
    render(<ExamImportDialog assignmentId="a1" currentSkeleton={EXISTING} prepare={prepare} onApplied={onApplied}
      onClose={vi.fn()} />);

    choose([png("de.png")]);
    fireEvent.click(screen.getByTestId("exam-import-convert"));
    expect(await screen.findByTestId("exam-import-replace")).toHaveTextContent("Thay toàn bộ");
    fireEvent.click(screen.getByTestId("exam-import-use-title"));
    fireEvent.click(screen.getByTestId("exam-import-append"));

    await waitFor(() => expect(apply).toHaveBeenCalledWith("a1", "d1", 7, false, true, "APPEND"));
    expect(onApplied).toHaveBeenCalledWith(expect.stringContaining("Ctrl+Z"));
  });

  it("xem trước hiện hình đã cắt qua link máy chủ cấp", async () => {
    vi.spyOn(assignmentsApi, "startImport").mockResolvedValue(draft("READY", {
      proposal: {
        kind: "IMPORT", title: "",
        document: { blocks: [{ id: "fig", origin: "TEACHER", type: "IMAGE",
          content: { fileRef: "ref-1", alt: "Đồ thị", width: 600 } }] },
        warnings: [], counts: { headings: 0, tables: 0, images: 1, illegible: 0 },
        previews: { fig: "/api/v1/files/download/tok" },
      },
    }));
    render(<ExamImportDialog assignmentId="a1" currentSkeleton={EMPTY} prepare={vi.fn()} onApplied={vi.fn()}
      onClose={vi.fn()} />);

    choose([png("de.png")]);
    fireEvent.click(screen.getByTestId("exam-import-convert"));

    const preview = await screen.findByTestId("exam-import-preview");
    expect(within(preview).getByRole("img", { name: "Đồ thị" })).toHaveAttribute("src", "/api/v1/files/download/tok");
  });

  it("Hủy trong lúc chờ thì bỏ bản nháp và quay lại danh sách tệp", async () => {
    vi.spyOn(assignmentsApi, "startImport").mockResolvedValue(draft("PENDING"));
    vi.spyOn(assignmentsApi, "importDraft").mockResolvedValue(draft("PENDING"));
    const discard = vi.spyOn(assignmentsApi, "discardAiDraft").mockResolvedValue(draft("DISCARDED") as never);
    render(<ExamImportDialog assignmentId="a1" currentSkeleton={EMPTY} prepare={vi.fn()} onApplied={vi.fn()}
      onClose={vi.fn()} />);

    choose([png("de.png")]);
    fireEvent.click(screen.getByTestId("exam-import-convert"));
    fireEvent.click(await screen.findByTestId("exam-import-cancel"));

    await waitFor(() => expect(discard).toHaveBeenCalledWith("a1", "d1"));
    expect(screen.getByTestId("exam-import-files")).toHaveTextContent("de.png");
  });

  it("AI lỗi thì báo lỗi, giữ tệp để thử lại", async () => {
    vi.spyOn(assignmentsApi, "startImport").mockResolvedValue(draft("FAILED", {
      safeError: "Chưa có model AI đọc được ảnh; hãy cấu hình hoặc chọn model khác" }));
    render(<ExamImportDialog assignmentId="a1" currentSkeleton={EMPTY} prepare={vi.fn()} onApplied={vi.fn()}
      onClose={vi.fn()} />);

    choose([png("de.png")]);
    fireEvent.click(screen.getByTestId("exam-import-convert"));

    expect(await screen.findByRole("alert")).toHaveTextContent("model AI đọc được ảnh");
    expect(screen.getByTestId("exam-import-convert")).toBeEnabled();
  });

  it("chặn ngay ở máy khách các lựa chọn tệp sai, không gọi máy chủ", async () => {
    const start = vi.spyOn(assignmentsApi, "startImport");
    render(<ExamImportDialog assignmentId="a1" currentSkeleton={EMPTY} prepare={vi.fn()} onApplied={vi.fn()}
      onClose={vi.fn()} />);
    await waitFor(() => expect(filesApi.getPolicies).toHaveBeenCalled());

    choose([png("a.png"), new File(["%PDF"], "b.pdf")]);
    fireEvent.click(screen.getByTestId("exam-import-convert"));

    expect(await screen.findByRole("alert")).toHaveTextContent("Không chọn chung ảnh");
    expect(start).not.toHaveBeenCalled();
  });

  it("DOCX đi đường nhập DOCX cũ, không gọi AI, và ghép vào cuối khung ở máy khách", async () => {
    const document: DocumentModel = { blocks: [{ id: "n", origin: "TEACHER", type: "PARAGRAPH",
      content: { runs: [{ text: "Đề DOCX" }] } }] };
    vi.spyOn(assignmentsApi, "importDocx").mockResolvedValue({ document, warnings: [], headingCount: 0,
      imageCount: 0, diagramCount: 0 });
    const start = vi.spyOn(assignmentsApi, "startImport");
    const save = vi.spyOn(assignmentsApi, "saveSkeleton").mockResolvedValue({} as AssignmentDetail);
    render(<ExamImportDialog assignmentId="a1" currentSkeleton={EXISTING} prepare={vi.fn().mockResolvedValue(3)}
      onApplied={vi.fn()} onClose={vi.fn()} />);

    choose([new File(["docx"], "de.docx")]);
    fireEvent.click(screen.getByTestId("exam-import-convert"));
    fireEvent.click(await screen.findByTestId("exam-import-append"));

    await waitFor(() => expect(save).toHaveBeenCalledWith("a1", 3,
      { blocks: [...EXISTING.blocks, ...document.blocks] }, { fromDocx: true }));
    expect(start).not.toHaveBeenCalled();
  });

  it("tự chuyển đổi tệp chọn sẵn từ popup tạo bài", async () => {
    const start = vi.spyOn(assignmentsApi, "startImport").mockResolvedValue(READY);
    const files = [new File(["%PDF"], "de.pdf")];
    render(<ExamImportDialog assignmentId="a1" currentSkeleton={EMPTY} prepare={vi.fn()} initialFiles={files}
      onApplied={vi.fn()} onClose={vi.fn()} />);

    await waitFor(() => expect(start).toHaveBeenCalledWith("a1", files, expect.any(String)));
    expect(await screen.findByTestId("exam-import-preview")).toBeInTheDocument();
  });
});

describe("checkImportFiles và tệp chờ nhập", () => {
  it("giới hạn 5 ảnh, 1 PDF, 1 DOCX, không trộn và theo dung lượng của chính sách", () => {
    const six = Array.from({ length: 6 }, (_, i) => png(`${i}.png`));
    expect(checkImportFiles([], POLICY)).toMatch(/Chọn ảnh/);
    expect(checkImportFiles(six, POLICY)).toMatch(/tối đa 5 ảnh/);
    expect(checkImportFiles([new File(["a"], "a.pdf"), new File(["b"], "b.pdf")], POLICY)).toMatch(/1 tệp PDF/);
    expect(checkImportFiles([new File(["a"], "a.gif")], POLICY)).toMatch(/Chỉ nhận/);
    const big = new File(["x"], "big.png");
    Object.defineProperty(big, "size", { value: 11 * 1024 * 1024 });
    expect(checkImportFiles([big], POLICY)).toMatch(/big.png: Tệp vượt 10 MB/);
    expect(checkImportFiles([png("a.png"), png("b.webp")], POLICY)).toBeNull();
  });

  it("giữ tệp chờ của từng bài tới khi popup mở", () => {
    const files = [png("a.png")];
    savePendingImport("a1", files);
    expect(peekPendingImport("a1")).toBe(files);
    expect(peekPendingImport("a2")).toEqual([]);
    clearPendingImport("a1");
    expect(peekPendingImport("a1")).toEqual([]);
  });
});
