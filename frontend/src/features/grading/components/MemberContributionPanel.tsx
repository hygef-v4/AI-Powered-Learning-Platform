"use client";
import { useId, useState } from "react";
import { Button, Field, TextInput } from "@/shared/ui";
import { gradingApi } from "../api";
import { formatErrorMessage } from "../model/format";
import { formatPoints, type Evaluation } from "../model/types";

export function MemberContributionPanel({
  root,
  members,
  names,
  onSaved,
  editable = true,
}: {
  root: Evaluation;
  members: Evaluation[];
  names: Record<string, string>;
  onSaved: () => void;
  editable?: boolean;
}) {
  return (
    <section id="group-member-grades" aria-labelledby="group-member-grades-title" tabIndex={-1}
      className="@container scroll-mt-36 rounded-xl border border-line bg-surface px-4 py-3 sm:px-5">
      <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
        <h2 id="group-member-grades-title" className="text-sm font-semibold text-ink">
          Điểm nhóm và thành viên
        </h2>
        <p className="text-xs text-muted">
          Điểm nhóm <span className="font-semibold tabular-nums text-ink">
            {formatPoints(root.score, root.maxScore)}
          </span>
        </p>
      </div>
      <p className="mt-1 text-xs text-muted">
        Giảng viên xem xét đóng góp; hệ thống không suy điểm từ số thao tác.
      </p>
      <ul className="mt-3 divide-y divide-line-soft border-t border-line-soft">
        {members.map((member) => (
          <MemberRow
            key={`${member.id}-${member.version}`}
            rootId={root.id}
            locked={!editable || root.state === "SUPERSEDED"}
            member={member}
            name={names[member.accountId!] ?? "Thành viên"}
            onSaved={onSaved}
          />
        ))}
      </ul>
    </section>
  );
}
function MemberRow({
  rootId,
  locked,
  member,
  name,
  onSaved,
}: {
  rootId: string;
  locked: boolean;
  member: Evaluation;
  name: string;
  onSaved: () => void;
}) {
  const fieldId = useId();
  const [score, setScore] = useState(String(member.score ?? ""));
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const disabled = locked || busy || member.state === "SUPERSEDED";
  async function handleSave() {
    if (disabled) return;
    setBusy(true);
    try {
      await gradingApi.saveMemberGrade(rootId, member.accountId!, member.version, score, reason || null);
      onSaved();
    } catch (e) {
      setError(formatErrorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <li className="py-3">
      <fieldset className="min-w-0">
        <legend className="mb-2 wrap-break-word text-sm font-semibold text-ink">{name}</legend>
        <div className="grid items-end gap-3 @lg:grid-cols-[7rem_minmax(0,1fr)_auto]">
          <Field label="Điểm" htmlFor={`${fieldId}-score`}>
            <TextInput
              id={`${fieldId}-score`}
              type="number"
              disabled={disabled}
              min="0"
              max={Number(member.maxScore)}
              step="0.01"
              value={score}
              onChange={(event) => setScore(event.target.value)}
            />
          </Field>
          <Field label="Lý do" htmlFor={`${fieldId}-reason`}>
            <TextInput
              id={`${fieldId}-reason`}
              value={reason}
              disabled={disabled}
              onChange={(event) => setReason(event.target.value)}
            />
          </Field>
          <Button
            disabled={disabled || rootId === member.id || score === ""}
            onClick={() => void handleSave()}
            className="h-10 justify-self-start"
          >
            Lưu điểm thành viên
          </Button>
        </div>
        {error && <p role="alert" className="mt-2 text-xs text-danger">{error}</p>}
      </fieldset>
    </li>
  );
}
