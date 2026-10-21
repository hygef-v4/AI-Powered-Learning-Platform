"use client";

import { useState } from "react";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Field, TextInput } from "@/shared/ui";
import { errorMessage, fromLocalInput, toLocalInput } from "./format";
import type { AssignmentDetail, ScheduleInput } from "./types";

type Props = {
  assignment: AssignmentDetail;
  /** {@code publish}: bài đã duyệt; {@code change}: bài đã lên lịch hoặc đang mở. */
  mode: "publish" | "change";
  onSubmit: (input: ScheduleInput) => Promise<void>;
};

type When = { date: string; time: string };

function split(iso: string | null, time: string): When {
  const local = toLocalInput(iso);
  return local ? { date: local.slice(0, 10), time: local.slice(11, 16) } : { date: "", time };
}

function join(when: When): string | null {
  return when.date ? fromLocalInput(`${when.date}T${when.time || "00:00"}`) : null;
}

/** Ngày và giờ tách hai ô: chọn ngày là đủ, giờ có sẵn mặc định. */
function DateTime({ id, label, value, disabled, onChange }: { id: string; label: string; value: When; disabled?: boolean; onChange: (value: When) => void }) {
  return (
    <Field label={label} htmlFor={id}>
      <div className="flex gap-2">
        <TextInput id={id} type="date" aria-label={`${label} (ngày)`} value={value.date} disabled={disabled} wrapperClassName="flex-1"
          onChange={(e) => onChange({ ...value, date: e.target.value })} />
        <TextInput type="time" aria-label={`${label} (giờ)`} value={value.time} disabled={disabled} wrapperClassName="w-28"
          onChange={(e) => onChange({ ...value, time: e.target.value })} />
      </div>
    </Field>
  );
}

/**
 * Lịch phát hành (F9, F11; BR-U07-33, 35) gộp một trang: hạn nộp, số lượt; tích "Hẹn giờ mở" thì mới chọn ngày mở
 * (không tích là mở ngay), tích "Cho nộp muộn" thì mới chọn hạn nộp muộn (≤ 30 ngày sau hạn). Giờ Việt Nam.
 */
export function ScheduleForm({ assignment, mode, onSubmit }: Props) {
  const open = assignment.status === "OPEN";
  const [scheduled, setScheduled] = useState(mode === "change" && !!assignment.opensAt);
  const [opens, setOpens] = useState<When>(split(assignment.opensAt, "07:00"));
  const [closes, setCloses] = useState<When>(split(assignment.closesAt, "23:59"));
  const [allowLate, setAllowLate] = useState(!!assignment.lateUntil);
  const [late, setLate] = useState<When>(split(assignment.lateUntil, "23:59"));
  const [maxAttempts, setMaxAttempts] = useState(String(assignment.maxAttempts ?? 1));
  const [confirmGroups, setConfirmGroups] = useState(false);
  const [groupWarnings, setGroupWarnings] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  function check(): string | null {
    const close = join(closes);
    if (!close) return "Chọn hạn nộp";
    const start = scheduled ? join(opens) : new Date().toISOString();
    if (!start) return "Chọn ngày mở";
    if (new Date(close) <= new Date(start)) return "Hạn nộp phải sau lúc mở";
    if (allowLate) {
      const until = join(late);
      if (!until) return "Chọn hạn nộp muộn";
      const gap = new Date(until).getTime() - new Date(close).getTime();
      if (gap <= 0 || gap > 30 * 24 * 3600 * 1000) return "Hạn nộp muộn phải sau hạn nộp, không quá 30 ngày";
    }
    const attempts = Number(maxAttempts);
    if (!Number.isInteger(attempts) || attempts < 1 || attempts > 10) return "Số lượt từ 1 đến 10";
    return null;
  }

  async function submit() {
    const problem = check();
    setError(problem);
    if (problem) return;
    setBusy(true);
    try {
      await onSubmit({
        openNow: mode === "publish" ? !scheduled : undefined,
        opensAt: scheduled || mode === "change" ? join(opens) : null,
        closesAt: join(closes)!,
        lateUntil: allowLate ? join(late) : null,
        maxAttempts: Number(maxAttempts),
        confirmUngroupedStudents: confirmGroups,
      });
    } catch (e) {
      const problem = e instanceof ApiError ? (e.problem as { code?: string; warnings?: string[]; errors?: string[] }) : null;
      if (problem?.code === "GROUP_CONFIRMATION_REQUIRED") setGroupWarnings(problem.warnings ?? []);
      setError(problem?.errors?.length ? `${errorMessage(e)}: ${problem.errors.join(", ")}` : errorMessage(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="flex flex-col gap-3" data-testid="schedule-form">
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      {!(mode === "change" && open) && (
        <label className="flex items-center gap-2 text-[13px]">
          <input type="checkbox" checked={scheduled} onChange={(e) => setScheduled(e.target.checked)} data-testid="schedule-later" />Hẹn giờ mở
        </label>
      )}
      {scheduled && <DateTime id="schedule-opens" label="Mở lúc" value={opens} disabled={open} onChange={setOpens} />}
      <DateTime id="schedule-closes" label="Hạn nộp" value={closes} onChange={setCloses} />
      <label className="flex items-center gap-2 text-[13px]">
        <input type="checkbox" checked={allowLate} onChange={(e) => setAllowLate(e.target.checked)} data-testid="schedule-late" />Cho nộp muộn
      </label>
      {allowLate && <DateTime id="schedule-late-until" label="Nộp muộn đến" value={late} onChange={setLate} />}
      <Field label="Số lượt nộp" htmlFor="schedule-attempts">
        <TextInput id="schedule-attempts" type="number" min={1} max={10} value={maxAttempts} disabled={open} wrapperClassName="w-24"
          onChange={(e) => setMaxAttempts(e.target.value)} />
      </Field>
      {groupWarnings.length > 0 && (
        <Alert tone="warning" title="Có sinh viên chưa có nhóm">
          <ul className="list-disc pl-4">{groupWarnings.map((w, i) => <li key={i}>{w}</li>)}</ul>
          <label className="mt-1 flex items-center gap-2"><input type="checkbox" checked={confirmGroups} onChange={(e) => setConfirmGroups(e.target.checked)} />Vẫn phát hành</label>
        </Alert>
      )}
      <div>
        <Button variant="primary" onClick={() => void submit()} disabled={busy} data-testid="schedule-submit">
          {mode === "publish" ? (scheduled ? "Lên lịch" : "Phát hành ngay") : "Lưu lịch"}
        </Button>
      </div>
    </div>
  );
}
