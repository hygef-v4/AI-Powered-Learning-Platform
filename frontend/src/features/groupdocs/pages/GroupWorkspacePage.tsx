"use client";

import { useCallback, useEffect, useMemo, useState, type CSSProperties } from "react";
import { createPortal } from "react-dom";
import { useRouter } from "next/navigation";
import { formatDateTime } from "@/features/groups/model/format";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Dialog, Icon, Menu, MenuItem, Pill, cx, showToast } from "@/shared/ui";
import { useActivityTracker, type ActivityEventInput } from "@/shared/activity/useActivityTracker";
import { DocumentEditor, type DocumentComment } from "@/shared/document";
import { outlineOf, publishOutline } from "@/shared/document/outline";
import { claimSection, downloadDocx, getWorkspace, groupDocImageUrl, releaseSection, sendActivity } from "../api";
import { AssignSectionDialog, SubmitGroupDialog } from "../components/SectionDialogs";
import { SectionHistoryDialog } from "../components/SectionHistoryDialog";
import type { SectionView, StreamEvent, WorkspaceView } from "../model/types";
import { useGroupDocStream } from "../hooks/useGroupDocStream";
import { useClock } from "../hooks/useClock";
import { describeDeadline, formatRemaining } from "../model/deadline";
import { assembleGroupDocument } from "../model/document";
import { SECTION_STATUS, memberName, sectionPerson } from "../model/status";
import { findSectionForHeading } from "../model/outline";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";

const NOOP = () => {};

