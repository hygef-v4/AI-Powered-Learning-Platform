"use client";

import { useCallback, useEffect, useState } from "react";
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
  lockSection,
  releaseSection,
  sendActivity,
  takeOverSection,
  unlockSection,
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
 * Thành viên nhận mục trống; trưởng nhóm tiếp quản mục người khác đang giữ, chốt mục (ghi công mọi người từng làm) và
 * nhập từ file; ai cũng xem lịch sử mục (BR-U12-33, 35, 39, 46); thẻ Đánh giá chéo (BR-U12-47).
 */
export function GroupWorkspacePage({ documentId }: { documentId: string }) {
  const [view, setView] = useState<WorkspaceView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [tab, setTab] = useUrlState("tab", "work", ["work", "submission", "peer"]);
  const [editing, setEditing] = useState<SectionView | null>(null);
  const [assigning, setAssigning] = useState<SectionView | null>(null);
  const [releasing, setReleasing] = useState<SectionView | null>(null);
  const [taking, setTaking] = useState<SectionView | null>(null);
  const [history, setHistory] = useState<SectionView | null>(null);
  const [importing, setImporting] = useState(false);
  const [info, setInfo] = useState<string | null>(null);
  // Tên người vừa tiếp quản mục đang mở trong trình soạn; trình soạn chuyển sang chỉ xem.
  const [lostTo, setLostTo] = useState<string | null>(null);
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
      if (event.type === "section.taken" && editing && event.sectionId === editing.id && view && event.holderId !== view.viewerId) {
        setLostTo(view.members.find((m) => m.accountId === event.holderId)?.name ?? "");
      }
      void reload();
    },
    [reload, setTab, editing, view],
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

  const openEditor = (s: SectionView) => {
    setLostTo(null);
    setEditing(s);
  };

  const act = async (action: () => Promise<unknown>, fallback: string) => {
    setNotice(null);
    try {
      await action();
      await reload();
    } catch (e) {
      setNotice(e instanceof ApiError ? (e.problem.detail ?? fallback) : fallback);
    }
  };

  // Nhận mục rồi mở luôn trình soạn bằng bản mục server trả về (U12-08).
  const claimAndEdit = async (s: SectionView) => {
    setNotice(null);
    try {
      openEditor(await claimSection(documentId, s.id));
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
  const isMine = (s: SectionView) => s.status === "CLAIMED" && s.holderId === myId;
  const locked = (s: SectionView) => !!s.lockedBy;
  const claimable = (s: SectionView) => editable && me.member && s.status !== "CLAIMED" && !locked(s);
  // Chỉ trưởng nhóm tiếp quản mục người khác đang giữ; thành viên thường chỉ nhận mục trống (BR-U12-33).
  const takeable = (s: SectionView) => editable && me.leader && s.status === "CLAIMED" && s.holderId !== myId;
  const credit = (s: SectionView) => (s.contributorIds ?? []).map((id) => name(id)).join(", ");
  // Bấm vào mục: mục mình giữ thì soạn tiếp, mục trống thì nhận và soạn, trưởng nhóm bấm mục người khác giữ thì hỏi tiếp quản.
  const openSection = (s: SectionView) => {
    if (editable && isMine(s)) openEditor(s);
    else if (claimable(s)) void claimAndEdit(s);
    else if (takeable(s)) setTaking(s);
    else document.getElementById(`section-${s.id}`)?.scrollIntoView?.({ behavior: "smooth", block: "start" });
  };
  const sectionHint = (s: SectionView) =>
    editable && isMine(s)
      ? "Bấm để soạn tiếp"
      : claimable(s)
        ? "Bấm để nhận và soạn"
        : takeable(s)
          ? "Bấm để tiếp quản và soạn"
          : locked(s)
            ? "Đã chốt, chỉ xem"
            : s.status === "CLAIMED"
              ? "Chỉ xem"
              : "Xem nội dung";
  const takeOver = async (s: SectionView) => {
    setTaking(null);
    setNotice(null);
    try {
      openEditor(await takeOverSection(documentId, s.id));
      void reload();
    } catch (e) {
      setNotice(e instanceof ApiError ? (e.problem.detail ?? "Không tiếp quản được mục.") : "Không tiếp quản được mục.");
    }
  };

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

      {tab === "work" ? (
        <div className="grid gap-4 lg:grid-cols-[320px_1fr]">
          <Section title={`Các mục (${done} / ${view.sections.length} Xong)`} icon="assignment">
            {me.member && editable && (
              <p className="text-xs text-muted">
                Bấm vào mục trống để nhận và soạn toàn trang; mục người khác đã nhận thì chỉ xem.
                {me.leader ? " Bạn tiếp quản được mục người khác đang giữ; bản nháp của họ được giữ trong lịch sử." : ""}{" "}
                Mục đã chốt được ghi công cho mọi người từng làm và không ai sửa được. Nội dung tự lưu mỗi 10 giây.
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
                        {locked(s) && <Pill tone="primary">Đã chốt</Pill>}
                        <Pill tone={STATUS[s.status].tone}>{STATUS[s.status].text}</Pill>
                      </span>
                      <span className="text-xs text-muted">
                        {s.status === "CLAIMED" ? `Đang giữ: ${name(s.holderId)}` : s.lastAuthorId ? `Xong bởi ${name(s.lastAuthorId)}` : "Chưa ai nhận"}
                      </span>
                      {locked(s) && credit(s) && <span className="text-xs text-primary-ink">Ghi công: {credit(s)}</span>}
                    </button>
                    <div className="flex flex-wrap gap-1.5">
                      {editable && claimable(s) && (
                        <Button size="sm" onClick={() => void claimAndEdit(s)}>
                          Nhận mục
                        </Button>
                      )}
                      {editable && mine && (
                        <Button size="sm" variant="primary" onClick={() => openEditor(s)}>
                          Làm
                        </Button>
                      )}
                      {takeable(s) && (
                        <Button size="sm" onClick={() => setTaking(s)}>
                          Tiếp quản
                        </Button>
                      )}
                      {editable && me.leader && !locked(s) && (
                        <Button size="sm" onClick={() => setAssigning(s)}>
                          Giao mục
                        </Button>
                      )}
                      {editable && me.leader && s.status === "DONE" && !locked(s) && (
                        <Button size="sm" icon="lock" onClick={() => void act(() => lockSection(documentId, s.id), "Không chốt được mục.")}>
                          Chốt
                        </Button>
                      )}
                      {editable && me.leader && locked(s) && (
                        <Button size="sm" icon="lock_open" onClick={() => void act(() => unlockSection(documentId, s.id), "Không mở chốt được mục.")}>
                          Mở chốt
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
          lostTo={lostTo}
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
      <Dialog
        open={taking !== null}
        title={`Tiếp quản mục ${taking?.title ?? ""}?`}
        icon="swap_horiz"
        onClose={() => setTaking(null)}
        footer={
          <Button variant="primary" onClick={() => taking && void takeOver(taking)}>
            Tiếp quản
          </Button>
        }
      >
        <p>
          {name(taking?.holderId ?? null)} đang giữ mục này. Bạn sẽ làm tiếp từ bản nháp đã lưu gần nhất của họ; bản nháp đó được
          giữ trong lịch sử và họ vẫn được ghi công khi chốt mục. Phần họ gõ trong 10 giây gần nhất chưa tự lưu có thể không có
          trong bản nháp.
        </p>
      </Dialog>
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
      {autoSubmitting && (
        <div role="alertdialog" aria-label="Đang nộp" className="fixed inset-0 z-50 flex items-center justify-center bg-ink/45">
          <div className="max-w-[420px] rounded-xl bg-surface p-6 text-center">
            <p className="text-[16px] font-bold text-ink">Đang nộp…</p>
            <p className="mt-2 text-[13px] text-body">
              Đã hết hạn nộp{view.closesAt && ` lúc ${dateTime(view.closesAt)}`}. Hệ thống đang tự nộp bản mới nhất của các mục; trang bị khóa trong
              lúc nộp.
            </p>
          </div>
        </div>
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
                  <Td>
                    {sec.lockedBy && (sec.contributorIds ?? []).length > 0
                      ? (sec.contributorIds ?? []).map((id) => view.members.find((m) => m.accountId === id)?.name ?? "Thành viên đã rời nhóm").join(", ")
                      : (view.members.find((m) => m.accountId === sec.lastAuthorId)?.name ?? "—")}
                  </Td>
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
