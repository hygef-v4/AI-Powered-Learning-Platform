"use client";
import { studentAssignmentFeedback } from "@/features/assignmentwork/slots";

import { useCallback, useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { useUrlState } from "@/shared/navigation/useUrlState";
import { formatDateTime } from "@/features/groups/model/format";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Dialog, Pill, Section, TableFrame, Td, Th, cx, type Tone } from "@/shared/ui";
import { useActivityTracker, type ActivityEventInput } from "@/shared/activity/useActivityTracker";
import { DocumentViewer } from "@/shared/document";
import { DOCUMENT_ENTRY, outlineOf, publishOutline } from "@/shared/document/outline";
import { claimSection, downloadDocx, getWorkspace, groupDocImageUrl, releaseSection, sendActivity } from "../api";
import { AssignSectionDialog, SectionEditorDialog, SubmitGroupDialog } from "../components/SectionDialogs";
import { SectionHistoryDialog } from "../components/SectionHistoryDialog";
import type { Receipt, SectionView, StreamEvent, WorkspaceView } from "../model/types";
import { useGroupDocStream } from "../hooks/useGroupDocStream";
import { useClock } from "../hooks/useClock";
import { describeDeadline, formatAgo } from "../model/deadline";
import { findSectionForHeading } from "../model/outline";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";

const STATUS: Record<SectionView["status"], { text: string; tone: Tone }> = {
  OPEN: { text: "Mở", tone: "neutral" },
  CLAIMED: { text: "Đang làm", tone: "info" },
  DONE: { text: "Xong", tone: "success" },
};

// Có mục người khác đang giữ thì tải lại định kỳ: lưu nháp không đẩy SSE (BR-U12-37) nên "lưu lần cuối" cần làm mới.
const HOLDER_REFRESH_MS = 60_000;

function dateTime(iso: string | null | undefined): string {
  return iso ? formatDateTime(iso) : "—";
}

/**
 * Màn #40 Assignment Workspace chế độ nhóm (UC 56): tài liệu chung, danh sách mục kèm người giữ và thao tác hợp lệ
 * cho từng người, nộp (trưởng nhóm), thẻ Bản nộp; realtime qua SSE, tự nộp thì khóa trang rồi chuyển sang Bản nộp.
 * Mục theo BR-U12-33, 36: thành viên nhận mục Mở hoặc Xong là soạn ngay; trưởng nhóm giao mục; người giữ, trưởng nhóm
 * hoặc giảng viên nhả mục. Lịch sử mục chỉ xem (BR-U12-35).
 */
