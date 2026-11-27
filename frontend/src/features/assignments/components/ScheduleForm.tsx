"use client";

import { useState } from "react";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, DateInput, Field, TextInput } from "@/shared/ui";
import { describeError, fromLocalInput, toLocalInput } from "../model/format";
import type { AssignmentDetail, ScheduleInput } from "../model/types";
import { msg } from "@/i18n/msg";

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

/** Ngày giờ hiện tại theo giờ Việt Nam, dạng {@code yyyy-mm-ddThh:mm}. */
function nowLocal(): When {
  const local = toLocalInput(new Date().toISOString()) ?? "";
  return { date: local.slice(0, 10), time: local.slice(11, 16) };
}

/** Thời điểm đã qua (so tới phút theo giờ Việt Nam). */
function isPast(when: When): boolean {
  const at = join(when);
  return !!at && new Date(at).getTime() < Date.now() - 60_000;
}

/**
 * Ngày và giờ tách hai ô: chọn ngày là đủ, giờ có sẵn mặc định. Không chọn được ngày đã qua; hôm nay thì không chọn được
 * giờ đã qua (yêu cầu 2026-11-26).
 */
function DateTime({ id, label, value, disabled, onChange }: { id: string; label: string; value: When; disabled?: boolean; onChange: (value: When) => void }) {
  const now = nowLocal();
  return (
    <Field label={label} htmlFor={id}>
      <div className="flex gap-2">
        <DateInput id={id} aria-label={msg("assignments.components.ScheduleForm.date", { label: String(label) })} value={value.date} disabled={disabled} wrapperClassName="flex-1"
          min={now.date} onChange={(date) => onChange({ ...value, date })} />
        <TextInput type="time" aria-label={msg("assignments.components.ScheduleForm.time", { label: String(label) })} value={value.time} disabled={disabled} wrapperClassName="w-28"
          min={value.date === now.date ? now.time : undefined} invalid={!disabled && isPast(value)}
          onChange={(e) => onChange({ ...value, time: e.target.value })} />
      </div>
    </Field>
  );
}

/**
 * Xếp lịch (F9, F11; BR-U07-33, 35) gộp một trang: hạn nộp, số lượt; ngày mở tùy chọn: tích "Hẹn giờ mở" thì mới chọn
 * ngày mở (không tích là mở ngay), tích "Cho nộp muộn" thì mới chọn hạn nộp muộn (≤ 30 ngày sau hạn). Giờ Việt Nam.
 */
