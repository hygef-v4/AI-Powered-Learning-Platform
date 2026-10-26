"use client";

import { useId, useState, type FormEvent } from "react";
import { Button, Field, TextInput } from "@/shared/ui";
import { updateSubject } from "./api";
import { needsSubjectReload, SubjectMutationFeedback, useSubjectMutation, type SubjectMutationProps } from "./subjectMutation";
import { validateSubjectInformation, type SubjectFieldErrors } from "./validation";

export function SubjectEditForm({ subject, onCancel, onSaved, onReload, onAccessError }: SubjectMutationProps & { onCancel: () => void }) {
  const id = useId();
  const [name, setName] = useState(subject.name);
  const [description, setDescription] = useState(subject.description ?? "");
  const [errors, setErrors] = useState<SubjectFieldErrors>({});
  const mutation = useSubjectMutation(onAccessError);
  const conflict = needsSubjectReload(mutation.error);
  function submit(event: FormEvent) {
    event.preventDefault();
    if (mutation.busy || conflict) return;
    const invalid = validateSubjectInformation({ name, description });
    setErrors(invalid);
    if (Object.keys(invalid).length) return;
    void mutation.run(() => updateSubject(subject.id, { name: name.trim(), description: description || null, version: subject.version }), onSaved);
  }
  return <form onSubmit={submit} noValidate aria-busy={mutation.busy} data-testid="subject-edit-form" className="flex flex-col gap-3.5">
    <fieldset disabled={mutation.busy} className="flex min-w-0 flex-col gap-3.5">
      <Field label="Mã môn" htmlFor={`${id}-code`} hint="Không đổi được sau khi tạo."><TextInput id={`${id}-code`} readOnly value={subject.code} /></Field>
      <Field label="Tên môn học" htmlFor={`${id}-name`} required error={errors.name}>
        <TextInput id={`${id}-name`} value={name} maxLength={200} invalid={!!errors.name} onChange={(event) => { setName(event.target.value); setErrors((current) => ({ ...current, name: undefined })); }} data-testid="subject-edit-name" />
      </Field>
      <Field label="Mô tả" htmlFor={`${id}-description`} error={errors.description}>
        <textarea id={`${id}-description`} value={description} maxLength={2000} aria-invalid={!!errors.description} onChange={(event) => { setDescription(event.target.value); setErrors((current) => ({ ...current, description: undefined })); }} data-testid="subject-edit-description"
          className="min-h-28 w-full rounded-lg border border-line-strong px-3 py-2 text-[13px] text-ink focus-visible:outline-primary" />
      </Field>
    </fieldset>
    <SubjectMutationFeedback error={mutation.error} onReload={onReload} />
    <div className="flex justify-end gap-2">
      <Button onClick={() => { if (mutation.canClose()) onCancel(); }} disabled={mutation.busy} data-testid="subject-edit-cancel">Hủy</Button>
      <Button type="submit" variant="primary" disabled={mutation.busy || conflict} data-testid="subject-edit-save">{mutation.busy ? "Đang lưu…" : "Lưu thay đổi"}</Button>
    </div>
  </form>;
}