export function GroupWorkspacePage({ documentId }: { documentId: string }) {
  const [view, setView] = useState<WorkspaceView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [tab, setTab] = useUrlState("tab", "work", ["work", "submission"]);
  const [editing, setEditing] = useState<SectionView | null>(null);
  const [assigning, setAssigning] = useState<SectionView | null>(null);
  const [releasing, setReleasing] = useState<SectionView | null>(null);
  const [confirmingEdit, setConfirmingEdit] = useState<SectionView | null>(null);
  const [history, setHistory] = useState<SectionView | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [autoSubmitting, setAutoSubmitting] = useState(false);

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
        void reload().then(() => {
          setAutoSubmitting(false);
          setTab("submission");
        });
        return;
      }
      void reload();
    },
    [reload, setTab],
  );
  useGroupDocStream(view ? documentId : null, onEvent, () => void reload());
  const now = useClock();

  const watchHolders =
    !!view &&
    view.status === "IN_PROGRESS" &&
    view.window !== "CLOSED" &&
    view.sections.some((s) => s.status === "CLAIMED" && s.holderId !== view.viewerId);
  useEffect(() => {
    if (!watchHolders) return;
    const timer = window.setInterval(() => {
      if (document.visibilityState === "visible") void reload();
    }, HOLDER_REFRESH_MS);
    return () => window.clearInterval(timer);
  }, [watchHolders, reload]);

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

  // Bấm một dòng trên sidebar: về thẻ Tài liệu và cuộn tới mục chứa heading đó.
  useEffect(() => {
    if (!view) return;
    const handleShowPart = (event: Event) => {
      const headingId = (event as CustomEvent<{ headingId: string }>).detail?.headingId;
      setTab("work");
      const target =
        headingId === DOCUMENT_ENTRY
          ? "group-doc-content"
          : (() => {
              const found = findSectionForHeading(view.sharedBlocks.blocks, view.sections, headingId);
              return found ? `section-${found.id}` : null;
            })();
      if (!target) return;
      // Đợi thẻ Tài liệu vẽ xong nếu vừa chuyển từ thẻ Bản nộp.
      window.setTimeout(() => document.getElementById(target)?.scrollIntoView?.({ behavior: "smooth", block: "start" }), 0);
    };
    window.addEventListener("document:show-part", handleShowPart);
    return () => window.removeEventListener("document:show-part", handleShowPart);
  }, [view, setTab]);

  // Sidebar sáng theo mục đang hiện trên màn hình khi cuộn.
  const sectionKey = view?.sections.map((s) => s.id).join(",") ?? "";
  useEffect(() => {
    if (!sectionKey || tab !== "work" || typeof IntersectionObserver === "undefined") return;
    const observer = new IntersectionObserver(
      (records) => {
        const shown = records.filter((r) => r.isIntersecting).sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)[0];
        const headingId = (shown?.target as HTMLElement | undefined)?.dataset.partId;
        if (headingId) window.dispatchEvent(new CustomEvent("document:current-part", { detail: { headingId } }));
      },
      { rootMargin: "0px 0px -60% 0px" },
    );
    sectionKey.split(",").forEach((id) => {
      const element = document.getElementById(`section-${id}`);
      if (element) observer.observe(element);
    });
    return () => observer.disconnect();
  }, [sectionKey, tab]);

  const openEditor = (s: SectionView) => setEditing(s);

  const act = async (action: () => Promise<unknown>, fallback: string) => {
    setNotice(null);
    try {
      await action();
      await reload();
    } catch (e) {
      setNotice(e instanceof ApiError ? (e.problem.detail ?? fallback) : fallback);
    }
  };

  // Nhận mục Mở hoặc Xong là soạn ngay (BR-U12-33): mở luôn trình soạn.
  const claimAndEdit = async (s: SectionView) => {
    setNotice(null);
    try {
      const claimed = await claimSection(documentId, s.id);
      openEditor(claimed);
      void reload();
    } catch (e) {
      setNotice(e instanceof ApiError ? (e.problem.detail ?? "Không nhận được mục.") : "Không nhận được mục.");
    }
  };

  if (error && !view) return <PageLoadError>{error}</PageLoadError>;
  if (!view) return <PageLoading label="Đang tải tài liệu nhóm…" />;

  const me = view.role;
  const editable = view.status === "IN_PROGRESS" && view.window !== "CLOSED";

  // Mất mục đang soạn (người khác nhả hoặc giao lại, BR-U12-36) hoặc tài liệu đã khóa (hết hạn, tự nộp): đóng trình
  // soạn vì bản nháp sẽ bị từ chối. Bỏ qua bản xem cũ hơn mục đang soạn (vừa nhận, chưa tải lại).
  if (editing) {
    const current = view.sections.find((s) => s.id === editing.id);
    const held = current?.status === "CLAIMED" && current.holderId === view.viewerId;
    const lost = !current || (current.version >= editing.version && !held);
    if (!editable || lost) {
      setEditing(null);
      setNotice(
        !editable
          ? `Đã hết hạn nộp nên trình soạn mục “${editing.title}” đã đóng.`
          : `Bạn không còn giữ mục “${editing.title}” (đã được nhả hoặc giao cho người khác); phần chưa lưu không được giữ.`,
      );
    }
  }
  const done = view.sections.filter((s) => s.status === "DONE").length;
  const deadline = describeDeadline(view, now);
  const leaderName = view.members.find((m) => m.accountId === view.leaderId)?.name ?? "chưa có";
  const myId = view.viewerId;
  const name = (id: string | null) =>
    id === null ? "Chưa ai nhận" : id === myId ? "Bạn" : (view.members.find((m) => m.accountId === id)?.name ?? "Thành viên");
  const isMine = (s: SectionView) => s.status === "CLAIMED" && s.holderId === myId;
  const claimable = (s: SectionView) => editable && me.member && s.status !== "CLAIMED";
  // Mục Xong do người khác viết: hỏi lại trước khi nhận, vì người nhận khóa mục và thành người viết khi bấm Xong.
  const handleClaim = (s: SectionView) => {
    if (s.status === "DONE" && s.lastAuthorId && s.lastAuthorId !== myId) setConfirmingEdit(s);
    else void claimAndEdit(s);
  };
  const openSection = (s: SectionView) => {
    if (editable && isMine(s)) openEditor(s);
    else if (claimable(s)) handleClaim(s);
    else document.getElementById(`section-${s.id}`)?.scrollIntoView?.({ behavior: "smooth", block: "start" });
  };
  const sectionHint = (s: SectionView) =>
    editable && isMine(s) ? "Bấm để soạn tiếp" : claimable(s) ? "Bấm để nhận và soạn" : "Xem nội dung";
  const holderActivity = (s: SectionView) =>
    s.holderSavedAt
      ? ` · lưu lần cuối ${formatAgo(s.holderSavedAt, now)}`
      : s.claimedAt
        ? ` · nhận ${formatAgo(s.claimedAt, now)}, chưa lưu`
        : "";
  const holderLine = (s: SectionView) =>
    s.status === "CLAIMED"
      ? `Đang soạn: ${name(s.holderId)}${holderActivity(s)}`
      : s.status === "DONE"
        ? `Viết bởi: ${s.lastAuthorId ? name(s.lastAuthorId) : "—"}`
        : "Chưa ai nhận";

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center gap-3 rounded-xl border border-line bg-surface px-4 py-3">
        <div className="min-w-0">
          <h1 className="text-[17px] font-bold text-ink">
            {view.assignmentTitle ?? "Bài nhóm"} · {view.groupName}
          </h1>
          <p className="text-xs text-muted">
            {me.teacher ? "Giảng viên · chỉ xem" : me.leader ? "Bạn là trưởng nhóm" : "Bạn là thành viên"} · trưởng nhóm {leaderName}
            {view.closesAt && ` · hạn ${dateTime(view.closesAt)}`}
          </p>
        </div>
        <Pill tone={view.lastSubmission ? "success" : "info"} className="ml-auto">
          {view.status === "CLOSED" ? "Đã đóng" : view.lastSubmission ? "Đã nộp" : "Đang làm"}
        </Pill>
        {me.leader && editable && (
          <Button variant="primary" icon="upload_file" onClick={() => setSubmitting(true)}>
            {view.lastSubmission ? "Nộp lại" : "Nộp tài liệu"}
          </Button>
        )}
        {me.member && !me.leader && editable && <span className="text-xs text-muted">Trưởng nhóm nộp bài</span>}
      </div>

      <div role="tablist" aria-label="Tài liệu nhóm" className="flex gap-1 border-b border-line">
        {([["work", "Tài liệu"], ["submission", "Bản nộp"]] as const).map(([key, label]) => (
          <button
            key={key}
            role="tab"
            type="button"
            aria-selected={tab === key}
            onClick={() => setTab(key)}
            className={cx("-mb-px border-b-2 px-3.5 py-2 text-[13px] font-semibold", tab === key ? "border-primary text-primary-ink" : "border-transparent text-muted")}
          >
            {label}
          </button>
        ))}
      </div>

      {notice && (
        <Alert tone="danger" role="alert">
          {notice}
        </Alert>
      )}
      {!editable && view.status === "IN_PROGRESS" && <Alert tone="info">Đã hết hạn nộp; tài liệu chỉ còn xem.</Alert>}
      {deadline && !me.teacher && (
        <Alert tone={deadline.tone} title={deadline.title}>
          <ul className="list-disc pl-4">
            {deadline.details.map((line) => (
              <li key={line}>{line}</li>
            ))}
          </ul>
        </Alert>
      )}

      {tab === "work" ? (
        <div className="grid gap-4 lg:grid-cols-[320px_1fr]">
          <Section title={`Các mục (${done} / ${view.sections.length} Xong)`} icon="assignment">
            {me.member && editable && (
              <p className="text-xs text-muted">
                Bấm vào mục để nhận và soạn toàn trang; mục người khác đang soạn chỉ xem. Nội dung tự lưu mỗi 10 giây, bấm
                Xong để cả nhóm thấy.
              </p>
            )}
            <ul className="flex flex-col gap-2">
              {view.sections.map((s) => {
                const mine = isMine(s);
                return (
                  <li key={s.id} className="flex flex-col gap-1.5 rounded-xl border border-line p-3">
                    <button
                      type="button"
                      onClick={() => openSection(s)}
                      title={sectionHint(s)}
                      aria-label={`${s.title} · ${STATUS[s.status].text} · ${sectionHint(s)}`}
                      className={cx(
                        "-m-1 flex flex-col gap-1.5 rounded-lg p-1 text-left",
                        (mine && editable) || claimable(s) ? "cursor-pointer hover:bg-primary-soft" : "hover:bg-shell",
                      )}
                    >
                      <span className="flex w-full items-center gap-2">
                        <span className="min-w-0 flex-1 text-[13px] font-semibold text-ink">{s.title}</span>
                        <Pill tone={STATUS[s.status].tone}>{STATUS[s.status].text}</Pill>
                      </span>
                      <span className="text-xs text-muted">{holderLine(s)}</span>
                    </button>
                    <div className="flex flex-wrap gap-1.5">
                      {claimable(s) && (
                        <Button size="sm" variant="primary" onClick={() => handleClaim(s)}>
                          {s.status === "DONE" ? "Sửa mục" : "Nhận mục"}
                        </Button>
                      )}
                      {editable && mine && (
                        <Button size="sm" variant="primary" onClick={() => openEditor(s)}>
                          Soạn tiếp
                        </Button>
                      )}
                      {editable && me.leader && (
                        <Button size="sm" onClick={() => setAssigning(s)}>
                          Giao mục
                        </Button>
                      )}
                      {editable && s.status === "CLAIMED" && (mine || me.leader || me.teacher) && (
                        <Button size="sm" variant="ghost" onClick={() => setReleasing(s)}>
                          Nhả mục
                        </Button>
                      )}
                      <Button size="sm" variant="ghost" icon="history" onClick={() => setHistory(s)}>
                        Lịch sử
                      </Button>
                    </div>
                  </li>
                );
              })}
            </ul>
          </Section>
          <div id="group-doc-content" className="min-w-0 scroll-mt-4">
          <Section title="Tài liệu nhóm" icon="description">
            {view.sharedBlocks.blocks.length > 0 && (
              <div className="rounded-xl bg-shell p-3">
                <p className="mb-1 text-xs font-semibold text-muted">Hướng dẫn của giảng viên</p>
                <DocumentViewer document={view.sharedBlocks} imageUrl={groupDocImageUrl(documentId)} />
              </div>
            )}
            {view.sections.map((s) => (
              <article key={s.id} id={`section-${s.id}`} data-part-id={s.partId} aria-label={s.title} className="scroll-mt-4">
                <h3 className="mt-2 text-[14px] font-bold text-ink">{s.title}</h3>
                <DocumentViewer document={{ blocks: s.publishedBlocks ?? [] }} imageUrl={groupDocImageUrl(documentId)} empty="Chưa có nội dung đã Xong." />
              </article>
            ))}
          </Section>
          </div>
        </div>
      ) : (
        <>
          <GroupSubmissionTab view={view} />
          {view.role.member && studentAssignmentFeedback.entries().map(({ key, component: Feedback }) =>
            <Feedback key={key} classId={view.classId} assignmentId={view.assignmentId} />)}
        </>
      )}

      {editing && (
        <SectionEditorDialog
          documentId={view.documentId}
          section={editing}
          shared={view.sharedBlocks}
          diagramEnabled={view.diagramEnabled}
          deadlineNotice={deadline?.title ?? null}
          onClose={() => setEditing(null)}
          onChanged={() => void reload()}
        />
      )}
      {history && <SectionHistoryDialog view={view} section={history} onClose={() => setHistory(null)} />}
      {assigning && <AssignSectionDialog view={view} section={assigning} onClose={() => setAssigning(null)} onChanged={() => void reload()} />}
      {submitting && (
        <SubmitGroupDialog
          view={view}
          onClose={() => setSubmitting(false)}
          onSubmitted={(receipt: Receipt) => {
            setSubmitting(false);
            setNotice(null);
            void reload().then(() => setTab("submission"));
            void receipt;
          }}
        />
      )}
      <Dialog
        open={releasing !== null}
        title={`Nhả mục ${releasing?.title ?? ""}?`}
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
                if (s) void act(() => releaseSection(view.documentId, s.id), "Không nhả được mục.");
              }}
            >
              Nhả mục
            </Button>
          </>
        }
      >
        <p>Bản nháp chưa Xong sẽ bị bỏ. Mục về Xong nếu đã có nội dung đã Xong, không thì về Mở.</p>
      </Dialog>
      <Dialog
        open={confirmingEdit !== null}
        title={`Sửa mục ${confirmingEdit?.title ?? ""}?`}
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
        <p>
          Mục này do {name(confirmingEdit?.lastAuthorId ?? null)} viết. Bạn sẽ giữ mục, người khác không sửa được cho tới khi
          bạn bấm Xong hoặc nhả mục; bấm Xong thì bạn thành người viết mục.
        </p>
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