export function ScheduleForm({ assignment, mode, onSubmit }: Props) {
  const open = assignment.status === "OPEN";
  // Chỉ bài Luyện tập có số lượt; bài có chấm nộp bao nhiêu lần cũng được khi còn hạn (yêu cầu 2026-11-10).
  const practice = assignment.gradingMode === "PRACTICE";
  const [scheduled, setScheduled] = useState(mode === "change" && !!assignment.opensAt);
  const [opens, setOpens] = useState<When>(split(assignment.opensAt, "00:00"));
  const [closes, setCloses] = useState<When>(split(assignment.closesAt, "00:00"));
  const [allowLate, setAllowLate] = useState(!!assignment.lateUntil);
  const [late, setLate] = useState<When>(split(assignment.lateUntil, "00:00"));
  const [maxAttempts, setMaxAttempts] = useState(String(assignment.maxAttempts ?? 1));
  const [confirmGroups, setConfirmGroups] = useState(false);
  const [groupWarnings, setGroupWarnings] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  function validateSchedule(): string | null {
    const close = join(closes);
    if (!close) return msg("assignments.components.ScheduleForm.missingDeadline");
    const start = scheduled ? join(opens) : new Date().toISOString();
    if (!start) return msg("assignments.components.ScheduleForm.missingOpeningDate");
    // Bài đang mở giữ ngày mở cũ; còn lại ngày mở hẹn giờ không được ở quá khứ.
    if (scheduled && !open && isPast(opens)) return msg("assignments.components.ScheduleForm.openingDateInThePast");
    // Bài đang trong hạn nộp muộn thì hạn nộp đã qua là bình thường: chỉ cần hạn cuối nhận bài còn ở tương lai.
    if (open ? isPast(allowLate && late.date ? late : closes) : isPast(closes)) return msg("assignments.components.ScheduleForm.deadlineInThePast");
    if (new Date(close) <= new Date(start)) return msg("assignments.components.ScheduleForm.theDeadlineMustBeAfter");
    if (allowLate) {
      const until = join(late);
      if (!until) return msg("assignments.components.ScheduleForm.missingLateDeadline");
      const gap = new Date(until).getTime() - new Date(close).getTime();
      if (gap <= 0) return msg("assignments.components.ScheduleForm.theLateDeadlineMustBe");
      if (gap > 30 * 24 * 3600 * 1000) return msg("assignments.components.ScheduleForm.theLateDeadlineIsMore");
    }
    const attempts = Number(maxAttempts);
    if (practice && (!Number.isInteger(attempts) || attempts < 1 || attempts > 10)) return msg("assignments.components.ScheduleForm.attemptsMustBe110");
    return null;
  }

  async function submit() {
    const problem = validateSchedule();
    setError(problem);
    if (problem) return;
    setBusy(true);
    try {
      await onSubmit({
        openNow: mode === "publish" ? !scheduled : undefined,
        // Bài đang mở giữ nguyên ngày mở (ô chỉ có tới phút, server so từng giây); bài đã lên lịch mà bỏ tích "Hẹn giờ
        // mở" thì mở ngay (bộ hẹn giờ mở trong vòng 1 phút, BR-U07-34).
        opensAt: open ? null : scheduled ? join(opens) : mode === "change" ? new Date().toISOString() : null,
        closesAt: join(closes)!,
        lateUntil: allowLate ? join(late) : null,
        maxAttempts: practice ? Number(maxAttempts) : null,
        confirmUngroupedStudents: confirmGroups,
      });
    } catch (e) {
      const problem = e instanceof ApiError ? (e.problem as { code?: string; warnings?: string[]; errors?: string[] }) : null;
      if (problem?.code === "GROUP_CONFIRMATION_REQUIRED") setGroupWarnings(problem.warnings ?? []);
      setError(problem?.errors?.length ? `${describeError(e)}: ${problem.errors.join(", ")}` : describeError(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="flex flex-col gap-3" data-testid="schedule-form">
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      {!(mode === "change" && open) && (
        <label className="flex items-center gap-2 text-[13px]">
          <input type="checkbox" checked={scheduled} onChange={(e) => setScheduled(e.target.checked)} data-testid="schedule-later" />{msg("assignments.components.ScheduleForm.scheduleOpening")}</label>
      )}
      {/* Ngày mở tùy chọn: chỉ hiện khi tích "Hẹn giờ mở"; bài đang mở thì không còn gì để chọn nên ẩn hẳn. */}
      {scheduled && !open && <DateTime id="schedule-opens" label={msg("assignments.components.ScheduleForm.opensAt")} value={opens} onChange={setOpens} />}
      <DateTime id="schedule-closes" label={msg("assignments.components.ScheduleForm.deadline")} value={closes} onChange={setCloses} />
      <label className="flex items-center gap-2 text-[13px]">
        <input type="checkbox" checked={allowLate} onChange={(e) => setAllowLate(e.target.checked)} data-testid="schedule-late" />{msg("assignments.components.ScheduleForm.allowLateSubmission")}</label>
      {allowLate && <DateTime id="schedule-late-until" label={msg("assignments.components.ScheduleForm.lateSubmissionUntil")} value={late} onChange={setLate} />}
      {practice && (
        <Field label={msg("assignments.components.ScheduleForm.attempts")} htmlFor="schedule-attempts">
          <TextInput id="schedule-attempts" type="number" min={1} max={10} value={maxAttempts} disabled={open} wrapperClassName="w-24"
            onChange={(e) => setMaxAttempts(e.target.value)} />
        </Field>
      )}
      {groupWarnings.length > 0 && (
        <Alert tone="warning" title={msg("assignments.components.ScheduleForm.someStudentsHaveNoGroup")}>
          <ul className="list-disc pl-4">{groupWarnings.map((w, i) => <li key={i}>{w}</li>)}</ul>
          <label className="mt-1 flex items-center gap-2"><input type="checkbox" checked={confirmGroups} onChange={(e) => setConfirmGroups(e.target.checked)} />{msg("assignments.components.ScheduleForm.publishAnyway")}</label>
        </Alert>
      )}
      <div>
        <Button variant="primary" onClick={() => void submit()} disabled={busy} data-testid="schedule-submit">
          {mode === "publish" ? (scheduled ? msg("assignments.components.ScheduleForm.schedule") : msg("assignments.components.ScheduleForm.publishNow")) : msg("assignments.components.ScheduleForm.saveSchedule")}
        </Button>
      </div>
    </div>
  );
}
