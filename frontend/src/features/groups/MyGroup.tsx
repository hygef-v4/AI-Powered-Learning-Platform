"use client";

import { useCallback, useEffect, useId, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, ButtonLink, Dialog, Field, PageHeader, Pill, Section, Select, TableFrame, Td, Th, type Tone } from "@/shared/ui";
import { cancelLeaderRequest, getMyGroup, submitLeaderRequest } from "./api";
import { formatDate, formatShort } from "./format";
import type { MyGroupView, RequestStatus } from "./types";

const REQUEST_STATUS: Record<RequestStatus, { text: string; tone: Tone }> = {
  PENDING: { text: "Chờ duyệt", tone: "warning" },
  APPROVED: { text: "Đã duyệt", tone: "success" },
  REJECTED: { text: "Từ chối", tone: "danger" },
  CANCELLED: { text: "Đã hủy", tone: "neutral" },
};

function useMyGroup(classId: string) {
  const [view, setView] = useState<MyGroupView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const reload = useCallback(
    () => getMyGroup(classId).then(setView, () => setError("Không tải được nhóm của bạn.")),
    [classId],
  );
  useEffect(() => {
    let active = true;
    getMyGroup(classId).then(
      (loaded) => {
        if (active) setView(loaded);
      },
      () => {
        if (active) setError("Không tải được nhóm của bạn.");
      },
    );
    return () => {
      active = false;
    };
  }, [classId]);
  return { view, error, reload };
}

/** Thẻ Nhóm của tôi trên Student Class Detail (s02). */
export function MyGroupCard({ classId }: { classId: string }) {
  const { view } = useMyGroup(classId);
  if (!view) return null;
  const leader = view.members.find((m) => m.leader);
  return (
    <Section
      title="Nhóm của tôi"
      icon="groups"
      tone="accent"
      actions={view.groupId ? <ButtonLink href={`/classes/${classId}/group`} size="sm">Mở Nhóm của tôi</ButtonLink> : undefined}
    >
      {view.groupId ? (
        <p className="text-[13px] text-body">
          <span className="font-semibold text-ink">{view.name}</span> · {view.members.length} thành viên · trưởng nhóm{" "}
          {leader?.name ?? "chưa có"}
        </p>
      ) : (
        <p className="text-[13px] text-muted">Bạn chưa có nhóm trong lớp này.</p>
      )}
    </Section>
  );
}

/** Trang Nhóm của tôi (s03, UC 54): thành viên, yêu cầu đổi trưởng nhóm gần nhất của mình, bài nhóm. */
export function MyGroupPage({ classId }: { classId: string }) {
  const { view, error, reload } = useMyGroup(classId);
  const [requesting, setRequesting] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  if (error && !view) {
    return (
      <Alert tone="danger" role="alert">
        {error}
      </Alert>
    );
  }
  if (!view) return <p className="text-[13px] text-muted">Đang tải...</p>;
  const latest = view.myRequests[0];
  const before = view.myRequests[1];
  const open = view.classStatus === "OPEN";

  const cancel = async (id: string) => {
    try {
      await cancelLeaderRequest(id);
      await reload();
    } catch (e) {
      setNotice(e instanceof ApiError ? (e.problem.detail ?? "Không hủy được yêu cầu") : "Không hủy được yêu cầu");
    }
  };

  return (
    <div className="flex flex-col gap-4">
      <PageHeader
        title={view.name ?? "Nhóm của tôi"}
        backHref={`/classes/${classId}`}
        breadcrumb={[{ label: "Lớp của tôi", href: "/classes" }, { label: "Lớp", href: `/classes/${classId}` }, { label: "Nhóm của tôi" }]}
        actions={
          view.groupId && open ? (
            <Button icon="swap_horiz" onClick={() => setRequesting(true)} disabled={view.pendingRequest}>
              Yêu cầu đổi trưởng nhóm
            </Button>
          ) : undefined
        }
      />
      {!view.groupId && <Alert tone="info">Bạn chưa có nhóm trong lớp này. Giảng viên sẽ xếp nhóm.</Alert>}
      {notice && (
        <Alert tone="danger" role="alert">
          {notice}
        </Alert>
      )}
      {view.groupId && (
        <>
          <Section title={`Thành viên (${view.members.length})`} description="Nhóm cố định của lớp" icon="group">
            <ul className="flex flex-col gap-1.5 text-[13px]">
              {view.members.map((m) => (
                <li key={m.accountId} className="flex items-center gap-2">
                  <span className="font-semibold text-ink">{m.name ?? m.email}</span>
                  {m.email && <span className="text-[11px] text-muted">{m.email}</span>}
                  {m.leader && <Pill tone="warning">Trưởng nhóm</Pill>}
                </li>
              ))}
            </ul>
            {view.pendingRequest && !latest?.status.includes("PENDING") && (
              <p className="text-xs text-muted">Nhóm đang có một yêu cầu đổi trưởng nhóm chờ duyệt.</p>
            )}
          </Section>
          {latest && (
            <Section title="Yêu cầu đổi trưởng nhóm gần nhất" icon="history">
              <div className="flex flex-wrap items-center gap-2 text-[13px]">
                <Pill tone={REQUEST_STATUS[latest.status].tone}>{REQUEST_STATUS[latest.status].text}</Pill>
                <span className="text-muted">gửi lúc {formatShort(latest.createdAt)}</span>
                {latest.status === "PENDING" && (
                  <Button size="sm" onClick={() => void cancel(latest.id)}>
                    Hủy yêu cầu
                  </Button>
                )}
              </div>
              <p className="text-[13px] text-body">Lý do: {latest.reason}</p>
              {latest.status === "REJECTED" && latest.decisionReason && (
                <p className="text-[13px] text-danger">Lý do từ chối: {latest.decisionReason}</p>
              )}
              {before && (
                <p className="text-xs text-muted">
                  Lần trước: {REQUEST_STATUS[before.status].text} · {formatShort(before.createdAt)}
                </p>
              )}
            </Section>
          )}
          <Section title="Bài nhóm" icon="assignment">
            {view.works.length === 0 ? (
              <p className="text-[13px] text-muted">Chưa có bài nhóm nào được mở.</p>
            ) : (
              <TableFrame>
                <thead>
                  <tr>
                    <Th>Bài nhóm</Th>
                    <Th>Hạn nộp</Th>
                    <Th>Trạng thái</Th>
                    <Th className="text-right">Thao tác</Th>
                  </tr>
                </thead>
                <tbody>
                  {view.works.map((w) => (
                    <tr key={w.assignmentId}>
                      <Td className="font-semibold">{w.title}</Td>
                      <Td>{w.closesAt ? formatDate(w.closesAt) : "—"}</Td>
                      <Td>
                        {w.submitted ? (
                          <Pill tone="success">Đã nộp{w.late ? " · trễ" : ""}</Pill>
                        ) : (
                          <span className="text-xs text-muted">
                            Đang làm · {w.doneSections} / {w.totalSections} mục xong
                          </span>
                        )}
                      </Td>
                      <Td className="text-right">
                        {w.documentId ? (
                          <ButtonLink href={`/group-docs/${w.documentId}`} size="sm">
                            {w.submitted ? "Xem" : "Mở tài liệu"}
                          </ButtonLink>
                        ) : (
                          <span className="text-xs text-muted">Đang chuẩn bị</span>
                        )}
                      </Td>
                    </tr>
                  ))}
                </tbody>
              </TableFrame>
            )}
          </Section>
        </>
      )}
      {requesting && view.groupId && (
        <LeaderChangeRequestDialog
          view={view}
          onClose={() => setRequesting(false)}
          onSent={() => {
            setRequesting(false);
            void reload();
          }}
        />
      )}
    </div>
  );
}