function dateTime(iso: string | null | undefined): string {
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
    () =>
      getWorkspace(documentId).then(
        (loaded) => {
          setView(loaded);
          setError(null);
        },
        (e: unknown) => setError(e instanceof ApiError && e.problem.status === 404 ? "Không tìm thấy tài liệu nhóm." : "Không tải được tài liệu nhóm."),
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
        if (active) setError(e instanceof ApiError && e.problem.status === 404 ? "Không tìm thấy tài liệu nhóm." : "Không tải được tài liệu nhóm.");
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
    const entries = outlineOf(view.sharedBlocks.blocks).map((entry) =>
      entry.number && entry.title.startsWith(entry.number) ? { ...entry, title: entry.title.slice(entry.number.length).trim() } : entry,
    );
    publishOutline({ assignmentId: view.assignmentId, backHref, backLabel: "Bài tập", entries });
  }, [view]);
  useEffect(() => () => publishOutline(null), []);

  // Soạn ở trang riêng (mọi mục mình giữ), mở đúng mục vừa bấm.
  const openEditor = (s: Pick<SectionView, "partId">) =>
    router.push(`/group-docs/${encodeURIComponent(documentId)}/edit?section=${encodeURIComponent(s.partId)}`);
  const groupDocument = useMemo(() => (view ? assembleGroupDocument(view.sharedBlocks, view.sections) : null), [view]);

  const download = async () => {
    if (!view) return;
    try {
      const blob = await downloadDocx(view.documentId, false);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${view.assignmentTitle ?? "bai-nhom"} - ${view.groupName}.docx`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      setNotice("Không tải được DOCX.");
    }
  };

  const act = async (action: () => Promise<unknown>, fallback: string, sectionId?: string) => {
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
      setNotice(e instanceof ApiError ? (e.problem.detail ?? "Không nhận được mục.") : "Không nhận được mục.");
    }
  };

  if (error && !view) return <PageLoadError>{error}</PageLoadError>;
  if (!view || !groupDocument) return <PageLoading label="Đang tải tài liệu nhóm…" />;

  const me = view.role;
  const editable = view.status === "IN_PROGRESS" && view.window !== "CLOSED";

  const deadline = describeDeadline(view, now);
  const cutoff = view.window === "LATE" ? view.lateUntil : view.closesAt;
  const left = editable ? formatRemaining(cutoff, now) : null;
  const myId = view.viewerId;
  const name = (id: string | null) => memberName(view, id);
  const isMine = (s: SectionView) => s.status === "CLAIMED" && s.holderId === myId;
  const claimable = (s: SectionView) => editable && me.member && s.status !== "CLAIMED";
  const failed = new Set(invalid);
  // Mục Xong do người khác viết: hỏi lại trước khi nhận để sửa.
  const handleClaim = (s: SectionView) => {
    if (s.status === "DONE" && s.lastAuthorId && s.lastAuthorId !== myId) setConfirmingEdit(s);
    else void claimAndEdit(s);
  };
  // Số thứ tự như trên trang (I., I.1.), đặt trước tên mục ở thẻ phân công như thẻ rubric.
  const numbers = new Map(outlineOf(groupDocument.blocks).map((entry) => [entry.id, entry.number]));
  const sectionName = (s: SectionView) => [numbers.get(s.partId), s.title].filter(Boolean).join(" ");
  // Trên tiêu đề mục chỉ còn trạng thái và Lịch sử.
  const nodeActions = Object.fromEntries(view.sections.map((s) => [s.partId, (
    <div key={s.id} role="group" aria-label={s.title} className="flex items-center gap-1.5">
      <Pill tone={SECTION_STATUS[s.status].tone}>{SECTION_STATUS[s.status].text}</Pill>
      <Button size="sm" variant="ghost" icon="history" onClick={() => setHistory(s)}>Lịch sử</Button>
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
          <p className="truncate text-[13px] font-semibold text-ink" title={sectionName(s)}>{sectionName(s)}</p>
          <p className="flex items-center gap-2 text-[13px]">
            <Icon name="person" size={16} filled={sectionPerson(s) !== null}
              className={cx("shrink-0", sectionPerson(s) !== null ? "text-primary-ink" : "text-muted")} />
            <span className="text-muted">Người làm</span>
            <span className="ml-auto min-w-0 truncate font-semibold text-ink">{name(sectionPerson(s))}</span>
          </p>
          {editable && (me.member || me.teacher) && (
            <div className="flex flex-wrap gap-1.5">
              {claimable(s) && (
                <Button size="sm" variant="primary" onClick={() => handleClaim(s)}>{s.status === "DONE" ? "Sửa mục" : "Nhận mục"}</Button>
              )}
              {mine && <Button size="sm" variant="primary" onClick={() => openEditor(s)}>Soạn tiếp</Button>}
              {me.leader && <Button size="sm" onClick={() => setAssigning(s)}>Giao mục</Button>}
              {s.status === "CLAIMED" && (mine || me.leader || me.teacher) && (
                <Button size="sm" variant="ghost" className="text-danger! hover:bg-danger-soft" onClick={() => setReleasing(s)}>Nhả mục</Button>
              )}
            </div>
          )}
          {failed.has(s.id) && <p className="text-xs text-danger">Mục này chưa xong.</p>}
        </section>
      ),
    };
  });
  const title = view.assignmentTitle ?? "Bài nhóm";
  const header = (
    <div className="mb-5 flex flex-col gap-2 border-b border-line pb-4">
      <p className="text-[13px] text-muted" data-testid="assignment-kind">Nhóm</p>
    </div>
  );

  return (
    <div data-wide-workspace className="flex flex-col gap-4" style={{ "--doc-toolbar-top": `${56 + headerHeight}px` } as CSSProperties}>
      <header ref={headerRef} className="sticky top-14 z-30 main-bleed -mt-8 grid grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-x-4 gap-y-2 border-b border-line bg-[var(--c-topbar)] py-3 backdrop-blur-md md:-mt-10">
        <div className="flex min-w-0 flex-1 items-center gap-x-3">
          <h1 className="min-w-0 truncate text-[15px] font-semibold text-ink" title={title}>{title}</h1>
          {left && <span title={`Hạn nộp: ${dateTime(cutoff)}`}><Pill tone="warning">Còn {left}</Pill></span>}
        </div>
        <div ref={setViewSlot} data-testid="view-slot" className="flex justify-center" />
        <div className="flex flex-wrap items-center justify-end gap-2">
          {me.member && (
            <Menu label="" icon="more_horiz" ariaLabel="Thao tác khác" align="right">
              {me.member && <MenuItem icon="download" onClick={() => void download()}>Tải DOCX</MenuItem>}
            </Menu>
          )}
          {me.leader && editable && (
            <Button variant="primary" icon="check_circle" onClick={() => setSubmitting(true)}>
              Nộp tài liệu
            </Button>
          )}
        </div>
      </header>

      {notice && <Alert tone="danger" role="alert">{notice}</Alert>}
      {!editable && view.status === "IN_PROGRESS" && <Alert tone="info">Đã hết hạn nộp; tài liệu chỉ còn xem.</Alert>}
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
        emptyLeafLabel="Chưa có nội dung."
        viewSlot={viewSlot} />

      {history && <SectionHistoryDialog view={view} section={history} onClose={() => setHistory(null)} />}
      {assigning && <AssignSectionDialog view={view} section={assigning} onClose={() => setAssigning(null)} onChanged={() => { clearInvalid(assigning.id); void reload(); }} />}
      {submitting && (
        <SubmitGroupDialog
          view={view}
          onClose={() => setSubmitting(false)}
          onSubmitted={() => {
            setSubmitting(false);
            setNotice(null);
            setInvalid([]);
            showToast("Đã nộp tài liệu nhóm");
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
        open={releasing !== null}
        title="Nhả mục"
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
                if (s) void act(() => releaseSection(view.documentId, s.id), "Không nhả được mục.", s.id);
              }}
            >
              Nhả mục
            </Button>
          </>
        }
      >
        {releasing && <p>Nhả mục <strong>{sectionName(releasing)}</strong> của <strong>{name(sectionPerson(releasing))}</strong>?
          Bản nháp đang làm của mục này sẽ bị bỏ. Nội dung đã hoàn thành trước đó được giữ lại; thành viên có thể nhận lại mục.</p>}
      </Dialog>
      <Dialog
        open={confirmingEdit !== null}
        title="Sửa mục"
        icon="edit"
        onClose={() => setConfirmingEdit(null)}
        footer={
          <>
            <Button onClick={() => setConfirmingEdit(null)}>Quay lại</Button>
            <Button
              variant="primary"
              onClick={() => {
                const s = confirmingEdit;
                setConfirmingEdit(null);
                if (s) void claimAndEdit(s);
              }}
            >
              Sửa mục
            </Button>
          </>
        }
      >
        <Alert tone="warning">Mục hiện tại đã có người làm.</Alert>
      </Dialog>
      {autoSubmitting && typeof document !== "undefined" && createPortal(
        <div role="alertdialog" aria-label="Đang nộp" className="fixed inset-0 z-[100] flex items-center justify-center bg-ink/45">
          <div className="max-w-[420px] rounded-xl bg-surface p-6 text-center">
            <p className="text-[16px] font-bold text-ink">Đang nộp…</p>
            <p className="mt-2 text-[13px] text-body">
              Đã hết hạn nộp{view.closesAt && ` lúc ${dateTime(view.closesAt)}`}. Hệ thống đang tự nộp bản mới nhất của các mục; trang bị khóa trong
              lúc nộp.
            </p>
          </div>
        </div>,
        document.body,
      )}
    </div>
  );
}
