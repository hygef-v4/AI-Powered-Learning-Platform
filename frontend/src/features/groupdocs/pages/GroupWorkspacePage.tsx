"use client";

import { useCallback, useEffect, useMemo, useState, type CSSProperties } from "react";
import { useRouter } from "next/navigation";
import { formatDateTime } from "@/features/groups/model/format";
import { ApiError } from "@/shared/api/client";
import { Alert, BusyOverlay, Button, Dialog, Icon, Menu, MenuItem, Pill, cx, showToast } from "@/shared/ui";
import { useActivityTracker, type ActivityEventInput } from "@/shared/activity/useActivityTracker";
import { DocumentEditor, type DocumentComment } from "@/shared/document";
import { buildOutline, publishOutline } from "@/shared/document/outline";
import { claimSection, downloadDocx, getWorkspace, groupDocImageUrl, releaseSection, sendActivity } from "../api";
import { AssignSectionDialog, SubmitGroupDialog } from "../components/SectionDialogs";
import { SectionHistoryDialog } from "../components/SectionHistoryDialog";
import type { SectionView, StreamEvent, WorkspaceView } from "../model/types";
import { useGroupDocStream } from "../hooks/useGroupDocStream";
import { useGroupDeadlineRefresh } from "../hooks/useGroupDeadlineRefresh";
import { useClock } from "../hooks/useClock";
import { describeDeadline, formatRemaining } from "../model/deadline";
import { assembleGroupDocument } from "../model/document";
import { SECTION_STATUS, resolveMemberName, resolveSectionPerson } from "../model/status";
import { findSectionForHeading } from "../model/outline";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import { NOOP } from "@/shared/noop";
import { WorkspaceTopBar } from "@/shared/layout/WorkspaceTopBar";
import { msg } from "@/i18n/msg";


function formatOptionalDateTime(iso: string | null | undefined): string {
  return iso ? formatDateTime(iso) : "—";
}

/**
 * Màn #40 Assignment Workspace chế độ nhóm (UC 56), cùng khung trang làm bài cá nhân: thanh trên dính (tên bài, thời gian
 * còn lại, chế độ xem, tải DOCX, nộp của trưởng nhóm) và cả tài liệu nhóm trên một trang (khung đề + nội dung đã Xong của
 * từng mục). Trên tiêu đề từng mục là trạng thái và Lịch sử; thẻ bên phải ngang tiêu đề (như rubric) ghi người làm và các
 * nút nhận, giao, nhả mục.
 * Mục theo BR-U12-33, 36: thành viên nhận mục Mở hoặc Xong là soạn ngay; trưởng nhóm giao mục; người giữ, trưởng nhóm
 * hoặc giảng viên nhả mục. Lịch sử mục chỉ xem (BR-U12-35). Realtime qua SSE; tự nộp thì khóa trang trong lúc nộp.
 */
