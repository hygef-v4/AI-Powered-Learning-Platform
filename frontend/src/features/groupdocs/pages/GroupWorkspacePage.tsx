"use client";

import { useCallback, useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { useUrlState } from "@/shared/navigation/useUrlState";
import { formatDateTime } from "@/features/groups/model/format";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, ButtonLink, Dialog, Pill, Section, TableFrame, Td, Th, cx, type Tone } from "@/shared/ui";
import { useActivityTracker, type ActivityEventInput } from "@/shared/activity/useActivityTracker";
import { DocumentViewer } from "@/shared/document";
import {
  claimSection,
  downloadDocx,
  getWorkspace,
  groupDocImageUrl,
  lockAssignment,
  releaseSection,
  sendActivity,
  unlockAssignment,
} from "../api";
import { AssignSectionDialog, SectionEditorDialog, SubmitGroupDialog } from "../components/SectionDialogs";
import { GroupImportDialog, PeerReviewPanel, SectionHistoryDialog } from "../components/CollaborationPanels";
import type { Receipt, SectionView, StreamEvent, WorkspaceView } from "../model/types";
import { useGroupDocStream } from "../hooks/useGroupDocStream";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";

const STATUS: Record<SectionView["status"], { text: string; tone: Tone }> = {
  OPEN: { text: "Mở", tone: "neutral" },
  CLAIMED: { text: "Đang làm", tone: "info" },
  DONE: { text: "Xong", tone: "success" },
};

function dateTime(iso: string | null | undefined): string {
  return iso ? formatDateTime(iso) : "—";
}

/**
 * Màn #40 Assignment Workspace chế độ nhóm (UC 56): tài liệu chung, danh sách mục kèm người giữ và thao tác hợp lệ
 * cho từng người, nộp (trưởng nhóm), thẻ Bản nộp; realtime qua SSE, tự nộp thì khóa trang rồi chuyển sang Bản nộp.
 * Giai đoạn phân công (FB-U12-01): thành viên nhận mục trống hoặc trưởng nhóm giao, chưa soạn; trưởng nhóm chốt phân
 * công khi mọi mục có người phụ trách, từ đó mỗi người soạn mục của mình; mở chốt để phân lại. Nhập từ file (FB-U12-03),
 * lịch sử mục (FB-U12-02), thẻ Đánh giá chéo (FB-U12-04).
 */
