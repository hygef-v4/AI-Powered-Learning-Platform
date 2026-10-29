"use client";

import { type CandidateLoader } from "./useCandidates";
import { useId, useState, type FormEvent } from "react";
import { Button, Dialog, Field, TextInput, showToast } from "@/shared/ui";
import { PersonPicker } from "./PersonPicker";
import type { CreateClassInput, PersonView, SubjectView } from "./types";
import { operationError } from "./ui";

type Errors = { code?: string; term?: string; name?: string };

/**
 * Popup Tạo lớp, cùng kiểu popup Thêm môn học: Mã lớp và Học kỳ, Tên lớp, Giảng viên (có thể để trống) rồi Mô tả. Không còn
 * lớp nháp: tạo xong lớp được mở luôn.
 */
export function ClassFormDialog({ subject, teachers, onSubmit, onClose, loadTeachers }: { subject: SubjectView; loadTeachers?: CandidateLoader; teachers: PersonView[]; onSubmit: (value: CreateClassInput) => Promise<void>; onClose: () => void }) {
  const id = useId();
  const [values, setValues] = useState({ code: "", name: "", term: "", description: "" });
  const [teacher, setTeacher] = useState<PersonView | null>(null);
  const [errors, setErrors] = useState<Errors>({});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  function set(key: keyof typeof values, value: string) {
    setValues({ ...values, [key]: value });
    setErrors((current) => ({ ...current, [key]: undefined }));
  }
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    const next: Errors = {};
    if (!values.code.trim()) next.code = "Nhập mã lớp.";
    if (!values.term.trim()) next.term = "Nhập học kỳ.";
    if (!values.name.trim()) next.name = "Nhập tên lớp.";
    setErrors(next);
    if (Object.keys(next).length) return;
    setBusy(true); setError("");
    try {
      await onSubmit({ subjectId: subject.id, code: values.code.trim(), name: values.name.trim(), term: values.term.trim(), description: values.description.trim(), teacherId: teacher?.id ?? null });
      showToast("Đã tạo lớp");
      onClose();
    } catch (cause) { setError(operationError(cause)); } finally { setBusy(false); }
  }
  return <Dialog open title="Tạo lớp" size="lg" onClose={onClose} dismissible={!busy}
    footer={<Button type="submit" form={`${id}-form`} variant="primary" disabled={busy} data-testid="class-form-submit">Tạo lớp</Button>}>
    <form id={`${id}-form`} onSubmit={(event) => void submit(event)} noValidate className="flex flex-col gap-3.5">
      <fieldset disabled={busy} className="flex min-w-0 flex-col gap-3.5">
        <div className="grid gap-3.5 sm:grid-cols-2">
          <Field label="Mã lớp" htmlFor={`${id}-code`} required error={errors.code}>
            <TextInput id={`${id}-code`} maxLength={30} value={values.code} invalid={!!errors.code} onChange={(event) => set("code", event.target.value)} data-testid="class-form-code" />
          </Field>
          <Field label="Học kỳ" htmlFor={`${id}-term`} required error={errors.term}>
            <TextInput id={`${id}-term`} maxLength={20} value={values.term} invalid={!!errors.term} onChange={(event) => set("term", event.target.value)} data-testid="class-form-term" />
          </Field>
        </div>
        <Field label="Tên lớp" htmlFor={`${id}-name`} required error={errors.name}>
          <TextInput id={`${id}-name`} maxLength={200} value={values.name} invalid={!!errors.name} onChange={(event) => set("name", event.target.value)} data-testid="class-form-name" />
        </Field>
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
