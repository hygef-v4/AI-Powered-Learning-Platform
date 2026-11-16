"use client";

import { useState } from "react";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Card, Dialog, Section, Select } from "@/shared/ui";
import { approveRequest, rejectRequest } from "../api";
import { personLabel } from "../model/groupDrafts";
import type { GroupSetView, LeaderRequestView } from "../model/types";
import { msg } from "@/i18n/msg";

/**
 * Yêu cầu đổi trưởng nhóm đang chờ (F3, BR-U12-11). Thẻ ngoài gom các yêu cầu, mỗi yêu cầu một thẻ con trên một dòng: tên
 * nhóm, chọn trưởng nhóm mới, Từ chối (hỏi lại trước khi từ chối), Duyệt; không có lý do. Đang mở một nhóm (`groupId`) thì chỉ
 * hiện thẻ con của nhóm đó; mục Chưa có nhóm không có yêu cầu nên không hiện.
 */
export function LeaderRequestList({ set, groupId, onChanged }: { set: GroupSetView; groupId?: string; onChanged: () => void }) {
  const requests = groupId ? set.pendingRequests.filter((r) => r.groupId === groupId) : set.pendingRequests;
  if (requests.length === 0) return null;
  return (
    <Section title={msg("groups.components.LeaderRequestList.yeuCauDoiTruongNhom")} icon="swap_horiz" tone="warning">
      <ul className="flex flex-col gap-2">
        {requests.map((r) => (
          <RequestRow key={r.id} request={r} set={set} onChanged={onChanged} />
        ))}
      </ul>
    </Section>
  );
}

function RequestRow({ request, set, onChanged }: { request: LeaderRequestView; set: GroupSetView; onChanged: () => void }) {
  const group = set.groups.find((g) => g.id === request.groupId);
  const candidates = group?.members.filter((m) => m.active && m.accountId !== group.leaderId) ?? [];
  const [leader, setLeader] = useState(request.proposedLeader?.accountId ?? "");
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const act = async (action: () => Promise<unknown>) => {
    setBusy(true);
    setError(null);
    try {
      await action();
      setConfirming(false);
      onChanged();
    } catch (e) {
      setError(e instanceof ApiError ? (e.problem.detail ?? msg("groups.components.LeaderRequestList.khongXuLyDuocYeuCau")) : msg("groups.components.LeaderRequestList.khongXuLyDuocYeuCau"));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Card as="li" padding="none" className="flex flex-col gap-2 px-4 py-3" data-testid={`leader-request-${request.groupId}`}>
      <div className="flex flex-wrap items-center gap-2 sm:flex-nowrap">
        <span className="min-w-0 flex-1 truncate text-[13.5px] font-semibold text-ink" title={request.groupName}>{request.groupName}</span>
        <Select aria-label={msg("groups.components.LeaderRequestList.truongNhomMoiCua", { groupName: String(request.groupName) })} value={leader} onChange={(e) => setLeader(e.target.value)} disabled={busy}
          wrapperClassName="w-full sm:w-64">
          <option value="">{msg("groups.components.LeaderRequestList.chonTruongNhomMoi")}</option>
          {candidates.map((m) => (
            <option key={m.accountId} value={m.accountId}>
              {personLabel(m)}
              {request.proposedLeader?.accountId === m.accountId ? msg("groups.components.LeaderRequestList.duocDeXuat") : ""}
            </option>
          ))}
        </Select>
        <Button size="sm" variant="danger" onClick={() => setConfirming(true)} disabled={busy}>
          {msg("groups.components.LeaderRequestList.tuChoi")}</Button>
        <Button size="sm" variant="primary" onClick={() => void act(() => approveRequest(request.id, leader || null))} disabled={busy || !leader}>
          {msg("groups.components.LeaderRequestList.duyet")}</Button>
      </div>
      {error && !confirming && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
      {confirming && (
        <Dialog open title={msg("groups.components.LeaderRequestList.tuChoiYeuCau")} tone="danger" onClose={() => { setConfirming(false); setError(null); }} dismissible={!busy}
          footer={<Button variant="danger" disabled={busy} onClick={() => void act(() => rejectRequest(request.id))} data-testid="leader-request-reject-confirm">{msg("groups.components.LeaderRequestList.tuChoi")}</Button>}>
          <p>{msg("groups.components.LeaderRequestList.banCoChacMuonTuChoi")}{request.groupName} {msg("groups.components.LeaderRequestList.khong")}</p>
          {error && <p role="alert" className="text-sm text-danger">{error}</p>}
        </Dialog>
      )}
    </Card>
  );
}
