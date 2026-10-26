"use client";
import { useState } from "react";
import { gradingApi } from "./api";
import { message } from "./useResource";
import { points, type Evaluation } from "./types";

export function MemberContributionPanel({
  root,
  members,
  names,
  onSaved,
}: {
  root: Evaluation;
  members: Evaluation[];
  names: Record<string, string>;
  onSaved: () => void;
}) {
  return (
    <section className="rounded-xl border p-4">
      <h2 className="font-bold">Điểm nhóm và thành viên</h2>
      <p>
        Điểm nhóm: {points(root.score, root.maxScore)}. Giảng viên xem xét đóng góp; hệ thống không
        suy điểm từ số thao tác.
      </p>
      {members.map((m) => (
        <MemberRow
          key={`${m.id}-${m.version}`}
          rootId={root.id}
          member={m}
          name={names[m.accountId!] ?? "Thành viên"}
          onSaved={onSaved}
        />
      ))}
    </section>
  );
}
function MemberRow({
  rootId,
  member,
  name,
  onSaved,
}: {
  rootId: string;
  member: Evaluation;
  name: string;
  onSaved: () => void;
}) {
  const [score, setScore] = useState(String(member.score ?? ""));
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function save() {
    setBusy(true);
    try {
      await gradingApi.member(rootId, member.accountId!, member.version, score, reason || null);
      onSaved();
    } catch (e) {
      setError(message(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="mt-3 rounded-xl border border-line p-4">
      <p>Thành viên {name}</p>
      <label>
        Điểm{" "}
        <input
          type="number"
          min="0"
          max={Number(member.maxScore)}
          step="0.01"
          value={score}
          onChange={(e) => setScore(e.target.value)}
          className="h-10 rounded-lg border border-line-strong bg-surface px-3 text-ink"
        />
      </label>
      <label className="ml-3">
        Lý do{" "}
        <input
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          className="h-10 rounded-lg border border-line-strong bg-surface px-3 text-ink"
        />
      </label>
      <button
        disabled={busy || member.state === "SUPERSEDED" || rootId === member.id || score === ""}
        onClick={() => void save()}
        className="ml-3 rounded border px-3 py-2"
      >
        Lưu điểm thành viên
      </button>
      {error && <p role="alert">{error}</p>}
    </div>
  );
}
