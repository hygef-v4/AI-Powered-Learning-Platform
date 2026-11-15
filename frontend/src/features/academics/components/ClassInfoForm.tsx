"use client";

import { useId, useState, type FormEvent } from "react";
import { Button, Field, LockedInput, Section, TextInput, showToast } from "@/shared/ui";
import type { ClassInfoInput, CourseClassView } from "../model/types";
import { CLASS_START_DATE_RANGE, CLASS_START_DATE_REQUIRED, formatClassFieldError, formatOperationError } from "../model/format";
import { termOf } from "../model/term";

/**
 * Thẻ Thông tin lớp, cùng kiểu Thông tin môn học: sửa ngay trên thẻ. Hàng đầu Mã môn và Mã lớp (khóa), rồi Ngày bắt đầu và Học
 * kỳ chỉ đọc (D-U04-05), rồi Mô tả; không còn tên lớp (tên hiện có được giữ khi lưu). Đổi ngày làm học kỳ đổi thì hiện
 * "cũ → mới", không hỏi xác nhận (FD Q2). Hủy về giá trị đang lưu; lớp lưu trữ chỉ xem.
 */
export function ClassInfoForm(props: { value: CourseClassView; onSave: (value: ClassInfoInput) => Promise<void> }) {
  return <InfoEditor key={`${props.value.id}-${props.value.version}`} {...props}/>;
}

function InfoEditor({ value, onSave }: { value: CourseClassView; onSave: (value: ClassInfoInput) => Promise<void> }) {
  const id = useId();
  const original = { description: value.description, startsOn: value.startsOn ?? "" };
  const [fields, setFields] = useState(original);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [dateError, setDateError] = useState("");
  const archived = value.status === "ARCHIVED";
  const dirty = fields.description !== original.description || fields.startsOn !== original.startsOn;
  const nextTerm = termOf(fields.startsOn);
  const termChanged = fields.startsOn !== original.startsOn && nextTerm !== null && nextTerm !== value.term;
  async function handleSave(event: FormEvent) {
    event.preventDefault();
    if (busy || archived) return;
    const invalidDate = !fields.startsOn ? CLASS_START_DATE_REQUIRED : !nextTerm ? CLASS_START_DATE_RANGE : "";
    setDateError(invalidDate);
    if (invalidDate) return;
    setBusy(true); setError("");
    try {
      await onSave({ name: value.name || value.code, description: fields.description.trim(), startsOn: fields.startsOn, version: value.version });
      showToast("Đã lưu thay đổi");
    } catch (cause) {
      const fieldError = formatClassFieldError(cause);
      if (fieldError) setDateError(fieldError.message);
      else setError(formatOperationError(cause));
    } finally { setBusy(false); }
  }
  return <Section title="Thông tin lớp">
    <form onSubmit={handleSave} aria-label="Thông tin lớp" noValidate className="flex flex-col gap-4">
      <fieldset disabled={busy} className="grid min-w-0 gap-4 sm:grid-cols-2">
        <Field label="Mã môn" htmlFor={`${id}-subject`}><LockedInput id={`${id}-subject`} value={value.subject.code} /></Field>
        <Field label="Mã lớp" htmlFor={`${id}-code`}><LockedInput id={`${id}-code`} value={value.code} /></Field>
        <Field label="Ngày bắt đầu" htmlFor={`${id}-starts-on`} required={!archived} error={dateError}>
          <TextInput id={`${id}-starts-on`} type="date" min="2000-01-01" max="2099-12-31" disabled={archived} value={fields.startsOn}
            invalid={!!dateError} onChange={(event) => { setFields({ ...fields, startsOn: event.target.value }); setDateError(""); }}
            data-testid="class-info-starts-on" />
        </Field>
        <div className="flex flex-col gap-1.5">
          <Field label="Học kỳ" htmlFor={`${id}-term`}><LockedInput id={`${id}-term`} value={value.term} /></Field>
          {termChanged && <p aria-live="polite" className="text-xs text-muted" data-testid="class-info-term-change">
            Học kỳ: <span className="font-mono text-ink">{value.term} → {nextTerm}</span></p>}
        </div>
        <div className="sm:col-span-2">
          <Field label="Mô tả" htmlFor={`${id}-description`}>
            <textarea id={`${id}-description`} maxLength={2000} disabled={archived} value={fields.description}
              onChange={(event) => setFields({ ...fields, description: event.target.value })}
              className="min-h-28 w-full rounded-lg border border-field px-3 py-2 text-[13.5px] text-ink outline-none focus:border-focus disabled:cursor-not-allowed disabled:bg-sunken disabled:text-muted" />
          </Field>
        </div>
      </fieldset>
      {error && <p role="alert" className="text-sm text-danger">{error}</p>}
      {!archived && <div className="flex justify-end gap-2 border-t border-line pt-4">
        <Button disabled={busy || !dirty} onClick={() => { setFields(original); setError(""); setDateError(""); }}>Hủy</Button>
        <Button type="submit" variant="primary" icon="save" disabled={busy || !dirty} data-testid="class-info-save">Lưu thay đổi</Button>
      </div>}
    </form>
  </Section>;
}
