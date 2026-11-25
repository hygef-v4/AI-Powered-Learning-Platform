"use client";

import { useCallback, useEffect, useMemo, useRef, useState, type CSSProperties } from "react";
import { useRouter } from "next/navigation";
import { uploadFile } from "@/features/files/api";
import { ApiError } from "@/shared/api/client";
import { useActivityTracker, type ActivityEventInput } from "@/shared/activity/useActivityTracker";
import {
  DocumentEditor,
  DocxImportDialog,
  buildOutline,
  publishOutline,
  numberSections,
  type DocumentBlock,
  type DocumentModel,
  type DocumentPart,
  type ValidationIssue,
} from "@/shared/document";
import { Alert, Button, Dialog, Menu, MenuItem, Pill, showErrorToast, showToast } from "@/shared/ui";
import { attemptImageUrl, downloadAttemptDocx, workApi } from "../api";
import { checkDocxStructure, mergeImportedWork } from "../model/docxMerge";
import { readErrorCode, describeError, formatDateTime, formatTime, readProblemIssues, describeRemaining } from "../model/format";

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
  /** Nhập DOCX là một bước trong lịch sử của trình soạn: Ctrl+Z quay về bản trước khi nhập, Ctrl+Y nhập lại. */
  const [importEdit, setImportEdit] = useState<{ id: number; before: DocumentModel } | null>(null);
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
  const [autosaveCycle, setAutosaveCycle] = useState(0);
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
      if (recovered) setBanner({ tone: "warning", text: msg("assignmentwork.pages.AssignmentWorkspacePage.restoredTheUnsavedVersionOn") });
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
          setAutosaveCycle((cycle) => cycle + 1);
        }
        persistDraft(true);
        halt.current = null;
        setIssues([]);
        setBanner(null);
        return true;
      } catch (error) {
        // Giữ đánh dấu chưa lưu trong mọi trường hợp: Nộp chỉ đi tiếp khi lần lưu cuối thành công.
        const code = readErrorCode(error);
        const transient = !(error instanceof ApiError) || error.problem.status >= 500;
        if (code === "CONTENT_VERSION_CONFLICT") {
          halt.current = "conflict";
          setBanner({ tone: "warning", reload: true, text: msg("assignmentwork.pages.AssignmentWorkspacePage.theWorkIsOpenElsewhere") });
        } else if (code === "DEADLINE_PASSED" || code === "ATTEMPT_SUBMITTED") {
          halt.current = "closed";
          setBanner({ tone: "danger", text: describeError(error) });
        } else if (transient) {
          setBanner({ tone: "warning", text: msg("assignmentwork.pages.AssignmentWorkspacePage.connectionLostNotSaved") });
          setAutosaveCycle((value) => value + 1);
        } else {
          halt.current = edits.current === startEdits ? "invalid" : null;
          if (!halt.current) setAutosaveCycle((cycle) => cycle + 1);
          setIssues(readProblemIssues(error));
          setBanner({ tone: "danger", text: msg("assignmentwork.pages.AssignmentWorkspacePage.notSaved", { value: String(describeError(error)) }) });
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
    const timer = setTimeout(() => {
      if (!halt.current) void save();
    }, AUTOSAVE_MS);
    return () => clearTimeout(timer);
  }, [dirty, autosaveCycle, save]);

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

  const sections = useMemo(() => numberSections(content, attempt?.parts ?? []), [content, attempt?.parts]);

  // Sidebar mục lục của bài, như trang soạn của giảng viên; lối về trang bài tập.
  useEffect(() => {
    if (!attempt) return;
    const backHref = `/my-assignments/${encodeURIComponent(attempt.assignmentId)}?classId=${encodeURIComponent(attempt.classId)}`;
    publishOutline({ assignmentId: attempt.assignmentId, backHref, backLabel: msg("assignmentwork.pages.AssignmentWorkspacePage.assignment"), entries: buildOutline(content.blocks) });
  }, [attempt, content.blocks]);
  useEffect(() => () => publishOutline(null), []);

  function edit(next: DocumentModel) {
    edits.current += 1;
    latest.current = { content: next, dirty: true };
    if (halt.current === "invalid") {
      halt.current = null;
      setAutosaveCycle((cycle) => cycle + 1);
    }
    setContent(next);
    setDirty(true);
  }

  if (loadError) return <Alert tone="danger" role="alert">{describeError(loadError)}</Alert>;
  if (!attempt) return <p role="status" className="text-sm text-muted">{msg("assignmentwork.pages.AssignmentWorkspacePage.openingYourWork")}</p>;

  const editable = attempt.editable && attempt.status === "IN_PROGRESS";
  const left = describeRemaining(attempt.deadlineAt, now + offset);
  const beforeDeadline = new Date(attempt.deadlineAt).getTime() > now + offset;
  const active = attempt.parts.find((part) => part.partId === activePart);
  const saveText = saving ? msg("assignmentwork.pages.AssignmentWorkspacePage.saving") : dirty ? msg("assignmentwork.pages.AssignmentWorkspacePage.unsavedChanges") : savedAt ? msg("assignmentwork.pages.AssignmentWorkspacePage.savedAt", { value: String(formatTime(savedAt)) }) : msg("assignmentwork.pages.AssignmentWorkspacePage.saved");

  // Dòng dưới tên bài như trang xem trước: chỉ ghi loại bài (Luyện tập, hoặc Cá nhân / Nhóm).
  const header = (
    <div className="mb-5 flex flex-col gap-2 border-b border-line pb-4">
      <p className="text-[13px] text-muted" data-testid="assignment-kind">
        {attempt.gradingMode === "PRACTICE" ? msg("assignmentwork.pages.AssignmentWorkspacePage.practice") : attempt.submissionMode === "GROUP" ? msg("assignmentwork.pages.AssignmentWorkspacePage.group") : msg("assignmentwork.pages.AssignmentWorkspacePage.individual")}
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
            <span title={msg("assignmentwork.pages.AssignmentWorkspacePage.deadline", { value: String(formatDateTime(attempt.deadlineAt)) })}><Pill tone="warning">{msg("assignmentwork.pages.AssignmentWorkspacePage.text")}{left}</Pill></span>
          )}
          <span data-testid="save-state" className="sr-only" role="status">{saveText}</span>
        </>}
        actions={<>
          <Menu label="" icon="more_horiz" ariaLabel={msg("assignmentwork.pages.AssignmentWorkspacePage.moreActions")} align="right">
            <MenuItem icon="download" onClick={() => {
              void downloadAttemptDocx(attempt.attemptId)
                .catch((error: unknown) => showErrorToast(describeError(error)));
            }}>{msg("assignmentwork.pages.AssignmentWorkspacePage.downloadDocx")}</MenuItem>
            {editable && <MenuItem icon="upload_file" onClick={() => setDialog("docx")}>{msg("assignmentwork.pages.AssignmentWorkspacePage.importDocx")}</MenuItem>}
          </Menu>
          {editable && (
            <Button variant="primary" icon="check_circle" data-testid="open-submit" onClick={() => {
              // Lưu ngay khi mở popup: lưu xong thì popup dùng kết quả kiểm nộp của server cho đúng bản đang có.
              setDialog("submit");
              void save();
            }}>{msg("assignmentwork.pages.AssignmentWorkspacePage.submit")}</Button>
          )}
        </>} />

      {banner && <Alert tone={banner.tone} role="alert" action={banner.reload ? <Button size="sm" onClick={() => window.location.reload()}>{msg("assignmentwork.pages.AssignmentWorkspacePage.reload")}</Button> : undefined}>
        {banner.text}
        {issues.length > 0 && <ul className="mt-1 list-disc pl-4">{issues.slice(0, 8).map((issue, i) => <li key={i}>{issue.message}</li>)}</ul>}
      </Alert>}
      {attempt.status === "SUBMITTED" && <Alert tone="info">{msg("assignmentwork.pages.AssignmentWorkspacePage.submittedAt")}{formatTime(attempt.submittedAt)} {msg("assignmentwork.pages.AssignmentWorkspacePage.viewOnly")}</Alert>}
      {!attempt.editable && attempt.status === "IN_PROGRESS" && <Alert tone="warning">{msg("assignmentwork.pages.AssignmentWorkspacePage.pastDeadline")}</Alert>}
      {attempt.activityMonitoringEnabled && attempt.activityNotice && <Alert tone="info" title={msg("assignmentwork.pages.AssignmentWorkspacePage.activityIsMonitored")}>{attempt.activityNotice}</Alert>}

      <DocumentEditor fullWidth document={content} mode="student" answerGuidance diagramEnabled={attempt.diagramEnabled} readOnly={!editable}
        imageUrl={attemptImageUrl(attempt.attemptId)} issues={issues}
        uploadImage={async (file) => (await uploadFile(file, "DOCUMENT_IMAGE", () => {}).promise).fileRef.value}
        onChange={edit}
        externalEdit={importEdit}
        titleField={{ text: attempt.title, readOnly: true, onText: NOOP }}
        header={header}
        highlightBlockId={null}
        onFocusBlock={(blockId) => setActivePart(sections.get(blockId) ?? null)}
        viewSlot={viewSlot} />

      {dialog === "submit" && (
        <SubmitDialog onClose={() => setDialog(null)}
          onSubmit={async () => {
            if (!(await save())) throw new Error(msg("assignmentwork.pages.AssignmentWorkspacePage.workNotSaved"));
            await workApi.submit(attempt.attemptId, version.current);
            const target = attempt.classId
              ? `/my-assignments/${encodeURIComponent(attempt.assignmentId)}?classId=${encodeURIComponent(attempt.classId)}`
              : `/my-assignments/${encodeURIComponent(attempt.assignmentId)}`;
            router.push(target);
          }} />
      )}
      {dialog === "docx" && (
        // Nhập DOCX (yêu cầu 2026-11-25): tệp phải cùng cấu trúc với bài; mỗi phần chỉ thêm bài mới, giữ định dạng.
        <DocxImportDialog allowReplace={false} showMode={false}
          title={msg("assignmentwork.pages.AssignmentWorkspacePage.importDocx")}
          preview={async (file) => {
            const result = await workApi.importDocx(attempt.attemptId, file);
            checkDocxStructure(latest.current.content, result.document, attempt.title);
            return result;
          }}
          onClose={() => setDialog(null)}
          onImport={(document) => {
            const before = latest.current.content;
            const merged = mergeImportedWork(before, attempt.parts, document, attempt.title);
            if (merged.updatedParts.length === 0) {
              showToast(msg("assignmentwork.pages.AssignmentWorkspacePage.nothingNewToImport"), "warning");
              return;
            }
            setImportEdit((current) => ({ id: (current?.id ?? 0) + 1, before }));
            edit(merged.content);
            showToast(msg("assignmentwork.pages.AssignmentWorkspacePage.importedIntoParts", {
              count: String(merged.updatedParts.length), parts: merged.updatedParts.map((title) => title.split(" › ").at(-1)).join(", ") }));
          }} />
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
    <Dialog open title={msg("assignmentwork.pages.AssignmentWorkspacePage.submit")} onClose={onClose} dismissible={!busy}
      footer={<>
        <Button variant="primary" disabled={busy} data-testid="confirm-submit" onClick={async () => {
          setBusy(true);
          setMessage(null);
          try {
            await onSubmit();
          } catch (error) {
            setMessage(describeError(error));
            setBusy(false);
          }
        }}>{msg("assignmentwork.pages.AssignmentWorkspacePage.submit")}</Button>
      </>}>
      {message && <Alert tone="danger" role="alert">{message}</Alert>}
    </Dialog>
  );
}
