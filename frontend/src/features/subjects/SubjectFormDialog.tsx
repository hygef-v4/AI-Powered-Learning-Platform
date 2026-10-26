"use client";

import { useCallback, useId, useRef, useState, type FormEvent } from "react";
import { ApiError } from "@/shared/api/client";
import { Button, Dialog, Field, TextInput } from "@/shared/ui";
import { createSubject } from "./api";
import { ManagerPicker } from "./ManagerPicker";
import type { ManagerCandidate, SearchSubjectManagers } from "./managerSearch";
import type { SubjectDetail } from "./types";
import { normalizeSubjectCode, validateCreateSubject, type SubjectFieldErrors } from "./validation";

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
  const close = useCallback(() => { if (!pending.current) onClose(); }, [onClose]);

  async function submit(event: FormEvent) {
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
      created = await createSubject({ code: normalizeSubjectCode(code), name: name.trim(), description: description || null, managerId: manager?.accountId ?? null });
    } catch (error: unknown) {
      const problem = error instanceof ApiError ? error.problem : null;
      if (problem?.code === "SUBJECT_CODE_DUPLICATE") setErrors({ code: "Mã môn đã tồn tại, hãy dùng mã khác." });
      else if (problem?.code === "SUBJECT_MANAGER_INVALID") setErrors({ managerId: "Chủ nhiệm đã chọn không còn hợp lệ. Hãy chọn lại hoặc bỏ lựa chọn." });
      else setMessage(problem?.status === 401 ? "Phiên đăng nhập không còn hợp lệ. Vui lòng đăng nhập lại."
        : problem?.status === 403 ? "Bạn không có quyền thêm môn học."
        : "Chưa xác nhận tạo môn thành công. Dữ liệu nhập được giữ lại; hãy kiểm tra danh sách trước khi thử lại.");
      pending.current = false;
      setBusy(false);
      return;
    }
    // Tách callback khỏi catch mutation: lỗi refetch không được diễn giải thành POST thất bại.
    onCreated(created);
  }

  return <Dialog open title="Thêm môn học" icon="menu_book" onClose={close} dismissible={!busy} size="lg"
    footer={<><Button onClick={close} disabled={busy} data-testid="subject-create-cancel">Hủy</Button>
      <Button type="submit" form={formId} variant="primary" disabled={busy} data-testid="subject-create-submit">{busy ? "Đang thêm…" : "Thêm môn học"}</Button></>}>
    <form id={formId} onSubmit={(event) => void submit(event)} noValidate aria-busy={busy} data-testid="subject-create-form" className="flex flex-col gap-3.5">
      <fieldset disabled={busy} className="flex min-w-0 flex-col gap-3.5">
        <Field label="Mã môn" htmlFor={`${formId}-code`} required error={errors.code} hint="Tự viết hoa; chữ hoa, số hoặc dấu -; tối đa20 ký tự. Không đổi sau khi tạo.">
          <TextInput id={`${formId}-code`} value={code} maxLength={20} invalid={!!errors.code} onChange={(event) => { setCode(event.target.value.toUpperCase()); setErrors((current) => ({ ...current, code: undefined })); }} data-testid="subject-create-code" />
        </Field>
        <Field label="Tên môn" htmlFor={`${formId}-name`} required error={errors.name}>
          <TextInput id={`${formId}-name`} value={name} maxLength={200} invalid={!!errors.name} onChange={(event) => { setName(event.target.value); setErrors((current) => ({ ...current, name: undefined })); }} data-testid="subject-create-name" />
        </Field>
        <ManagerPicker value={manager} onChange={(value) => { setManager(value); setErrors((current) => ({ ...current, managerId: undefined })); }} disabled={busy} searchManagers={searchManagers} />
        {errors.managerId && <p role="alert" className="text-xs text-danger">{errors.managerId}</p>}
        <Field label="Mô tả (tùy chọn)" htmlFor={`${formId}-description`} error={errors.description}>
          <textarea id={`${formId}-description`} value={description} maxLength={2000} aria-invalid={!!errors.description} onChange={(event) => { setDescription(event.target.value); setErrors((current) => ({ ...current, description: undefined })); }} data-testid="subject-create-description"
            className="min-h-20 w-full rounded-lg border border-line-strong px-3 py-2 text-[13px] text-ink focus-visible:outline-primary" />
        </Field>
      </fieldset>
      {message && <p role="alert" className="text-sm text-danger">{message}</p>}
    </form>
  </Dialog>;
}
