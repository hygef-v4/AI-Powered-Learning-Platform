"use client";

import { useId, useState, type FormEvent } from "react";
import { Button, Field, LockedInput, Section, TextInput, showToast } from "@/shared/ui";
import type { ClassInfoInput, CourseClassView } from "../model/types";
import { operationError } from "./ui";

/**
 * Thẻ Thông tin lớp, cùng kiểu Thông tin môn học: sửa ngay trên thẻ. Hàng đầu Mã lớp và Mã môn (khóa), hàng hai Tên lớp và
 * Học kỳ, rồi Mô tả. Hủy về giá trị đang lưu; lớp lưu trữ chỉ xem.
 */
export function ClassInfoForm(props: { value: CourseClassView; onSave: (value: ClassInfoInput) => Promise<void> }) {
  return <InfoEditor key={`${props.value.id}-${props.value.version}`} {...props}/>;
}

function InfoEditor({ value, onSave }: { value: CourseClassView; onSave: (value: ClassInfoInput) => Promise<void> }) {
  const id = useId();
  const original = { name: value.name, term: value.term, description: value.description };
  const [fields, setFields] = useState(original);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const archived = value.status === "ARCHIVED";
  const dirty = fields.name !== original.name || fields.term !== original.term || fields.description !== original.description;
  async function save(event: FormEvent) {
    event.preventDefault();
    if (busy || archived) return;
    if (!fields.name.trim() || !fields.term.trim()) { setError("Vui lòng điền tên lớp và học kỳ."); return; }
    setBusy(true); setError("");
    try {
      await onSave({ name: fields.name.trim(), description: fields.description.trim(), term: fields.term.trim(), version: value.version });
      showToast("Đã lưu thay đổi");
    } catch (cause) { setError(operationError(cause)); } finally { setBusy(false); }
  }
  return <Section title="Thông tin lớp">
    <form onSubmit={save} aria-label="Thông tin lớp" noValidate className="flex flex-col gap-4">
      <fieldset disabled={busy} className="grid min-w-0 gap-4 sm:grid-cols-2">
        <Field label="Mã lớp" htmlFor={`${id}-code`}><LockedInput id={`${id}-code`} value={value.code} /></Field>
        <Field label="Mã môn" htmlFor={`${id}-subject`}><LockedInput id={`${id}-subject`} value={value.subject.code} /></Field>
        <Field label="Tên lớp" htmlFor={`${id}-name`} required>
          <TextInput id={`${id}-name`} maxLength={200} disabled={archived} value={fields.name} onChange={(event) => setFields({ ...fields, name: event.target.value })} />
        </Field>
        <Field label="Học kỳ" htmlFor={`${id}-term`} required>
          <TextInput id={`${id}-term`} maxLength={20} disabled={archived} value={fields.term} onChange={(event) => setFields({ ...fields, term: event.target.value })} />
        </Field>
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
        <Button disabled={busy || !dirty} onClick={() => { setFields(original); setError(""); }}>Hủy</Button>
        <Button type="submit" variant="primary" icon="save" disabled={busy || !dirty} data-testid="class-info-save">Lưu thay đổi</Button>
      </div>}
    </form>
  </Section>;
}