/** Popup Yêu cầu đổi trưởng nhóm (s04, BR-U12-10): lý do 10–1.000 ký tự, đề cử tùy chọn. */
export function LeaderChangeRequestDialog({ view, onClose, onSent }: { view: MyGroupView; onClose: () => void; onSent: () => void }) {
  const reasonId = useId();
  const [reason, setReason] = useState("");
  const [proposed, setProposed] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const length = reason.trim().length;
  const leader = view.members.find((m) => m.leader);
  const send = async () => {
    setBusy(true);
    setError(null);
    try {
      await submitLeaderRequest(view.groupId!, reason.trim(), proposed || null);
      onSent();
    } catch (e) {
      setError(e instanceof ApiError ? (e.problem.detail ?? "Không gửi được yêu cầu") : "Không gửi được yêu cầu");
      setBusy(false);
    }
  };
  return (
    <Dialog
      open
      title="Yêu cầu đổi trưởng nhóm"
      icon="swap_horiz"
      onClose={onClose}
      dismissible={!busy}
      footer={
        <>
          <Button onClick={onClose} disabled={busy}>
            Hủy
          </Button>
          <Button variant="primary" onClick={() => void send()} disabled={busy || length < 10 || length > 1000}>
            Gửi yêu cầu
          </Button>
        </>
      }
    >
      <p className="text-muted">
        {view.name} · Giảng viên sẽ duyệt hoặc từ chối. Mỗi nhóm chỉ có một yêu cầu chờ duyệt.
      </p>
      <p className="text-[13px]">Trưởng nhóm hiện tại: {leader?.name ?? "chưa có"}</p>
      <Field label="Lý do" htmlFor={reasonId} required hint={`${length} / 1.000 ký tự (ít nhất 10)`} error={error}>
        <textarea
          id={reasonId}
          className="min-h-[96px] rounded-[9px] border border-line-strong p-2.5 text-[13px] outline-none focus:border-primary"
          maxLength={1000}
          value={reason}
          onChange={(e) => setReason(e.target.value)}
        />
      </Field>
      <Field label="Đề xuất trưởng nhóm mới (tùy chọn)" htmlFor={`${reasonId}-p`}>
        <Select id={`${reasonId}-p`} value={proposed} onChange={(e) => setProposed(e.target.value)}>
          <option value="">Không đề xuất</option>
          {view.members.filter((m) => !m.leader).map((m) => (
            <option key={m.accountId} value={m.accountId}>
              {m.name ?? m.email}
            </option>
          ))}
        </Select>
      </Field>
    </Dialog>
  );
}
