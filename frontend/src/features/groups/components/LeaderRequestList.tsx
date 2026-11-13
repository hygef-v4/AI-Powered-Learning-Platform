"use client";

import { useState } from "react";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Dialog, Section, Select } from "@/shared/ui";
import { approveRequest, rejectRequest } from "../api";
import { personLabel } from "../model/groupDrafts";
import type { GroupSetView, LeaderRequestView } from "../model/types";

/**
 * Yêu cầu đổi trưởng nhóm đang chờ (F3, BR-U12-11). Thẻ ngoài gom các yêu cầu, mỗi yêu cầu một thẻ con trên một dòng: tên
 * nhóm, chọn trưởng nhóm mới, Từ chối (hỏi lại trước khi từ chối), Duyệt; không có lý do. Đang mở một nhóm (`groupId`) thì chỉ
 * hiện thẻ con của nhóm đó; mục Chưa có nhóm không có yêu cầu nên không hiện.
 */
export function LeaderRequestList({ set, groupId, onChanged }: { set: GroupSetView; groupId?: string; onChanged: () => void }) {
  const requests = groupId ? set.pendingRequests.filter((r) => r.groupId === groupId) : set.pendingRequests;
  if (requests.length === 0) return null;
  return (
    <Section title="Yêu cầu đổi trưởng nhóm" icon="swap_horiz" tone="warning">
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
      setError(e instanceof ApiError ? (e.problem.detail ?? "Không xử lý được yêu cầu") : "Không xử lý được yêu cầu");
    } finally {
      setBusy(false);
    }
  };

  return (
    <li className="flex flex-col gap-2 rounded-xl border border-line bg-surface px-4 py-3" data-testid={`leader-request-${request.groupId}`}>
      <div className="flex flex-wrap items-center gap-2 sm:flex-nowrap">
        <span className="min-w-0 flex-1 truncate text-[13.5px] font-semibold text-ink" title={request.groupName}>{request.groupName}</span>
        <Select aria-label={`Trưởng nhóm mới của ${request.groupName}`} value={leader} onChange={(e) => setLeader(e.target.value)} disabled={busy}
          wrapperClassName="w-full sm:w-64">
          <option value="">Chọn trưởng nhóm mới</option>
          {candidates.map((m) => (
            <option key={m.accountId} value={m.accountId}>
              {personLabel(m)}
              {request.proposedLeader?.accountId === m.accountId ? " (được đề xuất)" : ""}
            </option>
          ))}
        </Select>
        <Button size="sm" variant="danger" onClick={() => setConfirming(true)} disabled={busy}>
          Từ chối
        </Button>
        <Button size="sm" variant="primary" onClick={() => void act(() => approveRequest(request.id, leader || null))} disabled={busy || !leader}>
          Duyệt
        </Button>
      </div>
      {error && !confirming && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
      {confirming && (
        <Dialog open title="Từ chối yêu cầu?" tone="danger" onClose={() => { setConfirming(false); setError(null); }} dismissible={!busy}
          footer={<Button variant="danger" disabled={busy} onClick={() => void act(() => rejectRequest(request.id))} data-testid="leader-request-reject-confirm">Từ chối</Button>}>
          <p>Bạn có chắc muốn từ chối yêu cầu đổi trưởng nhóm của {request.groupName} không?</p>
          {error && <p role="alert" className="text-sm text-danger">{error}</p>}
        </Dialog>
      )}
    </li>
  );
}