export function GroupWorkspacePage({ documentId }: { documentId: string }) {
  const [view, setView] = useState<WorkspaceView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const router = useRouter();
  const [assigning, setAssigning] = useState<SectionView | null>(null);
  const [releasing, setReleasing] = useState<SectionView | null>(null);
  // Lần nộp vừa lỗi: các mục chưa xong và các mục có lỗi tô đỏ, tới khi mục đó được nhận, giao hay nhả.
  const [invalid, setInvalid] = useState<string[]>([]);
  const [confirmingEdit, setConfirmingEdit] = useState<SectionView | null>(null);
  const clearInvalid = (id: string) => setInvalid((prev) => prev.filter((x) => x !== id));
  const [history, setHistory] = useState<SectionView | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [autoSubmitting, setAutoSubmitting] = useState(false);
  const [viewSlot, setViewSlot] = useState<HTMLElement | null>(null);
  const [headerHeight, setHeaderHeight] = useState(64);
  const [headerObserver, setHeaderObserver] = useState<ResizeObserver | null>(null);
  const headerRef = useCallback((element: HTMLElement | null) => {
    if (!element || typeof ResizeObserver === "undefined") return;
    const observer = new ResizeObserver(() => setHeaderHeight(element.offsetHeight));
    observer.observe(element);
    setHeaderObserver(observer);
  }, []);
  useEffect(() => () => headerObserver?.disconnect(), [headerObserver]);

  const reload = useCallback(
    (signal?: AbortSignal) =>
      getWorkspace(documentId, signal).then(
        (loaded) => {
          if (signal?.aborted) return;
          setView((previous) => previous?.documentId === loaded.documentId && previous.version > loaded.version
            ? previous : loaded);
          setError(null);
        },
        (e: unknown) => {
          if (!signal?.aborted) setError(e instanceof ApiError && e.problem.status === 404 ? msg("groupdocs.pages.GroupWorkspacePage.groupDocumentNotFound") : msg("groupdocs.pages.GroupWorkspacePage.couldNotLoadTheGroup"));
        },
      ),
    [documentId],
  );

  useEffect(() => {
    let active = true;
    getWorkspace(documentId).then(
      (loaded) => {
        if (active) setView(loaded);
      },
      (e: unknown) => {
        if (active) setError(e instanceof ApiError && e.problem.status === 404 ? msg("groupdocs.pages.GroupWorkspacePage.groupDocumentNotFound") : msg("groupdocs.pages.GroupWorkspacePage.couldNotLoadTheGroup"));
      },
    );
    return () => {
      active = false;
    };
  }, [documentId]);

  const onEvent = useCallback(
    (event: StreamEvent) => {
      if (event.type === "document.auto-submitted" || event.type === "document.closed") {
        setAutoSubmitting(true);
        void reload().then(() => setAutoSubmitting(false));
        return;
      }
      void reload();
    },
    [reload],
  );
  useGroupDocStream(view ? documentId : null, onEvent, () => void reload());
  useGroupDeadlineRefresh(view, reload);
  const now = useClock();

  const send = useCallback(
    (events: ActivityEventInput[], options: { keepalive: boolean }) => sendActivity(documentId, events, options.keepalive),
    [documentId],
  );
  useActivityTracker({
    enabled: !!view && view.activityMonitoring && view.role.member && view.status === "IN_PROGRESS" && view.window !== "CLOSED",
    send,
  });

  // Sidebar mục lục như trang làm bài cá nhân: đăng dàn ý của khung đề, lối về Bài tập.
  useEffect(() => {
    if (!view) return;
    const backHref = view.role.teacher
      ? `/teaching/classes/${encodeURIComponent(view.classId)}?tab=students`
      : `/my-assignments/${encodeURIComponent(view.assignmentId)}?classId=${encodeURIComponent(view.classId)}`;
    // Đề có heading tự gõ số ("1. Đặc tả") thì bỏ số trùng với số sidebar tự đánh, tránh "1. 1. Đặc tả".
    const entries = buildOutline(view.sharedBlocks.blocks).map((entry) =>
      entry.number && entry.title.startsWith(entry.number) ? { ...entry, title: entry.title.slice(entry.number.length).trim() } : entry,
    );
    publishOutline({ assignmentId: view.assignmentId, backHref, backLabel: msg("groupdocs.pages.GroupWorkspacePage.assignment"), entries });
  }, [view]);
  useEffect(() => () => publishOutline(null), []);

  // Soạn ở trang riêng (mọi mục mình giữ), mở đúng mục vừa bấm.
  const openEditor = (s: Pick<SectionView, "partId">) =>
    router.push(`/group-docs/${encodeURIComponent(documentId)}/edit?section=${encodeURIComponent(s.partId)}`);
  const groupDocument = useMemo(() => (view ? assembleGroupDocument(view.sharedBlocks, view.sections) : null), [view]);

  const download = async () => {
    if (!view) return;
    try {
      const blob = await downloadDocx(view.documentId, false, groupDocument?.blocks ?? []);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${view.assignmentTitle ?? "bai-nhom"} - ${view.groupName}.docx`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      setNotice(msg("groupdocs.pages.GroupWorkspacePage.couldNotDownloadTheDocx"));
    }
  };

  const runAction = async (action: () => Promise<unknown>, fallback: string, sectionId?: string) => {
    setNotice(null);
    try {
      await action();
      if (sectionId) clearInvalid(sectionId);
      await reload();
    } catch (e) {
      setNotice(e instanceof ApiError ? (e.problem.detail ?? fallback) : fallback);
    }
  };

  // Nhận mục chưa làm hoặc Xong là soạn ngay (BR-U12-33): sang luôn trang soạn.
  const claimAndEdit = async (s: SectionView) => {
    setNotice(null);
    try {
      const claimed = await claimSection(documentId, s.id);
      clearInvalid(s.id);
      openEditor(claimed);
    } catch (e) {
      setNotice(e instanceof ApiError ? (e.problem.detail ?? msg("groupdocs.pages.GroupWorkspacePage.couldNotTakeTheSection")) : msg("groupdocs.pages.GroupWorkspacePage.couldNotTakeTheSection"));
    }
  };

  if (error && !view) return <PageLoadError>{error}</PageLoadError>;
  if (!view || !groupDocument) return <PageLoading label={msg("groupdocs.pages.GroupWorkspacePage.loadingGroupDocument")} />;

  const me = view.role;
  const editable = view.status === "IN_PROGRESS" && view.window !== "CLOSED";

  const deadline = describeDeadline(view, now);
  const cutoff = view.window === "LATE" ? view.lateUntil : view.closesAt;
  const left = editable ? formatRemaining(cutoff, now) : null;
  const myId = view.viewerId;
  const resolveName = (id: string | null) => resolveMemberName(view, id);
  const isMine = (s: SectionView) => s.status === "CLAIMED" && s.holderId === myId;
  const isClaimable = (s: SectionView) => editable && me.member && s.status !== "CLAIMED";
  const failed = new Set(invalid);
  // Mục Xong do người khác viết: hỏi lại trước khi nhận để sửa.
  const handleClaim = (s: SectionView) => {
    if (s.status === "DONE" && s.lastAuthorId && s.lastAuthorId !== myId) setConfirmingEdit(s);
    else void claimAndEdit(s);
  };
  // Số thứ tự như trên trang (I., I.1.), đặt trước tên mục ở thẻ phân công như thẻ rubric.
  const numbers = new Map(buildOutline(groupDocument.blocks).map((entry) => [entry.id, entry.number]));
  const formatSectionName = (s: SectionView) => [numbers.get(s.partId), s.title].filter(Boolean).join(" ");
  // Trên tiêu đề mục chỉ còn trạng thái và Lịch sử.
  const nodeActions = Object.fromEntries(view.sections.map((s) => [s.partId, (
    <div key={s.id} role="group" aria-label={s.title} className="flex items-center gap-1.5">
      <Pill tone={SECTION_STATUS[s.status].tone}>{SECTION_STATUS[s.status].text}</Pill>
      <Button size="sm" variant="ghost" icon="history" onClick={() => setHistory(s)}>{msg("groupdocs.pages.GroupWorkspacePage.history")}</Button>
    </div>
  )]));
  // Thẻ phân công bên phải, ngang tiêu đề mục như thẻ rubric: người làm và các nút nhận, giao, nhả mục.
  const assignmentCards: DocumentComment[] = view.sections.map((s) => {
    const mine = isMine(s);
    return {
      key: s.id,
      anchorBlockId: s.partId,
      content: (
        <section aria-label={s.title} data-own={mine || undefined} data-invalid={failed.has(s.id) || undefined}
          className={cx("flex flex-col gap-2.5 rounded-xl border bg-surface p-3 shadow-sm",
            failed.has(s.id) ? "border-danger bg-danger-soft" : mine ? "border-primary" : "border-line")}>
          <p className="truncate text-[13px] font-semibold text-ink" title={formatSectionName(s)}>{formatSectionName(s)}</p>
          <p className="flex items-center gap-2 text-[13px]">
            <Icon name="person" size={16} filled={resolveSectionPerson(s) !== null}
              className={cx("shrink-0", resolveSectionPerson(s) !== null ? "text-primary-ink" : "text-muted")} />
            <span className="text-muted">{msg("groupdocs.pages.GroupWorkspacePage.author")}</span>
            <span className="ml-auto min-w-0 truncate font-semibold text-ink">{resolveName(resolveSectionPerson(s))}</span>
          </p>
          {editable && (me.member || me.teacher) && (
            <div className="flex flex-wrap gap-1.5">
              {isClaimable(s) && (
                <Button size="sm" variant="primary" onClick={() => handleClaim(s)}>{s.status === "DONE" ? msg("groupdocs.pages.GroupWorkspacePage.editSection") : msg("groupdocs.pages.GroupWorkspacePage.takeSection")}</Button>
              )}
              {mine && <Button size="sm" variant="primary" onClick={() => openEditor(s)}>{msg("groupdocs.pages.GroupWorkspacePage.continueWriting")}</Button>}
              {me.leader && <Button size="sm" onClick={() => setAssigning(s)}>{msg("groupdocs.pages.GroupWorkspacePage.assignSection")}</Button>}
              {s.status === "CLAIMED" && (mine || me.leader || me.teacher) && (
                <Button size="sm" variant="ghost" className="text-danger! hover:bg-danger-soft" onClick={() => setReleasing(s)}>{msg("groupdocs.pages.GroupWorkspacePage.releaseSection")}</Button>
              )}
            </div>
          )}
          {failed.has(s.id) && <p className="text-xs text-danger">{msg("groupdocs.pages.GroupWorkspacePage.thisSectionIsNotDone")}</p>}
        </section>
      ),
    };
  });
  const title = view.assignmentTitle ?? msg("groupdocs.pages.GroupWorkspacePage.groupAssignment");
  const header = (
    <div className="mb-5 flex flex-col gap-2 border-b border-line pb-4">
      <p className="text-[13px] text-muted" data-testid="assignment-kind">{msg("groupdocs.pages.GroupWorkspacePage.group")}</p>
    </div>
  );

  return (
    <div data-wide-workspace className="flex flex-col gap-4" style={{ "--doc-toolbar-top": `${56 + headerHeight}px` } as CSSProperties}>
      <WorkspaceTopBar barRef={headerRef} title={title} setViewSlot={setViewSlot}
        status={<>
          {left && <span title={msg("groupdocs.pages.GroupWorkspacePage.deadline", { value: String(formatOptionalDateTime(cutoff)) })}><Pill tone="warning">{msg("groupdocs.pages.GroupWorkspacePage.text")}{left}</Pill></span>}
        </>}
        actions={<>
          {me.member && (
            <Menu label="" icon="more_horiz" ariaLabel={msg("groupdocs.pages.GroupWorkspacePage.moreActions")} align="right">
              {me.member && <MenuItem icon="download" onClick={() => void download()}>{msg("groupdocs.pages.GroupWorkspacePage.downloadDocx")}</MenuItem>}
            </Menu>
          )}
          {me.leader && editable && (
            <Button variant="primary" icon="check_circle" onClick={() => setSubmitting(true)}>
              {msg("groupdocs.pages.GroupWorkspacePage.submitDocument")}</Button>
          )}
        </>} />

      {notice && <Alert tone="danger" role="alert">{notice}</Alert>}
      {!editable && view.status === "IN_PROGRESS" && <Alert tone="info">{msg("groupdocs.pages.GroupWorkspacePage.theDeadlineHasPassedThe")}</Alert>}
      {deadline && !me.teacher && (
        <Alert tone={deadline.tone} title={deadline.title}>
          <ul className="list-disc pl-4">
            {deadline.details.map((line) => <li key={line}>{line}</li>)}
          </ul>
        </Alert>
      )}

      <DocumentEditor fullWidth document={groupDocument} onChange={NOOP} mode="student" readOnly diagramEnabled={view.diagramEnabled}
        imageUrl={groupDocImageUrl(documentId)}
        titleField={{ text: title, readOnly: true, onText: NOOP }}
        header={header}
        sectionActions={nodeActions}
        comments={assignmentCards}
        emptyLeafLabel={msg("groupdocs.pages.GroupWorkspacePage.noContentYet")}
        viewSlot={viewSlot} />

      {history && <SectionHistoryDialog view={view} section={history} onClose={() => setHistory(null)} />}
      {editable && assigning && <AssignSectionDialog view={view} section={assigning} onClose={() => setAssigning(null)} onChanged={() => { clearInvalid(assigning.id); void reload(); }} />}
      {editable && submitting && (
        <SubmitGroupDialog
          view={view}
          onClose={() => setSubmitting(false)}
          onSubmitted={() => {
            setSubmitting(false);
            setNotice(null);
            setInvalid([]);
            showToast(msg("groupdocs.pages.GroupWorkspacePage.groupDocumentSubmitted"));
            // Nộp xong về trang chi tiết bài.
            router.push(`/my-assignments/${encodeURIComponent(view.assignmentId)}?classId=${encodeURIComponent(view.classId)}`);
          }}
          onFailed={(message, blockIds) => {
            setSubmitting(false);
            setInvalid(view.sections.filter((s) => s.status !== "DONE"
              || blockIds.some((id) => findSectionForHeading(view.sharedBlocks.blocks, view.sections, id)?.id === s.id
                || (s.publishedBlocks ?? []).some((b) => b.id === id))).map((s) => s.id));
            showToast(message, "danger");
            void reload();
          }}
        />
      )}
      <Dialog
        open={editable && releasing !== null}
        title={msg("groupdocs.pages.GroupWorkspacePage.releaseSection")}
        icon="warning"
        tone="warning"
        onClose={() => setReleasing(null)}
        footer={
          <>
            <Button
              variant="danger"
              onClick={() => {
                const s = releasing;
                setReleasing(null);
                if (s) void runAction(() => releaseSection(view.documentId, s.id), msg("groupdocs.pages.GroupWorkspacePage.couldNotReleaseTheSection"), s.id);
              }}
            >
              {msg("groupdocs.pages.GroupWorkspacePage.releaseSection")}</Button>
          </>
        }
      >
        {releasing && <p>{msg("groupdocs.pages.GroupWorkspacePage.releaseSection2")}<strong>{formatSectionName(releasing)}</strong> {msg("groupdocs.pages.GroupWorkspacePage.of")}<strong>{resolveName(resolveSectionPerson(releasing))}</strong>{msg("groupdocs.pages.GroupWorkspacePage.theDraftInProgressFor")}</p>}
      </Dialog>
      <Dialog
        open={editable && confirmingEdit !== null}
        title={msg("groupdocs.pages.GroupWorkspacePage.editSection")}
        icon="edit"
        onClose={() => setConfirmingEdit(null)}
        footer={
          <>
            <Button onClick={() => setConfirmingEdit(null)}>{msg("groupdocs.pages.GroupWorkspacePage.back")}</Button>
            <Button
              variant="primary"
              onClick={() => {
                const s = confirmingEdit;
                setConfirmingEdit(null);
                if (s) void claimAndEdit(s);
              }}
            >
              {msg("groupdocs.pages.GroupWorkspacePage.editSection")}</Button>
          </>
        }
      >
        <Alert tone="warning">{msg("groupdocs.pages.GroupWorkspacePage.someoneIsAlreadyWorkingOn")}</Alert>
      </Dialog>
      {autoSubmitting && (
        <BusyOverlay label={msg("groupdocs.pages.GroupWorkspacePage.submitting")}>
          <p className="text-[16px] font-bold text-ink">{msg("groupdocs.pages.GroupWorkspacePage.submitting2")}</p>
          <p className="mt-2 text-[13px] text-body">
            {msg("groupdocs.pages.GroupWorkspacePage.theDeadlineHasPassed")}{view.closesAt && msg("groupdocs.pages.GroupWorkspacePage.at", { value: String(formatOptionalDateTime(view.closesAt)) })}{msg("groupdocs.pages.GroupWorkspacePage.theSystemIsAutomaticallySubmitting")}</p>
        </BusyOverlay>
      )}
    </div>
  );
}
