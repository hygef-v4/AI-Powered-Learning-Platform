"use client";

import { useState } from "react";
import { formatShort } from "./format";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Pill, Section, Select, TextInput } from "@/shared/ui";
import { approveRequest, rejectRequest } from "./api";
import { personLabel } from "./GroupEditor";
import type { GroupSetView, LeaderRequestView } from "./types";

/** Yêu cầu đổi trưởng nhóm đang chờ (F3, BR-U12-11): duyệt chọn trưởng nhóm mới, từ chối bắt buộc lý do. */
export function LeaderRequestList({ set, onChanged }: { set: GroupSetView; onChanged: () => void }) {
  if (set.pendingRequests.length === 0) return null;
  return (
    <Section title={`Yêu cầu đổi trưởng nhóm (${set.pendingRequests.length} chờ duyệt)`} icon="swap_horiz" tone="warning">
      {set.pendingRequests.map((r) => (
        <RequestRow key={r.id} request={r} set={set} onChanged={onChanged} />
      ))}
    </Section>
  );
}

function RequestRow({ request, set, onChanged }: { request: LeaderRequestView; set: GroupSetView; onChanged: () => void }) {
  const group = set.groups.find((g) => g.id === request.groupId);
  const candidates = group?.members.filter((m) => m.active && m.accountId !== group.leaderId) ?? [];
  const [leader, setLeader] = useState(request.proposedLeader?.accountId ?? "");
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const act = async (action: () => Promise<unknown>) => {
    setBusy(true);
    setError(null);
    try {
      await action();
      onChanged();
    } catch (e) {
      setError(e instanceof ApiError ? (e.problem.detail ?? "Không xử lý được yêu cầu") : "Không xử lý được yêu cầu");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="flex flex-col gap-2 rounded-xl border border-line p-3.5">
      <div className="flex flex-wrap items-center gap-2 text-[13px]">
        <span className="font-semibold text-ink">{request.groupName}</span>
        <span className="text-muted">· {personLabel(request.requestedBy)} · {formatShort(request.createdAt)}</span>
        <Pill tone="warning">Chờ duyệt</Pill>
      </div>
      <p className="text-[13px] text-body">{request.reason}</p>
      <div className="grid gap-2 md:grid-cols-[1fr_1fr_auto]">
        <Select aria-label="Trưởng nhóm mới" value={leader} onChange={(e) => setLeader(e.target.value)} disabled={busy}>
          <option value="">Chọn trưởng nhóm mới</option>
          {candidates.map((m) => (
            <option key={m.accountId} value={m.accountId}>
              {personLabel(m)}
              {request.proposedLeader?.accountId === m.accountId ? " (được đề xuất)" : ""}
            </option>
          ))}
        </Select>
        <TextInput
          aria-label="Lý do từ chối"
          placeholder="Lý do từ chối (bắt buộc khi từ chối)"
          value={reason}
          maxLength={1000}
          onChange={(e) => setReason(e.target.value)}
          disabled={busy}
        />
        <div className="flex gap-2">
          <Button size="sm" onClick={() => void act(() => rejectRequest(request.id, reason.trim()))} disabled={busy || !reason.trim()}>
            Từ chối
          </Button>
          <Button size="sm" variant="primary" onClick={() => void act(() => approveRequest(request.id, leader || null))} disabled={busy || !leader}>
            Duyệt
          </Button>
        </div>
      </div>
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
    </div>
  );
}
