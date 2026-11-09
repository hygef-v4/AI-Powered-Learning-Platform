"use client";

import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties } from "react";
import { useRouter } from "next/navigation";
import { uploadFile } from "@/features/files/api";
import { ApiError } from "@/shared/api/client";
import { useActivityTracker, type ActivityEventInput } from "@/shared/activity/useActivityTracker";
import {
  DocumentEditor,
  DocxImportDialog,
  outlineOf,
  publishOutline,
  sectionsOf,
  type DocumentBlock,
  type DocumentModel,
  type DocumentPart,
  type ValidationIssue,
} from "@/shared/document";
import { Alert, Button, Dialog, Menu, MenuItem, Pill } from "@/shared/ui";
import { attemptImageUrl, exportUrl, workApi } from "../api";
import { errorCode, errorMessage, formatDateTime, formatTime, problemIssues, remaining } from "../model/format";

const NOOP = () => {};
import type { Attempt } from "../model/types";

/** Tự lưu mỗi 5 phút khi có thay đổi (yêu cầu 2026-11-05), và khi rời trang, trước khi nộp. */
export const AUTOSAVE_MS = 5 * 60_000;
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

/** Lý do dừng tự lưu: lệch phiên bản, lượt đã đóng (chờ tải lại); bài không hợp lệ (chờ người học sửa). */
type Halt = "conflict" | "closed" | "invalid" | null;

