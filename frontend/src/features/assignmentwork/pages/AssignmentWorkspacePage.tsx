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

import type { Attempt } from "../model/types";
import { NOOP } from "@/shared/noop";
import { WorkspaceTopBar } from "@/shared/layout/WorkspaceTopBar";
import { msg } from "@/i18n/msg";

/** Tự lưu mỗi 5 phút khi có thay đổi (yêu cầu 2026-11-05), và khi rời trang, trước khi nộp. */
export const AUTOSAVE_MS = 5 * 60_000;
/** Trình duyệt giới hạn body của yêu cầu {@code keepalive} khoảng 64 KB; bài lớn hơn thì lưu bằng yêu cầu thường. */
const KEEPALIVE_MAX = 60_000;
/** Departure saves still running after unmount; reopening the same attempt waits so it loads their version. */
const departures = new Map<string, Promise<boolean>>();

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
  const draftKey = `assignment-work-draft:${attemptId}`;
  const storedDraft = useRef<string | null>(null);
  const persistDraft = useCallback((onlyIfUnchanged = false) => {
    try {
      // A late save from a departed page must not overwrite a newer departure draft.
      if (onlyIfUnchanged && sessionStorage.getItem(draftKey) !== storedDraft.current) return;
      if (latest.current.dirty) {
        const draft = JSON.stringify({
          contentVersion: version.current, content: latest.current.content,
        });
        sessionStorage.setItem(draftKey, draft);
        storedDraft.current = draft;
      } else {
        sessionStorage.removeItem(draftKey);
        storedDraft.current = null;
      }
    } catch { /* Storage may be disabled or full; server saving must still work. */ }
  }, [draftKey]);
  useEffect(() => {
    latest.current = { content, dirty };
  }, [content, dirty]);

  useEffect(() => {
    const controller = new AbortController();
    void (departures.get(attemptId) ?? Promise.resolve(true)).then(() => {
      return workApi.attempt(attemptId, controller.signal);
    }).then((value) => {
      if (controller.signal.aborted) return;
      let recovered: DocumentModel | null = null;
      try {
        storedDraft.current = sessionStorage.getItem(draftKey);
        const draft = JSON.parse(storedDraft.current ?? "null") as {
          contentVersion: number; content: DocumentModel;
        } | null;
        // The unsaved draft extends this exact server version; a newer server save takes precedence.
        if (value.editable && value.status === "IN_PROGRESS"
          && draft?.contentVersion === value.contentVersion && Array.isArray(draft.content?.blocks)) {
          recovered = draft.content;
        } else {
          sessionStorage.removeItem(draftKey);
        }
      } catch { /* An unreadable draft must not prevent opening the server copy. */ }
      setAttempt(value);
      setContent(recovered ?? value.content);
      setDirty(!!recovered);
      latest.current = { content: recovered ?? value.content, dirty: !!recovered };
      if (recovered) setBanner({ tone: "warning", text: "Đã khôi phục bản chưa lưu trên thiết bị này" });
      setSavedAt(value.lastSavedAt);
      version.current = value.contentVersion;
      setOffset(new Date(value.serverTime).getTime() - Date.now());
    }, (error) => { if (!controller.signal.aborted) setLoadError(error); });
    return () => controller.abort();
  }, [attemptId, draftKey]);

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
          const merged = mergeSaved(latest.current.content, saved.content);
          latest.current = { content: merged, dirty: true };
          setContent(merged);
        }
        persistDraft(true);
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
          setBanner({ tone: "warning", reload: true, text: msg("assignmentwork.pages.AssignmentWorkspacePage.baiDangMoONoiKhac") });
        } else if (code === "DEADLINE_PASSED" || code === "ATTEMPT_SUBMITTED") {
          halt.current = "closed";
          setBanner({ tone: "danger", text: errorMessage(error) });
        } else if (transient) {
          setBanner({ tone: "warning", text: msg("assignmentwork.pages.AssignmentWorkspacePage.matKetNoiChuaLuu") });
          setRetry((value) => value + 1);
        } else {
          halt.current = "invalid";
          setIssues(problemIssues(error));
          setBanner({ tone: "danger", text: msg("assignmentwork.pages.AssignmentWorkspacePage.chuaLuu", { value: String(errorMessage(error)) }) });
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
  }, [attemptId, persistDraft]);

  // Đếm từ lần sửa đầu tiên chưa lưu, không dời theo từng lần gõ: gõ liên tục vẫn lưu đều mỗi 5 phút.
  useEffect(() => {
    if (!dirty || halt.current) return;
    const timer = setTimeout(() => void save(), AUTOSAVE_MS);
    return () => clearTimeout(timer);
  }, [dirty, retry, save]);

  useEffect(() => {
    const leave = () => {
      if (latest.current.dirty) persistDraft();
      const departure = save(true);
      departures.set(attemptId, departure);
      void departure.finally(() => {
        if (departures.get(attemptId) === departure) departures.delete(attemptId);
      });
    };
    window.addEventListener("pagehide", leave);
    return () => {
      window.removeEventListener("pagehide", leave);
      leave();
    };
  }, [attemptId, save, persistDraft]);

  const sendActivity = useCallback((events: ActivityEventInput[], options: { keepalive: boolean }) =>
    workApi.activity(attemptId, events, options.keepalive), [attemptId]);
  useActivityTracker({ enabled: !!attempt?.activityMonitoringEnabled && attempt.status === "IN_PROGRESS", send: sendActivity });

  const sections = useMemo(() => sectionsOf(content, attempt?.parts ?? []), [content, attempt?.parts]);

  // Sidebar mục lục của bài, như trang soạn của giảng viên; lối về trang bài tập.
  useEffect(() => {
    if (!attempt) return;
    const backHref = `/my-assignments/${encodeURIComponent(attempt.assignmentId)}?classId=${encodeURIComponent(attempt.classId)}`;
    publishOutline({ assignmentId: attempt.assignmentId, backHref, backLabel: msg("assignmentwork.pages.AssignmentWorkspacePage.baiTap"), entries: outlineOf(content.blocks) });
  }, [attempt, content.blocks]);
  useEffect(() => () => publishOutline(null), []);

  function edit(next: DocumentModel) {
    edits.current += 1;
    latest.current = { content: next, dirty: true };
    if (halt.current === "invalid") halt.current = null;
    setContent(next);
    setDirty(true);
  }

  if (loadError) return <Alert tone="danger" role="alert">{errorMessage(loadError)}</Alert>;
  if (!attempt) return <p role="status" className="text-sm text-muted">{msg("assignmentwork.pages.AssignmentWorkspacePage.dangMoBaiLam")}</p>;

  const editable = attempt.editable && attempt.status === "IN_PROGRESS";
  const left = remaining(attempt.deadlineAt, now + offset);
  const beforeDeadline = new Date(attempt.deadlineAt).getTime() > now + offset;
  const active = attempt.parts.find((part) => part.partId === activePart);
  const saveText = saving ? msg("assignmentwork.pages.AssignmentWorkspacePage.dangLuu") : dirty ? msg("assignmentwork.pages.AssignmentWorkspacePage.coThayDoiChuaLuu") : savedAt ? msg("assignmentwork.pages.AssignmentWorkspacePage.daLuuLuc", { value: String(formatTime(savedAt)) }) : msg("assignmentwork.pages.AssignmentWorkspacePage.daLuu");

  // Dòng dưới tên bài như trang xem trước: chỉ ghi loại bài (Luyện tập, hoặc Cá nhân / Nhóm).
  const header = (
    <div className="mb-5 flex flex-col gap-2 border-b border-line pb-4">
      <p className="text-[13px] text-muted" data-testid="assignment-kind">
        {attempt.gradingMode === "PRACTICE" ? msg("assignmentwork.pages.AssignmentWorkspacePage.luyenTap") : attempt.submissionMode === "GROUP" ? msg("assignmentwork.pages.AssignmentWorkspacePage.nhom") : msg("assignmentwork.pages.AssignmentWorkspacePage.caNhan")}
      </p>
    </div>
  );

  return (
    <div data-wide-workspace className="flex flex-col gap-4" style={{ "--doc-toolbar-top": `${56 + headerHeight}px` } as CSSProperties}>
      {/* Quay lại nằm ở sidebar của bài; đầu trang chỉ còn tên bài và trạng thái, như trang xem trước của giảng viên. */}
      <WorkspaceTopBar barRef={headerRef} title={attempt.title} setViewSlot={setViewSlot}
        status={<>
          {/* Cạnh tên bài chỉ có thời gian còn lại (yêu cầu 2026-11-05). */}
          {editable && beforeDeadline && (
            <span title={msg("assignmentwork.pages.AssignmentWorkspacePage.hanNop", { value: String(formatDateTime(attempt.deadlineAt)) })}><Pill tone="warning">{msg("assignmentwork.pages.AssignmentWorkspacePage.con")}{left}</Pill></span>
          )}
          <span data-testid="save-state" className="sr-only" role="status">{saveText}</span>
        </>}
        actions={<>
          <Menu label="" icon="more_horiz" ariaLabel={msg("assignmentwork.pages.AssignmentWorkspacePage.thaoTacKhac")} align="right">
            <MenuItem icon="download" onClick={() => { window.location.href = exportUrl(attempt.attemptId); }}>{msg("assignmentwork.pages.AssignmentWorkspacePage.taiDocx")}</MenuItem>
            {editable && <MenuItem icon="upload_file" onClick={() => setDialog("docx")}>{msg("assignmentwork.pages.AssignmentWorkspacePage.nhapDocx")}</MenuItem>}
          </Menu>
          {editable && (
            <Button variant="primary" icon="check_circle" data-testid="open-submit" onClick={() => {
              // Lưu ngay khi mở popup: lưu xong thì popup dùng kết quả kiểm nộp của server cho đúng bản đang có.
              setDialog("submit");
              void save();
            }}>{msg("assignmentwork.pages.AssignmentWorkspacePage.nopBai")}</Button>
          )}
        </>} />

      {banner && <Alert tone={banner.tone} role="alert" action={banner.reload ? <Button size="sm" onClick={() => window.location.reload()}>{msg("assignmentwork.pages.AssignmentWorkspacePage.taiLai")}</Button> : undefined}>
        {banner.text}
        {issues.length > 0 && <ul className="mt-1 list-disc pl-4">{issues.slice(0, 8).map((issue, i) => <li key={i}>{issue.message}</li>)}</ul>}
      </Alert>}
      {attempt.status === "SUBMITTED" && <Alert tone="info">{msg("assignmentwork.pages.AssignmentWorkspacePage.daNopLuc")}{formatTime(attempt.submittedAt)} {msg("assignmentwork.pages.AssignmentWorkspacePage.chiXem")}</Alert>}
      {!attempt.editable && attempt.status === "IN_PROGRESS" && <Alert tone="warning">{msg("assignmentwork.pages.AssignmentWorkspacePage.daQuaHan")}</Alert>}
      {attempt.activityMonitoringEnabled && attempt.activityNotice && <Alert tone="info" title={msg("assignmentwork.pages.AssignmentWorkspacePage.baiCoGhiNhanHoatDong")}>{attempt.activityNotice}</Alert>}

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
            if (!(await save())) throw new Error(msg("assignmentwork.pages.AssignmentWorkspacePage.baiChuaLuu"));
            await workApi.submit(attempt.attemptId, version.current);
            const target = attempt.classId
              ? `/my-assignments/${encodeURIComponent(attempt.assignmentId)}?classId=${encodeURIComponent(attempt.classId)}`
              : `/my-assignments/${encodeURIComponent(attempt.assignmentId)}`;
            router.push(target);
          }} />
      )}
      {dialog === "docx" && (
        <DocxImportDialog allowReplace={false}
          title={active ? msg("assignmentwork.pages.AssignmentWorkspacePage.nhapDocxVaoPhan", { value: String(active.title.split(" › ").at(-1)) }) : msg("assignmentwork.pages.AssignmentWorkspacePage.nhapDocxVaoPhanCuoiCua")}
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
    <Dialog open title={msg("assignmentwork.pages.AssignmentWorkspacePage.nopBai")} onClose={onClose} dismissible={!busy}
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
        }}>{msg("assignmentwork.pages.AssignmentWorkspacePage.nopBai")}</Button>
      </>}>
      {message && <Alert tone="danger" role="alert">{message}</Alert>}
    </Dialog>
  );
}
