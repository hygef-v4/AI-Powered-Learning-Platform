"use client";

import { useEffect, useRef, useState } from "react";
import { getPolicies } from "@/features/files/api";
import type { PurposePolicy } from "@/features/files/model/purposes";
import { DocumentViewer, type DocumentModel } from "@/shared/document";
import { Alert, Button, Dialog, Icon, IconButton } from "@/shared/ui";
import { assignmentsApi } from "../api";
import { errorMessage, requestRef } from "../model/format";
import { checkImportFiles, IMPORT_ACCEPT, importKindOf } from "../model/importFiles";
import type { ImportDraft, ImportPlacement, ImportProposal, ImportWarning } from "../model/types";

type Props = {
  assignmentId: string;
  /** Khung đề hiện có: quyết định nút "Ghi vào đề" hay "Thay toàn bộ" / "Chèn vào cuối"; DOCX ghép ở máy khách. */
  currentSkeleton: DocumentModel;
  /** Lưu phần đang sửa trước khi ghi đè, trả version mới nhất. */
  prepare: () => Promise<number>;
  /** Tệp chọn sẵn từ popup tạo bài; có thì tự chuyển đổi ngay. */
  initialFiles?: File[];
  onApplied: (message: string) => void | Promise<void>;
  onClose: () => void;
};

/** Bản xem trước chung cho ảnh/PDF (bản nháp AI) và DOCX (đọc ngay, không gọi AI). */
type Preview =
  | { kind: "AI"; draft: ImportDraft; proposal: ImportProposal }
  | { kind: "DOCX"; document: DocumentModel; warnings: ImportWarning[]; counts: ImportProposal["counts"] };

const POLL_MS = 2000;
const APPLIED_MESSAGE = "Đã nhập đề. Nhấn Ctrl+Z để quay lại bản trước.";

/**
 * Nhập đề cũ (ảnh, PDF, DOCX) thành khung đề (BR-U07-87…101): chọn và sắp tệp, chờ AI đọc (có Hủy), xem trước kèm
 * cảnh báo, rồi Ghi vào đề / Thay toàn bộ / Chèn vào cuối. Ghi xong trang soạn bài giữ bản trước để Ctrl+Z.
 */
