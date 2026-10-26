"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { uploadFile } from "@/features/files/api";
import { ApiError } from "@/shared/api/client";
import { useActivityTracker, type ActivityEventInput } from "@/shared/activity/useActivityTracker";
import {
  DocumentEditor,
  DocxImportDialog,
  ToolbarButton,
  diagramTypeLabels,
  sectionsOf,
  type DiagramType,
  type DocumentBlock,
  type DocumentModel,
  type DocumentPart,
  type ValidationIssue,
} from "@/shared/document";
import { Alert, Button, Dialog, Icon, PageHeader, Pill, TableFrame, Td, Th } from "@/shared/ui";
import { attemptImageUrl, exportUrl, workApi } from "./api";
import { errorCode, errorMessage, formatDateTime, formatTime, problemIssues, remaining } from "./format";
import type { Attempt } from "./types";

export const AUTOSAVE_MS = 10_000;
/** Trình duyệt giới hạn body của yêu cầu {@code keepalive} khoảng 64 KB; bài lớn hơn thì lưu bằng yêu cầu thường. */
const KEEPALIVE_MAX = 60_000;

/** Ảnh vừa tải lên đã được gắn khi lưu: bỏ {@code fileRef} trong bản đang sửa để lần lưu sau không gắn lại. */
export function mergeSaved(local: DocumentModel, saved: DocumentModel): DocumentModel {
  const persisted = new Set(saved.blocks.filter((b) => b.type === "IMAGE" && b.content.hasFile).map((b) => b.id));
  return {
    blocks: local.blocks.map((block): DocumentBlock => block.type === "IMAGE" && block.content.fileRef && persisted.has(block.id)
      ? { ...block, content: { ...block.content, fileRef: undefined, hasFile: true } } : block),
  };
}

/**
 * Chèn các block (nhập DOCX) vào cuối phần {@code partId}; không có phần (hoặc không tìm thấy) thì thêm vào cuối bài,
 * chỗ luôn thuộc phần cuối của đề (yêu cầu 2026-10-21: người học chỉ viết trong các phần).
 */
export function insertIntoPart(content: DocumentModel, parts: DocumentPart[], partId: string | null, added: DocumentBlock[]): DocumentModel {
  const sections = sectionsOf(content, parts);
  let at = -1;
  if (partId) content.blocks.forEach((block, index) => { if (sections.get(block.id) === partId) at = index; });
  if (at < 0) return { blocks: [...content.blocks, ...added] };
  return { blocks: [...content.blocks.slice(0, at + 1), ...added, ...content.blocks.slice(at + 1)] };
}

/**
 * Kiểm sơ bộ trước khi nộp (server kiểm lại bằng validateForSubmit), không chặn bài server sẽ nhận: đủ tổng số sơ đồ bắt
 * buộc, có nội dung của người học. Sơ đồ của giảng viên có ảnh xem trước tính như đã vẽ, bảng của giảng viên có chữ
 * tính như đã điền; server phân biệt chính xác phần đã làm và phần có sẵn của đề.
 */
export function precheck(attempt: Pick<Attempt, "requiredDiagrams">, content: DocumentModel): ValidationIssue[] {
  const mine = content.blocks.filter((b) => b.origin === "STUDENT");
  const drawn = content.blocks.filter((b) => b.origin === "TEACHER" && b.type === "DIAGRAM" && !!b.content.svgPreview);
  const filled = content.blocks.some((b) => b.origin === "TEACHER" && b.type === "TABLE"
    && b.content.rows.some((row) => row.some((cell) => cell.runs.some((r) => r.text.trim()))));
  const issues: ValidationIssue[] = [];
  // Loại sơ đồ do server suy từ hình vẽ khi lưu (yêu cầu 2026-10-22), bản trên máy có thể chưa có loại mới: chỉ kiểm
  // tổng số sơ đồ (điều kiện cần), loại từng sơ đồ để server kiểm.
  const required = Object.entries(attempt.requiredDiagrams).filter(([, count]) => count > 0) as [DiagramType, number][];
  const total = required.reduce((sum, [, count]) => sum + count, 0);
  const have = [...mine, ...drawn].filter((b) => b.type === "DIAGRAM").length;
  if (have < total) {
    issues.push({ code: "REQUIRED_DIAGRAMS_MISSING", blockId: null,
      message: `Cần ít nhất ${total} sơ đồ (${required.map(([type, count]) => `${count} ${diagramTypeLabels[type]}`).join(", ")}), bài mới có ${have}` });
  }
  const hasContent = drawn.length > 0 || filled || mine.some((b) => b.type === "IMAGE" || b.type === "DIAGRAM"
    || (b.type === "PARAGRAPH" && b.content.runs.some((r) => r.text.trim()))
    || (b.type === "LIST" && b.content.items.some((i) => i.runs.some((r) => r.text.trim())))
    || (b.type === "TABLE" && b.content.rows.some((row) => row.some((cell) => cell.runs.some((r) => r.text.trim())))));
  if (!hasContent) issues.push({ code: "SUBMISSION_EMPTY", blockId: null, message: "Bài làm chưa có nội dung của bạn" });
  return issues;
}

