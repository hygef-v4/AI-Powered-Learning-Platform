"use client";

import { type CandidateLoader } from "../hooks/useCandidates";
import { useId, useState, type FormEvent } from "react";
import { Button, DateInput, Dialog, Field, TextInput, showToast } from "@/shared/ui";
import { PersonPicker } from "./PersonPicker";
import type { CreateClassInput, PersonView, SubjectView } from "../model/types";
import { CLASS_START_DATE_RANGE, CLASS_START_DATE_REQUIRED, formatClassFieldError, formatOperationError } from "../model/format";
import { termOf } from "../model/term";

type Errors = { code?: string; startsOn?: string };

/**
 * Popup Tạo lớp, cùng kiểu popup Thêm môn học: Mã lớp, Ngày bắt đầu, Giảng viên (có thể để trống) rồi Mô tả; không còn tên lớp
 * (yêu cầu 2026-11-02, tên lưu bằng mã lớp). Học kỳ suy ra từ ngày bắt đầu và chỉ xem trước ở đây (D-U04-05, FD Q1).
 */
export function ClassFormDialog({ subject, teachers, onSubmit, onClose, loadTeachers }: { subject: SubjectView; loadTeachers?: CandidateLoader; teachers: PersonView[]; onSubmit: (value: CreateClassInput) => Promise<void>; onClose: () => void }) {
  const id = useId();
  const [values, setValues] = useState({ code: "", startsOn: "", description: "" });
  const [teacher, setTeacher] = useState<PersonView | null>(null);
  const [errors, setErrors] = useState<Errors>({});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const term = termOf(values.startsOn);
  function set(key: keyof typeof values, value: string) {
    setValues({ ...values, [key]: value });
    setErrors((current) => ({ ...current, [key]: undefined }));
  }
  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    const next: Errors = {};
    if (!values.code.trim()) next.code = "Nhập mã lớp.";
    if (!values.startsOn) next.startsOn = CLASS_START_DATE_REQUIRED;
    else if (!term) next.startsOn = CLASS_START_DATE_RANGE;
    setErrors(next);
    if (Object.keys(next).length) return;
    setBusy(true); setError("");
    try {
      await onSubmit({ subjectId: subject.id, code: values.code.trim(), name: values.code.trim(), description: values.description.trim(), startsOn: values.startsOn, teacherId: teacher?.id ?? null });
      showToast("Đã tạo lớp");
      onClose();
    } catch (cause) {
      const fieldError = formatClassFieldError(cause);
      if (fieldError) setErrors((current) => ({ ...current, [fieldError.field]: fieldError.message }));
      else setError(formatOperationError(cause));
    } finally { setBusy(false); }
  }
  return <Dialog open title="Tạo lớp" size="lg" onClose={onClose} dismissible={!busy}
    footer={<Button type="submit" form={`${id}-form`} variant="primary" disabled={busy} data-testid="class-form-submit">Tạo lớp</Button>}>
    <form id={`${id}-form`} onSubmit={(event) => void handleSubmit(event)} noValidate className="flex flex-col gap-3.5">
      <fieldset disabled={busy} className="flex min-w-0 flex-col gap-3.5">
        <Field label="Mã lớp" htmlFor={`${id}-code`} required error={errors.code}>
          <TextInput id={`${id}-code`} maxLength={30} value={values.code} invalid={!!errors.code} onChange={(event) => set("code", event.target.value)} data-testid="class-form-code" />
        </Field>
        <div className="flex flex-col gap-1.5">
          <Field label="Ngày bắt đầu" htmlFor={`${id}-starts-on`} required error={errors.startsOn}>
            <DateInput id={`${id}-starts-on`} min="2000-01-01" max="2099-12-31" value={values.startsOn} invalid={!!errors.startsOn}
              onChange={(value) => set("startsOn", value)} data-testid="class-form-starts-on" />
          </Field>
          {term && <p aria-live="polite" className="text-xs text-muted" data-testid="class-form-term">Học kỳ: <span className="font-mono text-ink">{term}</span></p>}
        </div>
        <PersonPicker label="Giảng viên" value={teacher} onChange={setTeacher} people={teachers} loader={loadTeachers} clearable disabled={busy} />
        <Field label="Mô tả" htmlFor={`${id}-description`}>
          <textarea id={`${id}-description`} maxLength={2000} value={values.description} onChange={(event) => set("description", event.target.value)}
            className="min-h-20 w-full rounded-lg border border-field px-3 py-2 text-[13.5px] text-ink outline-none focus:border-focus" />
        </Field>
      </fieldset>
      {error && <p role="alert" className="text-sm text-danger">{error}</p>}
    </form>
  </Dialog>;
}
