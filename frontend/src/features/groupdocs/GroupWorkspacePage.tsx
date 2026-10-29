"use client";

import { useCallback, useEffect, useState } from "react";
import { formatDateTime } from "@/features/groups/format";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, ButtonLink, Dialog, Pill, Section, TableFrame, Td, Th, cx, type Tone } from "@/shared/ui";
import { useActivityTracker, type ActivityEventInput } from "@/shared/activity/useActivityTracker";
import { DocumentViewer } from "@/shared/document";
import { claimSection, downloadDocx, getWorkspace, releaseSection, sendActivity } from "./api";
import { AssignSectionDialog, SectionEditorDialog, SubmitGroupDialog } from "./SectionDialogs";
import type { Receipt, SectionView, StreamEvent, WorkspaceView } from "./types";
import { useGroupDocStream } from "./useGroupDocStream";
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
 */
export function GroupWorkspacePage({ documentId }: { documentId: string }) {
  const [view, setView] = useState<WorkspaceView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [tab, setTab] = useState<"work" | "submission">("work");
  const [editing, setEditing] = useState<SectionView | null>(null);
  const [assigning, setAssigning] = useState<SectionView | null>(null);
  const [releasing, setReleasing] = useState<SectionView | null>(null);
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
    [reload],
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
      setEditing(await claimSection(documentId, s.id));
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
  const claimable = (s: SectionView) => editable && me.member && s.status !== "CLAIMED";
  // Bấm vào mục: mục mình giữ thì soạn tiếp, mục trống thì nhận và soạn, mục người khác giữ thì chỉ cuộn tới nội dung.
  const openSection = (s: SectionView) => {
    if (editable && isMine(s)) setEditing(s);
    else if (claimable(s)) void claimAndEdit(s);
    else document.getElementById(`section-${s.id}`)?.scrollIntoView?.({ behavior: "smooth", block: "start" });
  };
  const sectionHint = (s: SectionView) =>
    editable && isMine(s) ? "Bấm để soạn tiếp" : claimable(s) ? "Bấm để nhận và soạn" : s.status === "CLAIMED" ? "Chỉ xem" : "Xem nội dung";

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center gap-3 rounded-xl border border-line bg-surface px-4 py-3">
        <ButtonLink href={me.teacher ? `/teaching/classes/${view.classId}` : `/classes/${view.classId}`} icon="arrow_back" size="sm">
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

      {tab === "work" ? (
        <div className="grid gap-4 lg:grid-cols-[320px_1fr]">
          <Section title={`Các mục (${done} / ${view.sections.length} Xong)`} icon="assignment">
            {me.member && editable && (
              <p className="text-xs text-muted">
                Bấm vào mục Mở hoặc Xong để nhận và soạn toàn trang; bạn chỉ sửa được mục mình đang giữ. Nội dung tự lưu mỗi
                10 giây.
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
                      <span className="text-xs text-muted">
                        {s.status === "CLAIMED" ? `Đang giữ: ${name(s.holderId)}` : s.lastAuthorId ? `Xong bởi ${name(s.lastAuthorId)}` : "Chưa ai nhận"}
                      </span>
                    </button>
                    {editable && (
                      <div className="flex flex-wrap gap-1.5">
                        {claimable(s) && (
                          <Button size="sm" onClick={() => void claimAndEdit(s)}>
                            Nhận mục
                          </Button>
                        )}
                        {mine && (
                          <Button size="sm" variant="primary" onClick={() => setEditing(s)}>
                            Làm
                          </Button>
                        )}
                        {me.leader && (
                          <Button size="sm" onClick={() => setAssigning(s)}>
                            Giao mục
                          </Button>
                        )}
                        {s.status === "CLAIMED" && (mine || me.leader || me.teacher) && (
                          <Button size="sm" variant="ghost" onClick={() => setReleasing(s)}>
                            Nhả mục
                          </Button>
                        )}
                      </div>
                    )}
                  </li>
                );
              })}
            </ul>
          </Section>
          <Section title="Tài liệu nhóm" icon="description">
            {view.sharedBlocks.blocks.length > 0 && (
              <div className="rounded-xl bg-shell p-3">
                <p className="mb-1 text-xs font-semibold text-muted">Hướng dẫn của giảng viên</p>
                <DocumentViewer document={view.sharedBlocks} />
              </div>
            )}
            {view.sections.map((s) => (
              <article key={s.id} id={`section-${s.id}`} aria-label={s.title} className="scroll-mt-4">
                <h3 className="mt-2 text-[14px] font-bold text-ink">{s.title}</h3>
                <DocumentViewer document={{ blocks: s.publishedBlocks ?? [] }} empty="Chưa có nội dung đã Xong." />
              </article>
            ))}
          </Section>
        </div>
      ) : (
        <GroupSubmissionTab view={view} />
      )}

      {editing && (
        <SectionEditorDialog
          documentId={view.documentId}
          section={editing}
          shared={view.sharedBlocks}
          diagramEnabled={view.diagramEnabled}
          onClose={() => setEditing(null)}
          onChanged={() => void reload()}
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
                  <Td>{view.members.find((m) => m.accountId === sec.lastAuthorId)?.name ?? "—"}</Td>
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