/**
 * Kiểm trong popup Nộp: bản đã lưu có kết quả của server (biết sơ đồ, bảng của đề đã sửa khác đề chưa) thì dùng kết quả
 * đó; chưa có (mất mạng, bản chưa lưu) thì tự kiểm sơ bộ bằng {@link precheck}.
 */
export function submitCheck(attempt: Pick<Attempt, "requiredDiagrams">, content: DocumentModel,
                            serverIssues: ValidationIssue[] | null): ValidationIssue[] {
  return serverIssues ?? precheck(attempt, content);
}

/** Lý do dừng tự lưu: lệch phiên bản, lượt đã đóng (chờ tải lại); bài không hợp lệ (chờ người học sửa). */
type Halt = "conflict" | "closed" | "invalid" | null;

/**
 * #40 Assignment Workspace (F4–F6, UC 39): trang giấy kiểu Google Docs. Đề (hướng dẫn, các phần do giảng viên soạn)
 * hiện ở đây lần đầu; người học chỉ viết trong các phần (ruột trắng của phần lá), không thấy rubric (yêu cầu 2026-10-21). Tự lưu
 * 10 giây sau lần sửa cuối và khi rời trang; lỗi mạng giữ trạng thái chưa lưu và thử lại, nên Nộp không nộp bản cũ.
 */