export function GroupWorkspacePage({ documentId }: { documentId: string }) {
  const [view, setView] = useState<WorkspaceView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [tab, setTab] = useUrlState("tab", "work", ["work", "submission", "peer"]);
  const [editing, setEditing] = useState<SectionView | null>(null);
  const [assigning, setAssigning] = useState<SectionView | null>(null);
  const [releasing, setReleasing] = useState<SectionView | null>(null);
  const [history, setHistory] = useState<SectionView | null>(null);
  const [importing, setImporting] = useState(false);
  const [info, setInfo] = useState<string | null>(null);
  const [unlocking, setUnlocking] = useState(false);
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

  const send = useCallback(
    (events: ActivityEventInput[], options: { keepalive: boolean }) => sendActivity(documentId, events, options.keepalive),
    [documentId],
  );
  useActivityTracker({
    enabled: !!view && view.activityMonitoring && view.role.member && view.status === "IN_PROGRESS" && view.window !== "CLOSED",
    send,
  });

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

  // Chưa chốt phân công: nhận mục (làm người phụ trách). Đã chốt: người phụ trách bắt đầu soạn, mở luôn trình soạn.
  const claimAndEdit = async (s: SectionView) => {
    setNotice(null);
    try {
      const claimed = await claimSection(documentId, s.id);
      if (view?.assignmentLockedAt) openEditor(claimed);
      void reload();
    } catch (e) {
      setNotice(e instanceof ApiError ? (e.problem.detail ?? "Không nhận được mục.") : "Không nhận được mục.");
    }
  };

  if (error && !view) return <PageLoadError>{error}</PageLoadError>;
  if (!view) return <PageLoading label="Đang tải tài liệu nhóm…" />;

  const me = view.role;
  const editable = view.status === "IN_PROGRESS" && view.window !== "CLOSED";
  const done = view.sections.filter((s) => s.status === "DONE").length;
  const leaderName = view.members.find((m) => m.accountId === view.leaderId)?.name ?? "chưa có";
  const myId = view.viewerId;
  const name = (id: string | null) =>
    id === null ? "Chưa ai nhận" : id === myId ? "Bạn" : (view.members.find((m) => m.accountId === id)?.name ?? "Thành viên");
  const working = !!view.assignmentLockedAt;
  const unassigned = view.sections.filter((s) => !s.assigneeId).length;
  const isMine = (s: SectionView) => s.status === "CLAIMED" && s.holderId === myId;
  const responsible = (s: SectionView) => s.assigneeId === myId;
  // Chưa chốt: nhận mục chưa có ai. Đã chốt: người phụ trách bắt đầu soạn mục của mình.
  const claimable = (s: SectionView) => editable && me.member && (working ? responsible(s) && !isMine(s) : !s.assigneeId);
  const openSection = (s: SectionView) => {
    if (editable && isMine(s)) openEditor(s);
    else if (claimable(s)) void claimAndEdit(s);
    else document.getElementById(`section-${s.id}`)?.scrollIntoView?.({ behavior: "smooth", block: "start" });
  };
  const sectionHint = (s: SectionView) =>
    editable && isMine(s)
      ? "Bấm để soạn tiếp"
      : claimable(s)
        ? working
          ? "Bấm để soạn"
          : "Bấm để nhận mục"
        : "Xem nội dung";
  const assigneeLine = (s: SectionView) =>
    s.status === "CLAIMED" && working
      ? `Đang soạn: ${name(s.holderId)}`
      : s.assigneeId
        ? `Phụ trách: ${name(s.assigneeId)}`
        : "Chưa có người nhận";

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center gap-3 rounded-xl border border-line bg-surface px-4 py-3">
        <ButtonLink back href={me.teacher ? `/teaching/classes/${view.classId}` : `/classes/${view.classId}`} icon="arrow_back" size="sm">
          Thoát
        </ButtonLink>
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
          <Button icon="folder_open" onClick={() => setImporting(true)}>
            Nhập từ file
          </Button>
        )}
        {me.leader && editable && (
          <Button variant="primary" icon="upload_file" onClick={() => setSubmitting(true)}>
            {view.lastSubmission ? "Nộp lại" : "Nộp tài liệu"}
          </Button>
        )}
        {me.member && !me.leader && editable && <span className="text-xs text-muted">Trưởng nhóm nộp bài</span>}
      </div>

      <div role="tablist" aria-label="Tài liệu nhóm" className="flex gap-1 border-b border-line">
        {([["work", "Tài liệu"], ["submission", "Bản nộp"], ["peer", "Đánh giá chéo"]] as const).map(([key, label]) => (
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
      {info && (
        <Alert tone="success" role="status">
          {info}
        </Alert>
      )}
      {!editable && view.status === "IN_PROGRESS" && <Alert tone="info">Đã hết hạn nộp; tài liệu chỉ còn xem.</Alert>}
      {editable && tab === "work" && (
        <Alert
          tone={working ? "success" : "warning"}
          title={working ? "Đã chốt phân công" : "Đang phân công"}
          action={
            me.leader &&
            (working ? (
              <Button size="sm" icon="lock_open" onClick={() => setUnlocking(true)}>
                Mở chốt
              </Button>
            ) : (
              <Button
                size="sm"
                variant="primary"
                icon="lock"
                disabled={unassigned > 0}
                title={unassigned > 0 ? `Còn ${unassigned} mục chưa có người phụ trách` : undefined}
                onClick={() => void act(() => lockAssignment(documentId), "Không chốt được phân công.")}
              >
                Chốt phân công
              </Button>
            ))
          }
        >
          {working
            ? `Trưởng nhóm chốt lúc ${dateTime(view.assignmentLockedAt)}. Mỗi người soạn mục mình phụ trách.`
            : `Nhận mục hoặc chờ trưởng nhóm giao; chưa soạn được cho tới khi trưởng nhóm chốt phân công. ${view.sections.length - unassigned}/${view.sections.length} mục đã có người phụ trách.`}
        </Alert>
      )}

      {tab === "work" ? (
        <div className="grid gap-4 lg:grid-cols-[320px_1fr]">
          <Section title={`Các mục (${done} / ${view.sections.length} Xong)`} icon="assignment">
            {me.member && editable && (
              <p className="text-xs text-muted">
                {working
                  ? "Bấm vào mục bạn phụ trách để soạn toàn trang; mục của người khác chỉ xem. Nội dung tự lưu mỗi 10 giây."
                  : "Bấm vào mục chưa có người nhận để nhận; mục đã có người nhận thì người khác không nhận được."}
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
                      <span className="text-xs text-muted">{assigneeLine(s)}</span>
                    </button>
                    <div className="flex flex-wrap gap-1.5">
                      {claimable(s) && (
                        <Button size="sm" variant={working ? "primary" : "secondary"} onClick={() => void claimAndEdit(s)}>
                          {working ? "Làm" : "Nhận mục"}
                        </Button>
                      )}
                      {editable && mine && (
                        <Button size="sm" variant="primary" onClick={() => openEditor(s)}>
                          Làm
                        </Button>
                      )}
                      {editable && !working && me.leader && (
                        <Button size="sm" onClick={() => setAssigning(s)}>
                          Giao mục
                        </Button>
                      )}
                      {editable && !working && s.assigneeId && (responsible(s) || me.leader || me.teacher) && (
                        <Button size="sm" variant="ghost" onClick={() => void act(() => releaseSection(view.documentId, s.id), "Không bỏ nhận được mục.")}>
                          {responsible(s) ? "Bỏ nhận" : "Bỏ giao"}
                        </Button>
                      )}
                      {editable && working && s.status === "CLAIMED" && (mine || me.leader || me.teacher) && (
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
          <Section title="Tài liệu nhóm" icon="description">
            {view.sharedBlocks.blocks.length > 0 && (
              <div className="rounded-xl bg-shell p-3">
                <p className="mb-1 text-xs font-semibold text-muted">Hướng dẫn của giảng viên</p>
                <DocumentViewer document={view.sharedBlocks} imageUrl={groupDocImageUrl(documentId)} />
              </div>
            )}
            {view.sections.map((s) => (
              <article key={s.id} id={`section-${s.id}`} aria-label={s.title} className="scroll-mt-4">
                <h3 className="mt-2 text-[14px] font-bold text-ink">{s.title}</h3>
                <DocumentViewer document={{ blocks: s.publishedBlocks ?? [] }} imageUrl={groupDocImageUrl(documentId)} empty="Chưa có nội dung đã Xong." />
              </article>
            ))}
          </Section>
        </div>
      ) : tab === "peer" ? (
        <PeerReviewPanel view={view} />
      ) : (
        <GroupSubmissionTab view={view} />
      )}

      {editing && (
        <SectionEditorDialog
          documentId={view.documentId}
          section={editing}
          shared={view.sharedBlocks}
          diagramEnabled={view.diagramEnabled}
          members={view.members}
          viewerId={view.viewerId}
          onClose={() => setEditing(null)}
          onChanged={() => void reload()}
        />
      )}
      {history && <SectionHistoryDialog view={view} section={history} onClose={() => setHistory(null)} />}
      {importing && (
        <GroupImportDialog
          view={view}
          onClose={() => setImporting(false)}
          onImported={(text) => {
            setInfo(text);
            void reload();
          }}
        />
      )}
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
        open={unlocking}
        title="Mở chốt phân công?"
        icon="lock_open"
        tone="warning"
        onClose={() => setUnlocking(false)}
        footer={
          <Button
            variant="primary"
            onClick={() => {
              setUnlocking(false);
              void act(() => unlockAssignment(documentId), "Không mở chốt được.");
            }}
          >
            Mở chốt
          </Button>
        }
      >
        <p>Cả nhóm tạm dừng soạn cho tới khi chốt lại. Nội dung đã Xong và bản nháp đã lưu giữ nguyên; bạn đổi được người phụ trách các mục.</p>
      </Dialog>
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
  const submitter = view.members.find((m) => m.accountId === s?.submittedBy)?.name;
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
                <Th>Tác giả</Th>
                <Th>Trạng thái hiện tại</Th>
              </tr>
            </thead>
            <tbody>
              {view.sections.map((sec) => (
                <tr key={sec.id}>
                  <Td>{sec.title}</Td>
                  <Td>{view.members.find((m) => m.accountId === (sec.lastAuthorId ?? sec.assigneeId))?.name ?? "—"}</Td>
                  <Td>{STATUS[sec.status].text}</Td>
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
