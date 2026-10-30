"use client";

import { useId, useState, type FormEvent } from "react";
import { Button, Field, LockedInput, TextInput } from "@/shared/ui";
import { updateSubject } from "../api";
import { needsSubjectReload, SubjectMutationFeedback, useSubjectMutation, type SubjectMutationProps } from "../hooks/subjectMutation";
import { validateSubjectInformation, type SubjectFieldErrors } from "../model/validation";

/** Sửa tên, mô tả ngay trên thẻ; mã môn khóa. Hủy đưa về giá trị đang lưu, hai nút chỉ bấm được khi đã sửa. */
export function SubjectEditForm({ subject, onSaved, onReload, onAccessError }: SubjectMutationProps) {
  const id = useId();
  const [name, setName] = useState(subject.name);
  const [description, setDescription] = useState(subject.description ?? "");
  const [errors, setErrors] = useState<SubjectFieldErrors>({});
  const mutation = useSubjectMutation(onAccessError);
  const conflict = needsSubjectReload(mutation.error);
  const dirty = name !== subject.name || description !== (subject.description ?? "");
  function reset() {
    setName(subject.name);
    setDescription(subject.description ?? "");
    setErrors({});
  }
  function submit(event: FormEvent) {
    event.preventDefault();
    if (mutation.busy || conflict) return;
    const invalid = validateSubjectInformation({ name, description });
    setErrors(invalid);
    if (Object.keys(invalid).length) return;
    void mutation.run(() => updateSubject(subject.id, { name: name.trim(), description: description || null, version: subject.version }), onSaved);
  }
  return <form onSubmit={submit} noValidate aria-busy={mutation.busy} data-testid="subject-edit-form" className="flex flex-col gap-4">
    <fieldset disabled={mutation.busy} className="grid min-w-0 gap-4 sm:grid-cols-2">
      <Field label="Mã môn" htmlFor={`${id}-code`}><LockedInput id={`${id}-code`} value={subject.code} /></Field>
      <Field label="Tên môn học" htmlFor={`${id}-name`} required error={errors.name}>
        <TextInput id={`${id}-name`} value={name} maxLength={200} invalid={!!errors.name} onChange={(event) => { setName(event.target.value); setErrors((current) => ({ ...current, name: undefined })); }} data-testid="subject-edit-name" />
      </Field>
      <div className="sm:col-span-2">
        <Field label="Mô tả" htmlFor={`${id}-description`} error={errors.description}>
          <textarea id={`${id}-description`} value={description} maxLength={2000} aria-invalid={!!errors.description} onChange={(event) => { setDescription(event.target.value); setErrors((current) => ({ ...current, description: undefined })); }} data-testid="subject-edit-description"
            className="min-h-28 w-full rounded-lg border border-field px-3 py-2 text-[13.5px] text-ink outline-none focus:border-focus" />
        </Field>
      </div>
    </fieldset>
    <SubjectMutationFeedback error={mutation.error} onReload={onReload} />
    <div className="flex justify-end gap-2 border-t border-line pt-4">
      <Button onClick={reset} disabled={!dirty || mutation.busy} data-testid="subject-edit-cancel">Hủy</Button>
      <Button type="submit" variant="primary" icon="save" disabled={!dirty || mutation.busy || conflict} data-testid="subject-edit-save">Lưu thay đổi</Button>
    </div>
  </form>;
}