export function AssignmentWorkspacePage({ attemptId }: { attemptId: string }) {
  const router = useRouter();
  const [attempt, setAttempt] = useState<Attempt | null>(null);
  const [content, setContent] = useState<DocumentModel>({ blocks: [] });
  const [loadError, setLoadError] = useState<unknown>(null);
  const [dirty, setDirty] = useState(false);
  const [saving, setSaving] = useState(false);
  const [savedAt, setSavedAt] = useState<string | null>(null);
  const [banner, setBanner] = useState<{ tone: "danger" | "warning"; text: string; reload?: boolean } | null>(null);
  const [issues, setIssues] = useState<ValidationIssue[]>([]);
  /** Kiểm nộp của bản đã lưu do server trả (biết bản đề gốc); null khi chưa có, lúc đó tự kiểm sơ bộ. */
  const [submitIssues, setSubmitIssues] = useState<ValidationIssue[] | null>(null);
  const [dialog, setDialog] = useState<"submit" | "docx" | null>(null);
  const [now, setNow] = useState(() => Date.now());
  const [offset, setOffset] = useState(0);
  const [focusMode, setFocusMode] = useState(false);
  const [activePart, setActivePart] = useState<string | null>(null);
  const [retry, setRetry] = useState(0);
  const version = useRef(0);
  const edits = useRef(0);
  const halt = useRef<Halt>(null);
  const inflight = useRef<Promise<boolean> | null>(null);
  const latest = useRef({ content, dirty });
  useEffect(() => {
    latest.current = { content, dirty };
  }, [content, dirty]);

  useEffect(() => {
    const controller = new AbortController();
    workApi.attempt(attemptId, controller.signal).then((value) => {
      if (controller.signal.aborted) return;
      setAttempt(value);
      setContent(value.content);
      setSavedAt(value.lastSavedAt);
      setSubmitIssues(value.submitIssues ?? null);
      version.current = value.contentVersion;
      setOffset(new Date(value.serverTime).getTime() - Date.now());
    }, (error) => { if (!controller.signal.aborted) setLoadError(error); });
    return () => controller.abort();
  }, [attemptId]);

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);

  /** Một lần lưu tại một thời điểm; gọi khi đang lưu thì chờ lần đó xong rồi lưu tiếp phần còn đổi. */
  const save = useCallback(async (keepalive = false): Promise<boolean> => {
    while (inflight.current) await inflight.current;
    if (halt.current === "conflict" || halt.current === "closed") return false;
    if (!latest.current.dirty) return true;
    const run = (async () => {
      setSaving(true);
      const startEdits = edits.current;
      const body = latest.current.content;
      try {
        const small = keepalive && JSON.stringify(body).length < KEEPALIVE_MAX;
        const saved = await workApi.save(attemptId, version.current, body, small);
        version.current = saved.contentVersion;
        setSavedAt(saved.lastSavedAt);
        setSubmitIssues(saved.submitIssues ?? null);
        if (edits.current === startEdits) {
          setContent(saved.content);
          setDirty(false);
          latest.current = { content: saved.content, dirty: false };
        } else {
          setContent((current) => mergeSaved(current, saved.content));
        }
        halt.current = null;
        setIssues([]);
        setBanner(null);
        return true;
      } catch (error) {
        // Giữ đánh dấu chưa lưu trong mọi trường hợp: Nộp chỉ đi tiếp khi lần lưu cuối thành công.
        const code = errorCode(error);
        const transient = !(error instanceof ApiError) || error.problem.status >= 500;
        if (code === "CONTENT_VERSION_CONFLICT") {
          halt.current = "conflict";
          setBanner({ tone: "warning", reload: true, text: "Bài đang mở ở nơi khác, bản đang sửa chưa lưu" });
        } else if (code === "DEADLINE_PASSED" || code === "ATTEMPT_SUBMITTED") {
          halt.current = "closed";
          setBanner({ tone: "danger", text: errorMessage(error) });
        } else if (transient) {
          setBanner({ tone: "warning", text: "Mất kết nối, chưa lưu" });
          setRetry((value) => value + 1);
        } else {
          halt.current = "invalid";
          setIssues(problemIssues(error));
          setBanner({ tone: "danger", text: `Chưa lưu: ${errorMessage(error)}` });
        }
        return false;
      } finally {
        setSaving(false);
      }
    })();
    inflight.current = run;
    try {
      return await run;
    } finally {
      if (inflight.current === run) inflight.current = null;
    }
  }, [attemptId]);

  useEffect(() => {
    if (!dirty || halt.current) return;
    const timer = setTimeout(() => void save(), AUTOSAVE_MS);
    return () => clearTimeout(timer);
  }, [dirty, content, retry, save]);

  useEffect(() => {
    const leave = () => { void save(true); };
    window.addEventListener("pagehide", leave);
    return () => window.removeEventListener("pagehide", leave);
  }, [save]);

  const sendActivity = useCallback((events: ActivityEventInput[], options: { keepalive: boolean }) =>
    workApi.activity(attemptId, events, options.keepalive), [attemptId]);
  useActivityTracker({ enabled: !!attempt?.activityMonitoringEnabled && attempt.status === "IN_PROGRESS", send: sendActivity });

  const sections = useMemo(() => sectionsOf(content, attempt?.parts ?? []), [content, attempt?.parts]);

  function edit(next: DocumentModel) {
    edits.current += 1;
    if (halt.current === "invalid") halt.current = null;
    setContent(next);
    setDirty(true);
  }

  if (loadError) return <Alert tone="danger" role="alert">{errorMessage(loadError)}</Alert>;
  if (!attempt) return <p role="status" className="text-sm text-muted">Đang mở bài làm…</p>;

  const editable = attempt.editable && attempt.status === "IN_PROGRESS";
  const detailHref = `/my-assignments/${encodeURIComponent(attempt.assignmentId)}?classId=${encodeURIComponent(attempt.classId)}`;
  const left = remaining(attempt.deadlineAt, now + offset);
  const required = Object.entries(attempt.requiredDiagrams) as [DiagramType, number][];
  const active = attempt.parts.find((part) => part.partId === activePart);

  return (
    <div data-workspace-focus={focusMode} className="flex flex-col gap-3">
      <PageHeader backHref={detailHref} title={<>{attempt.title}<Pill>Lượt {attempt.attemptNo}/{attempt.maxAttempts}</Pill></>}
        subtitle={`Hạn nộp: ${formatDateTime(attempt.deadlineAt)}${editable ? ` · ${new Date(attempt.deadlineAt).getTime() > now + offset ? `Còn ${left} đến hạn nộp` : "Đã đến hạn nộp"}` : ""}`}
        actions={<>
          <Button aria-pressed={focusMode} onClick={() => setFocusMode((value) => !value)}>{focusMode ? "Thoát tập trung" : "Chế độ tập trung"}</Button>
          {attempt.status === "SUBMITTED" && <Button variant="primary" onClick={() => router.push(`/attempts/${encodeURIComponent(attempt.attemptId)}/receipt`)}>Xem biên nhận</Button>}
        </>} />
      {banner && <Alert tone={banner.tone} role="alert" action={banner.reload ? <Button size="sm" onClick={() => window.location.reload()}>Tải lại</Button> : undefined}>
        {banner.text}
        {issues.length > 0 && <ul className="mt-1 list-disc pl-4">{issues.slice(0, 8).map((issue, i) => <li key={i}>{issue.message}</li>)}</ul>}
      </Alert>}
      {attempt.status === "SUBMITTED" && <Alert tone="info">Đã nộp lúc {formatTime(attempt.submittedAt)} · chỉ xem</Alert>}
      {!attempt.editable && attempt.status === "IN_PROGRESS" && <Alert tone="warning">Đã quá hạn</Alert>}
      {attempt.activityMonitoringEnabled && attempt.activityNotice && <Alert tone="info" title="Bài có ghi nhận hoạt động">{attempt.activityNotice}</Alert>}
      <DocumentEditor document={content} mode="student" answerGuidance diagramEnabled={attempt.diagramEnabled} readOnly={!editable}
        imageUrl={attemptImageUrl(attempt.attemptId)} issues={issues}
        uploadImage={async (file) => (await uploadFile(file, "DOCUMENT_IMAGE", () => {}).promise).fileRef.value}
        onChange={edit}
        highlightBlockId={active?.headingBlockId ?? null}
        onFocusBlock={(blockId) => setActivePart(sections.get(blockId) ?? null)}
        header={
          <div className="mb-5 flex flex-col gap-2 border-b border-line pb-4">
            <h2 className="text-[26px] leading-tight font-bold text-ink">{attempt.title}</h2>
            {attempt.instructions && <p className="text-sm leading-[1.65] whitespace-pre-wrap text-body">{attempt.instructions}</p>}
            <p className="flex flex-wrap items-center gap-1.5 text-xs text-muted">
              <Pill>{attempt.submissionMode === "GROUP" ? "Nhóm" : "Cá nhân"}</Pill>
              <Pill tone={attempt.gradingMode === "GRADED" ? "info" : "accent"}>{attempt.gradingMode === "GRADED" ? "Graded" : "Practice"}</Pill>
              {required.length > 0 && <span>· Sơ đồ cần có: {required.map(([type, count]) => `${count} ${diagramTypeLabels[type]}`).join(", ")}</span>}
            </p>
          </div>
        }
        toolbarExtra={
          <details className="relative ml-auto">
            <summary className="cursor-pointer rounded-md px-2 py-1 text-sm font-medium text-body hover:bg-sunken">Tệp DOCX</summary>
            <div className="absolute right-0 z-30 mt-2 flex min-w-40 flex-col gap-1 rounded-lg border border-line bg-surface p-2 shadow-[var(--shadow-float)]">
              {editable && <ToolbarButton icon="upload_file" onClick={(event) => { event.currentTarget.closest("details")?.removeAttribute("open"); setDialog("docx"); }}>Nhập DOCX</ToolbarButton>}
              <a href={exportUrl(attempt.attemptId)} className="inline-flex items-center gap-1 rounded-md px-2 py-1 text-sm text-body hover:bg-sunken">
                <Icon name="download" size={18} />Tải DOCX
              </a>
            </div>
          </details>
        } />
      {editable && <div role="region" aria-label="Lưu và nộp bài" className="sticky bottom-0 z-20 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-line bg-surface px-4 py-3 shadow-[var(--shadow-float)]">
        <p role="status" className="text-sm text-muted">{saving ? "Đang lưu…" : dirty ? "Có thay đổi chưa lưu" : savedAt ? `Đã lưu lúc ${formatTime(savedAt)}` : "Tự lưu 10 giây sau lần sửa cuối"}</p>
        <div className="flex items-center gap-2">
          <Button disabled={saving || !dirty} onClick={() => void save()}>Lưu bài</Button>
          <Button variant="primary" icon="check_circle" data-testid="open-submit" onClick={() => {
            // Lưu ngay khi mở popup: lưu xong thì popup dùng kết quả kiểm nộp của server cho đúng bản đang có.
            setDialog("submit");
            void save();
          }}>Nộp bài</Button>
        </div>
      </div>}

      {dialog === "submit" && (
        <SubmitDialog attempt={attempt} content={content} serverIssues={dirty ? null : submitIssues} onClose={() => setDialog(null)}
          onSubmit={async () => {
            if (!(await save())) throw new Error("Bài chưa lưu");
            const receipt = await workApi.submit(attempt.attemptId, version.current);
            router.push(`/attempts/${encodeURIComponent(receipt.attemptId)}/receipt`);
          }} />
      )}
      {dialog === "docx" && (
        <DocxImportDialog allowReplace={false}
          title={active ? `Nhập DOCX vào phần “${active.title.split(" › ").at(-1)}”` : "Nhập DOCX vào phần cuối của bài"}
          preview={(file) => workApi.importDocx(attempt.attemptId, file)}
          onClose={() => setDialog(null)}
          onImport={(document) => edit(insertIntoPart(latest.current.content, attempt.parts, activePart, document.blocks))} />
      )}
    </div>
  );
}