/** Thẻ Bản nộp (s18d, BR-U12-44): bản nộp cuối, biên nhận, tác giả từng mục, tải DOCX. */
function GroupSubmissionTab({ view }: { view: WorkspaceView }) {
  const [error, setError] = useState<string | null>(null);
  const s = view.lastSubmission;
  const download = async (submitted: boolean) => {
    try {
      const blob = await downloadDocx(view.documentId, submitted);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${view.assignmentTitle ?? "bai-nhom"} - ${view.groupName}.docx`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      setError("Không tải được DOCX.");
    }
  };
  const memberName = (id: string | null) => view.members.find((m) => m.accountId === id)?.name ?? "—";
  const submitter = view.members.find((m) => m.accountId === s?.submittedBy)?.name;
  // Tác giả lấy theo bản nộp, không theo mục hiện tại: mục sửa sau khi nộp không đổi người viết bản nộp.
  const rows = s?.authors?.length
    ? s.authors.map((a) => ({
        key: a.partId,
        title: a.title,
        author: a.done ? memberName(a.accountId) : "Chưa Xong, không vào bản nộp",
        current: view.sections.find((sec) => sec.partId === a.partId)?.status,
      }))
    : view.sections.map((sec) => ({ key: sec.id, title: sec.title, author: memberName(sec.lastAuthorId), current: sec.status }));
  return (
    <Section title="Bản nộp" icon="receipt_long">
      {!s ? (
        <p className="text-[13px] text-muted">Nhóm chưa nộp bản nào.</p>
      ) : (
        <>
          <p className="text-[13px] text-body">
            <Pill tone="success">Đã nộp</Pill>{" "}
            {s.mode === "MANUAL" ? `Trưởng nhóm ${submitter ?? ""} nộp` : s.mode === "AUTO_RETIRED" ? "Tự nộp khi bài ngưng giao" : "Tự nộp khi hết hạn"} lúc{" "}
            {dateTime(s.submittedAt)}
            {s.late && " · trễ hạn"}
          </p>
          <p className="font-mono text-[11px] break-all text-muted">Mã biên nhận: {s.receiptHash}</p>
          <TableFrame className="[&_table]:min-w-[520px] [&_table]:table-fixed">
            <colgroup><col /><col className="w-[30%]" /><col className="w-[170px]" /></colgroup><thead>
              <tr>
                <Th>Mục</Th>
                <Th>Tác giả trong bản nộp</Th>
                <Th>Trạng thái hiện tại</Th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => (
                <tr key={row.key}>
                  <Td>{row.title}</Td>
                  <Td>{row.author}</Td>
                  <Td>{row.current ? STATUS[row.current].text : "—"}</Td>
                </tr>
              ))}
            </tbody>
          </TableFrame>
        </>
      )}
      <div className="flex flex-wrap gap-2">
        {s && (
          <Button size="sm" icon="download" onClick={() => void download(true)}>
            Tải DOCX bản nộp
          </Button>
        )}
        {view.role.member && (
          <Button size="sm" icon="download" onClick={() => void download(false)}>
            Tải DOCX bản hiện tại
          </Button>
        )}
      </div>
      {view.status === "IN_PROGRESS" && s && view.closesAt && (
        <p className="text-xs text-muted">Nộp lại được trước hạn {dateTime(view.closesAt)}.</p>
      )}
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
    </Section>
  );
}