export function ExamImportDialog({ assignmentId, currentSkeleton, prepare, initialFiles, onApplied, onClose }: Props) {
  const [files, setFiles] = useState<File[]>(initialFiles ?? []);
  const [policy, setPolicy] = useState<PurposePolicy | null>(null);
  const [phase, setPhase] = useState<"pick" | "running" | "preview" | "applying">("pick");
  const [draft, setDraft] = useState<ImportDraft | null>(null);
  const [preview, setPreview] = useState<Preview | null>(null);
  const [useTitle, setUseTitle] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [over, setOver] = useState(false);
  const ref = useRef(requestRef());
  const autoStarted = useRef(false);
  const hasContent = currentSkeleton.blocks.length > 0;

  useEffect(() => {
    let active = true;
    getPolicies().then(
      (policies) => { if (active) setPolicy(policies.find((p) => p.purpose === "ASSIGNMENT_IMPORT") ?? null); },
      () => {},
    );
    return () => { active = false; };
  }, []);

  // Chờ AI đọc: hỏi lại trạng thái bản nháp mỗi 2 giây tới khi xong hoặc lỗi.
  useEffect(() => {
    if (!draft || draft.status !== "PENDING") return;
    const timer = setTimeout(() => {
      assignmentsApi.importDraft(assignmentId, draft.draftId).then(receive, (e) => fail(errorMessage(e)));
    }, POLL_MS);
    return () => clearTimeout(timer);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [assignmentId, draft]);

  useEffect(() => {
    if (autoStarted.current || !initialFiles?.length) return;
    autoStarted.current = true;
    void convert(initialFiles);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function receive(next: ImportDraft) {
    setDraft(next);
    if (next.status === "READY" && next.proposal) {
      setPreview({ kind: "AI", draft: next, proposal: next.proposal });
      setPhase("preview");
    } else if (next.status === "FAILED" || next.status === "DISCARDED") {
      fail(next.safeError ?? "AI không đọc được đề. Hãy thử lại.");
    }
  }

  function fail(message: string) {
    setError(message);
    setDraft(null);
    setPreview(null);
    setPhase("pick");
    ref.current = requestRef();
  }

  function addFiles(list: FileList | File[] | null) {
    if (!list) return;
    // Chép ngay: FileList của input là đối tượng sống, bị làm rỗng khi xóa value trước lúc updater chạy.
    const added = Array.from(list);
    setError(null);
    setFiles((current) => [...current, ...added]);
  }

  function move(index: number, step: -1 | 1) {
    setFiles((current) => {
      const next = [...current];
      const target = index + step;
      if (target < 0 || target >= next.length) return current;
      const [moved] = next.splice(index, 1);
      if (moved) next.splice(target, 0, moved);
      return next;
    });
  }

  async function convert(chosen: File[] = files) {
    const issue = checkImportFiles(chosen, policy);
    if (issue) {
      setError(issue);
      return;
    }
    setError(null);
    setPhase("running");
    try {
      const first = chosen[0];
      if (first && importKindOf(first) === "DOCX") {
        const docx = await assignmentsApi.importDocx(assignmentId, first);
        setPreview({
          kind: "DOCX", document: docx.document,
          warnings: docx.warnings.map((w) => ({ kind: "OTHER" as const, message: w.message })),
          counts: { headings: docx.headingCount, tables: 0, images: docx.imageCount, illegible: 0 },
        });
        setPhase("preview");
        return;
      }
      receive(await assignmentsApi.startImport(assignmentId, chosen, ref.current));
    } catch (e) {
      fail(errorMessage(e));
    }
  }

  async function cancel() {
    const current = draft;
    setDraft(null);
    setPhase("pick");
    ref.current = requestRef();
    if (current) await assignmentsApi.discardAiDraft(assignmentId, current.draftId).catch(() => {});
  }

  async function close() {
    if (draft && (draft.status === "PENDING" || draft.status === "READY")) {
      await assignmentsApi.discardAiDraft(assignmentId, draft.draftId).catch(() => {});
    }
    onClose();
  }

  async function apply(placement: ImportPlacement) {
    if (!preview) return;
    setPhase("applying");
    setError(null);
    try {
      const version = await prepare();
      if (preview.kind === "AI") {
        await assignmentsApi.applyAiDraft(assignmentId, preview.draft.draftId, version, false, useTitle, placement);
      } else {
        const blocks = placement === "APPEND"
          ? [...currentSkeleton.blocks, ...preview.document.blocks] : preview.document.blocks;
        await assignmentsApi.saveSkeleton(assignmentId, version, { ...currentSkeleton, blocks }, { fromDocx: true });
      }
      setDraft(null);
      await onApplied(APPLIED_MESSAGE);
    } catch (e) {
      setError(errorMessage(e));
      setPhase("preview");
    }
  }

  const document = preview?.kind === "AI" ? preview.proposal.document : preview?.document;
  const warnings = preview?.kind === "AI" ? preview.proposal.warnings : preview?.warnings ?? [];
  const counts = preview?.kind === "AI" ? preview.proposal.counts : preview?.counts;
  const examTitle = preview?.kind === "AI" ? preview.proposal.title.trim() : "";
  const busy = phase === "running" || phase === "applying";

  const footer = phase === "preview" || phase === "applying" ? (
    <div className="flex flex-wrap justify-end gap-2">
      <Button data-testid="exam-import-discard" disabled={busy} onClick={() => void close()}>Bỏ</Button>
      {hasContent ? <>
        <Button data-testid="exam-import-append" disabled={busy} onClick={() => void apply("APPEND")}>Chèn vào cuối</Button>
        <Button variant="primary" data-testid="exam-import-replace" disabled={busy} onClick={() => void apply("REPLACE")}>
          Thay toàn bộ
        </Button>
      </> : (
        <Button variant="primary" data-testid="exam-import-apply" disabled={busy} onClick={() => void apply("REPLACE")}>
          Ghi vào đề
        </Button>
      )}
    </div>
  ) : phase === "pick" ? (
    <div className="flex justify-end">
      <Button variant="primary" icon="sparkle" data-testid="exam-import-convert" disabled={files.length === 0}
        onClick={() => void convert()}>Chuyển thành đề</Button>
    </div>
  ) : undefined;

  return (
    <Dialog open title="Nhập đề từ tệp" size={phase === "preview" || phase === "applying" ? "xl" : "lg"}
      dismissible={!busy} onClose={() => void close()} footer={footer}>
      <div className="flex flex-col gap-4" data-testid="exam-import-dialog">
        {error && <Alert tone="danger" role="alert">{error}</Alert>}

        {phase === "pick" && <>
          <p className="text-[13.5px] text-muted">
            Chọn tối đa 5 ảnh (PNG, JPEG, WEBP), hoặc 1 PDF tối đa 10 trang, hoặc 1 tệp DOCX. AI chép đúng nguyên văn đề
            cũ thành khung đề để bạn chỉnh sửa; ảnh và PDF tính vào hạn mức AI.
          </p>
          <label
            onDragOver={(event) => { event.preventDefault(); setOver(true); }}
            onDragLeave={() => setOver(false)}
            onDrop={(event) => { event.preventDefault(); setOver(false); addFiles(event.dataTransfer.files); }}
            className={`flex cursor-pointer items-center gap-3 rounded-[10px] border-[1.5px] border-dashed px-3.5 py-3.5 text-[13.5px] font-semibold transition-colors duration-200 ${
              over ? "border-primary bg-primary-soft text-ink" : "border-line-strong text-body hover:border-primary hover:bg-sunken"
            }`}
          >
            <Icon name="upload_file" size={18} className="text-primary-ink" />
            Kéo thả hoặc bấm để chọn ảnh, PDF, DOCX
            <input type="file" multiple accept={IMPORT_ACCEPT} className="sr-only" aria-label="Chọn tệp đề cũ"
              data-testid="exam-import-input" onChange={(event) => { addFiles(event.target.files); event.target.value = ""; }} />
          </label>
          {files.length > 0 && (
            <ol className="flex flex-col gap-2" aria-label="Tệp đã chọn" data-testid="exam-import-files">
              {files.map((file, index) => (
                <li key={`${file.name}-${index}`} draggable
                  onDragStart={(event) => event.dataTransfer.setData("text/plain", String(index))}
                  onDragOver={(event) => event.preventDefault()}
                  onDrop={(event) => {
                    event.preventDefault();
                    const from = Number(event.dataTransfer.getData("text/plain"));
                    if (Number.isNaN(from) || from === index) return;
                    setFiles((current) => {
                      const next = [...current];
                      const [moved] = next.splice(from, 1);
                      if (moved) next.splice(index, 0, moved);
                      return next;
                    });
                  }}
                  className="flex items-center gap-3 rounded-[10px] border border-line bg-sunken px-3 py-2 text-[13.5px]">
                  <span className="w-12 shrink-0 text-xs font-semibold text-muted">Trang {index + 1}</span>
                  <span className="min-w-0 flex-1 truncate font-semibold text-ink" title={file.name}>{file.name}</span>
                  <IconButton icon="expand_more" label={`Đưa ${file.name} lên trước`} size={28} className="rotate-180"
                    disabled={index === 0} onClick={() => move(index, -1)} />
                  <IconButton icon="expand_more" label={`Đưa ${file.name} xuống sau`} size={28}
                    disabled={index === files.length - 1} onClick={() => move(index, 1)} />
                  <IconButton icon="close" label={`Bỏ ${file.name}`} size={28}
                    onClick={() => setFiles((current) => current.filter((_, i) => i !== index))} />
                </li>
              ))}
            </ol>
          )}
        </>}

        {phase === "running" && (
          <div className="flex flex-col items-center gap-4 py-8" data-testid="exam-import-running">
            <div role="status" aria-live="polite" className="flex items-center gap-3">
              <span aria-hidden="true"
                className="size-5 animate-spin rounded-full border-2 border-line-strong border-t-primary motion-reduce:animate-none" />
              <span className="text-[14px] font-semibold text-ink">Đang đọc đề… có thể mất tới một phút</span>
            </div>
            {draft && <Button data-testid="exam-import-cancel" onClick={() => void cancel()}>Hủy</Button>}
          </div>
        )}

        {(phase === "preview" || phase === "applying") && document && counts && <>
          <p className="text-[13.5px] text-muted" role="status" aria-live="polite" data-testid="exam-import-counts">
            {counts.headings} tiêu đề · {counts.tables} bảng · {counts.images} hình
            {counts.illegible > 0 && <> · <span className="font-semibold text-warning">{counts.illegible} chỗ không đọc rõ</span></>}
          </p>
          {warnings.length > 0 && (
            <Alert tone="warning" title="Cần kiểm tra lại">
              <ul className="list-disc pl-5" data-testid="exam-import-warnings">
                {warnings.map((warning, index) => (
                  <li key={index}>{warning.page ? `Trang ${warning.page}: ` : ""}{warning.message}</li>
                ))}
              </ul>
            </Alert>
          )}
          {examTitle && (
            <label className="flex items-center gap-2 text-[13.5px] text-body">
              <input type="checkbox" checked={useTitle} onChange={(event) => setUseTitle(event.target.checked)}
                data-testid="exam-import-use-title" />
              Dùng “{examTitle}” làm tên bài
            </label>
          )}
          <div className="max-h-[55vh] overflow-y-auto rounded-xl border border-line p-4" data-testid="exam-import-preview">
            <DocumentViewer document={document} previews={preview?.kind === "AI" ? preview.proposal.previews : undefined} />
          </div>
        </>}
      </div>
    </Dialog>
  );
}