/**
 * #40 Assignment Workspace (F4–F6, UC 39). Giống hệt trang xem trước bài tập của giảng viên (yêu cầu 2026-11-05): thanh trên
 * cùng có tên bài và trạng thái, nút Cả trang / Từng phần ở giữa, thao tác bên phải; trang giấy có tên bài và loại bài;
 * sidebar là mục lục của bài. Khác duy nhất: sinh viên làm bài được.
 *
 * Trang giấy kiểu Google Docs. Đề (hướng dẫn, các phần do giảng viên soạn)
 * hiện ở đây lần đầu; người học chỉ viết trong các phần (ruột trắng của phần lá), không thấy rubric (yêu cầu 2026-10-21). Tự lưu
 * mỗi 5 phút khi có thay đổi và khi rời trang; lỗi mạng giữ trạng thái chưa lưu và thử lại, nên Nộp không nộp bản cũ.
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
  const [dialog, setDialog] = useState<"submit" | "docx" | null>(null);
  const [now, setNow] = useState(() => Date.now());
  const [offset, setOffset] = useState(0);
  const [viewSlot, setViewSlot] = useState<HTMLElement | null>(null);
  const [headerHeight, setHeaderHeight] = useState(64);
  const headerObserver = useRef<ResizeObserver | null>(null);
  const headerRef = useCallback((element: HTMLElement | null) => {
    headerObserver.current?.disconnect();
    headerObserver.current = null;
    if (!element || typeof ResizeObserver === "undefined") return;
    const observer = new ResizeObserver(() => setHeaderHeight(element.offsetHeight));
    observer.observe(element);
    headerObserver.current = observer;
  }, []);
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

  // Đếm từ lần sửa đầu tiên chưa lưu, không dời theo từng lần gõ: gõ liên tục vẫn lưu đều mỗi 5 phút.
  useEffect(() => {
    if (!dirty || halt.current) return;
    const timer = setTimeout(() => void save(), AUTOSAVE_MS);
    return () => clearTimeout(timer);
  }, [dirty, retry, save]);

  useEffect(() => {
    const leave = () => { void save(true); };
    window.addEventListener("pagehide", leave);
    return () => window.removeEventListener("pagehide", leave);
  }, [save]);

  const sendActivity = useCallback((events: ActivityEventInput[], options: { keepalive: boolean }) =>
    workApi.activity(attemptId, events, options.keepalive), [attemptId]);
  useActivityTracker({ enabled: !!attempt?.activityMonitoringEnabled && attempt.status === "IN_PROGRESS", send: sendActivity });

  const sections = useMemo(() => sectionsOf(content, attempt?.parts ?? []), [content, attempt?.parts]);

  // Sidebar mục lục của bài, như trang soạn của giảng viên; lối về trang bài tập.
  useEffect(() => {
    if (!attempt) return;
    const backHref = `/my-assignments/${encodeURIComponent(attempt.assignmentId)}?classId=${encodeURIComponent(attempt.classId)}`;
    publishOutline({ assignmentId: attempt.assignmentId, backHref, backLabel: "Bài tập", entries: outlineOf(content.blocks) });
  }, [attempt, content.blocks]);
  useEffect(() => () => publishOutline(null), []);

  function edit(next: DocumentModel) {
    edits.current += 1;
    if (halt.current === "invalid") halt.current = null;
    setContent(next);
    setDirty(true);
  }

  if (loadError) return <Alert tone="danger" role="alert">{errorMessage(loadError)}</Alert>;
  if (!attempt) return <p role="status" className="text-sm text-muted">Đang mở bài làm…</p>;

  const editable = attempt.editable && attempt.status === "IN_PROGRESS";
  const left = remaining(attempt.deadlineAt, now + offset);
  const beforeDeadline = new Date(attempt.deadlineAt).getTime() > now + offset;
  const active = attempt.parts.find((part) => part.partId === activePart);
  const saveText = saving ? "Đang lưu…" : dirty ? "Có thay đổi chưa lưu" : savedAt ? `Đã lưu lúc ${formatTime(savedAt)}` : "Đã lưu";

  // Dòng dưới tên bài như trang xem trước: chỉ ghi loại bài (Luyện tập, hoặc Cá nhân / Nhóm).
  const header = (
    <div className="mb-5 flex flex-col gap-2 border-b border-line pb-4">
      <p className="text-[13px] text-muted" data-testid="assignment-kind">
        {attempt.gradingMode === "PRACTICE" ? "Luyện tập" : attempt.submissionMode === "GROUP" ? "Nhóm" : "Cá nhân"}
      </p>
    </div>
  );

  return (
    <div data-wide-workspace className="flex flex-col gap-4" style={{ "--doc-toolbar-top": `${56 + headerHeight}px` } as CSSProperties}>
      <header ref={headerRef} className="sticky top-14 z-30 -mx-4 -mt-8 grid grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-x-4 gap-y-2 border-b border-line bg-[var(--c-topbar)] px-4 py-3 backdrop-blur-md sm:-mx-8 sm:px-8 md:-mt-10">
        {/* Quay lại nằm ở sidebar của bài; đầu trang chỉ còn tên bài và trạng thái, như trang xem trước của giảng viên. */}
        <div className="flex min-w-0 flex-1 items-center gap-x-3">
          <h1 className="min-w-0 truncate text-[15px] font-semibold text-ink" title={attempt.title}>{attempt.title}</h1>
          {/* Cạnh tên bài chỉ có thời gian còn lại (yêu cầu 2026-11-05). */}
          {editable && beforeDeadline && (
            <span title={`Hạn nộp: ${formatDateTime(attempt.deadlineAt)}`}><Pill tone="warning">Còn {left}</Pill></span>
          )}
          <span data-testid="save-state" className="sr-only" role="status">{saveText}</span>
        </div>
        <div ref={setViewSlot} data-testid="view-slot" className="flex justify-center" />
        <div className="flex flex-wrap items-center justify-end gap-2">
          <Menu label="" icon="more_horiz" ariaLabel="Thao tác khác" align="right">
            <MenuItem icon="download" onClick={() => { window.location.href = exportUrl(attempt.attemptId); }}>Tải DOCX</MenuItem>
            {editable && <MenuItem icon="upload_file" onClick={() => setDialog("docx")}>Nhập DOCX</MenuItem>}
          </Menu>
          {editable && (
            <Button variant="primary" icon="check_circle" data-testid="open-submit" onClick={() => {
              // Lưu ngay khi mở popup: lưu xong thì popup dùng kết quả kiểm nộp của server cho đúng bản đang có.
              setDialog("submit");
              void save();
            }}>Nộp bài</Button>
          )}
        </div>
      </header>

      {banner && <Alert tone={banner.tone} role="alert" action={banner.reload ? <Button size="sm" onClick={() => window.location.reload()}>Tải lại</Button> : undefined}>
        {banner.text}
        {issues.length > 0 && <ul className="mt-1 list-disc pl-4">{issues.slice(0, 8).map((issue, i) => <li key={i}>{issue.message}</li>)}</ul>}
      </Alert>}
      {attempt.status === "SUBMITTED" && <Alert tone="info">Đã nộp lúc {formatTime(attempt.submittedAt)} · chỉ xem</Alert>}
      {!attempt.editable && attempt.status === "IN_PROGRESS" && <Alert tone="warning">Đã quá hạn</Alert>}
      {attempt.activityMonitoringEnabled && attempt.activityNotice && <Alert tone="info" title="Bài có ghi nhận hoạt động">{attempt.activityNotice}</Alert>}

      <DocumentEditor fullWidth document={content} mode="student" answerGuidance diagramEnabled={attempt.diagramEnabled} readOnly={!editable}
        imageUrl={attemptImageUrl(attempt.attemptId)} issues={issues}
        uploadImage={async (file) => (await uploadFile(file, "DOCUMENT_IMAGE", () => {}).promise).fileRef.value}
        onChange={edit}
        titleField={{ text: attempt.title, readOnly: true, onText: NOOP }}
        header={header}
        highlightBlockId={null}
        onFocusBlock={(blockId) => setActivePart(sections.get(blockId) ?? null)}
        viewSlot={viewSlot} />

      {dialog === "submit" && (
        <SubmitDialog onClose={() => setDialog(null)}
          onSubmit={async () => {
            if (!(await save())) throw new Error("Bài chưa lưu");
            await workApi.submit(attempt.attemptId, version.current);
            const target = attempt.classId
              ? `/my-assignments/${encodeURIComponent(attempt.assignmentId)}?classId=${encodeURIComponent(attempt.classId)}`
              : `/my-assignments/${encodeURIComponent(attempt.assignmentId)}`;
            router.push(target);
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

function SubmitDialog({ onClose, onSubmit }: {
  onClose: () => void;
  onSubmit: () => Promise<void>;
}) {
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  return (
    <Dialog open title="Nộp bài" onClose={onClose} dismissible={!busy}
      footer={<>
        <Button variant="primary" disabled={busy} data-testid="confirm-submit" onClick={async () => {
          setBusy(true);
          setMessage(null);
          try {
            await onSubmit();
          } catch (error) {
            setMessage(errorMessage(error));
            setBusy(false);
          }
        }}>Nộp bài</Button>
      </>}>
      {message && <Alert tone="danger" role="alert">{message}</Alert>}
    </Dialog>
  );
}
