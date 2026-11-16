"use client";

import { useCallback, useId, useRef, useState, type FormEvent } from "react";
import { ApiError } from "@/shared/api/client";
import { Button, Dialog, Field, Textarea, TextInput } from "@/shared/ui";
import { createSubject } from "../api";
import { ManagerPicker } from "./ManagerPicker";
import type { ManagerCandidate, SearchSubjectManagers } from "../model/managerSearch";
import type { SubjectDetail } from "../model/types";
import {
  normalizeSubjectCode,
  validateCreateSubject,
  type SubjectFieldErrors,
} from "../model/validation";
import { msg } from "@/i18n/msg";

type Props = {
  onClose: () => void;
  onCreated: (subject: SubjectDetail) => void;
  searchManagers?: SearchSubjectManagers;
};

/** Mount khi mở, unmount khi đóng: mỗi lần mở là một draft mới. */
export function SubjectFormDialog({ onClose, onCreated, searchManagers }: Props) {
  const formId = useId();
  const pending = useRef(false);
  const [busy, setBusy] = useState(false);
  const [code, setCode] = useState("");
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [manager, setManager] = useState<ManagerCandidate | null>(null);
  const [errors, setErrors] = useState<SubjectFieldErrors & { managerId?: string }>({});
  const [message, setMessage] = useState<string | null>(null);
  const handleClose = useCallback(() => {
    if (!pending.current) onClose();
  }, [onClose]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (pending.current) return;
    const nextErrors = validateCreateSubject({ code, name, description });
    setErrors(nextErrors);
    setMessage(null);
    if (Object.keys(nextErrors).length > 0) return;
    pending.current = true;
    setBusy(true);
    let created: SubjectDetail;
    try {
      created = await createSubject({
        code: normalizeSubjectCode(code),
        name: name.trim(),
        description: description || null,
        managerId: manager?.accountId ?? null,
      });
    } catch (error: unknown) {
      const problem = error instanceof ApiError ? error.problem : null;
      if (problem?.code === "SUBJECT_CODE_DUPLICATE")
        setErrors({ code: msg("subjects.components.SubjectFormDialog.maMonDaTonTaiHay") });
      else if (problem?.code === "SUBJECT_MANAGER_INVALID")
        setErrors({
          managerId: msg("subjects.components.SubjectFormDialog.chuNhiemDaChonKhongCon"),
        });
      else
        setMessage(
          problem?.status === 401
            ? msg("subjects.components.SubjectFormDialog.phienDangNhapKhongConHop")
            : problem?.status === 403
              ? msg("subjects.components.SubjectFormDialog.banKhongCoQuyenThemMon")
              : msg("subjects.components.SubjectFormDialog.chuaXacNhanTaoMonThanh"),
        );
      pending.current = false;
      setBusy(false);
      return;
    }
    // Tách callback khỏi catch mutation: lỗi refetch không được diễn giải thành POST thất bại.
    onCreated(created);
  }

  return (
    <Dialog
      open
      title={msg("subjects.components.SubjectFormDialog.themMonHoc")}
      icon="menu_book"
      onClose={handleClose}
      dismissible={!busy}
      size="lg"
      footer={
        <Button
          type="submit"
          form={formId}
          variant="primary"
          disabled={busy}
          data-testid="subject-create-submit"
        >
          {busy ? msg("subjects.components.SubjectFormDialog.dangThem") : msg("subjects.components.SubjectFormDialog.themMonHoc")}
        </Button>
      }
    >
      <form
        id={formId}
        onSubmit={(event) => void handleSubmit(event)}
        noValidate
        aria-busy={busy}
        data-testid="subject-create-form"
        className="flex flex-col gap-3.5"
      >
        <fieldset disabled={busy} className="flex min-w-0 flex-col gap-3.5">
          <Field label={msg("subjects.components.SubjectFormDialog.maMon")} htmlFor={`${formId}-code`} required error={errors.code}>
            <TextInput
              id={`${formId}-code`}
              value={code}
              maxLength={20}
              invalid={!!errors.code}
              onChange={(event) => {
                setCode(event.target.value.toUpperCase());
                setErrors((current) => ({ ...current, code: undefined }));
              }}
              data-testid="subject-create-code"
            />
          </Field>
          <Field label={msg("subjects.components.SubjectFormDialog.tenMon")} htmlFor={`${formId}-name`} required error={errors.name}>
            <TextInput
              id={`${formId}-name`}
              value={name}
              maxLength={200}
              invalid={!!errors.name}
              onChange={(event) => {
                setName(event.target.value);
                setErrors((current) => ({ ...current, name: undefined }));
              }}
              data-testid="subject-create-name"
            />
          </Field>
          <ManagerPicker
            value={manager}
            onChange={(value) => {
              setManager(value);
              setErrors((current) => ({ ...current, managerId: undefined }));
            }}
            disabled={busy}
            searchManagers={searchManagers}
          />
          {errors.managerId && (
            <p role="alert" className="text-xs text-danger">
              {errors.managerId}
            </p>
          )}
          <Field label={msg("subjects.components.SubjectFormDialog.moTa")} htmlFor={`${formId}-description`} error={errors.description}>
            <Textarea
              id={`${formId}-description`}
              value={description}
              maxLength={2000}
              aria-invalid={!!errors.description}
              onChange={(event) => {
                setDescription(event.target.value);
                setErrors((current) => ({ ...current, description: undefined }));
              }}
              data-testid="subject-create-description"
              className="min-h-20"
            />
          </Field>
        </fieldset>
        {message && (
          <p role="alert" className="text-sm text-danger">
            {message}
          </p>
        )}
      </form>
    </Dialog>
  );
}