function SubmitDialog({ attempt, content, serverIssues, onClose, onSubmit }: {
  attempt: Attempt;
  content: DocumentModel;
  serverIssues: ValidationIssue[] | null;
  onClose: () => void;
  onSubmit: () => Promise<void>;
}) {
  const [busy, setBusy] = useState(false);
  const [errors, setErrors] = useState<ValidationIssue[]>(() => submitCheck(attempt, content, serverIssues));
  const [message, setMessage] = useState<string | null>(null);
  const left = attempt.maxAttempts - attempt.attemptNo;
  return (
    <Dialog open size="lg" icon="assignment" title={`Nộp bài lượt ${attempt.attemptNo}?`} onClose={onClose} dismissible={!busy}
      footer={errors.length > 0 ? <Button onClick={onClose}>Quay lại sửa</Button> : <>
        <Button variant="primary" disabled={busy} data-testid="confirm-submit" onClick={async () => {
          setBusy(true);
          setMessage(null);
          try {
            await onSubmit();
          } catch (error) {
            const issues = problemIssues(error);
            if (issues.length > 0) setErrors(issues);
            setMessage(errorMessage(error));
            setBusy(false);
          }
        }}>Nộp bài</Button>
      </>}>
      {message && <Alert tone="danger" role="alert">{message}</Alert>}
      <TableFrame>
        <thead><tr><Th>Mục</Th><Th>Kiểm tra</Th></tr></thead>
        <tbody>
          {errors.length === 0 ? <tr><Td>Toàn bài</Td><Td className="text-success">Hợp lệ</Td></tr>
            : errors.map((issue, i) => <tr key={i}><Td>{attempt.parts.find((p) => p.headingBlockId === issue.blockId)?.title ?? "Toàn bài"}</Td><Td className="text-danger">{issue.message}</Td></tr>)}
        </tbody>
      </TableFrame>
      <p>Sau khi nộp, lượt này không sửa được. Bạn còn {Math.max(0, left)} lượt sau lượt này.</p>
    </Dialog>
  );
}
