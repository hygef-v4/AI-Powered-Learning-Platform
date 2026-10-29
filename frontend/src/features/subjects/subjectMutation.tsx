"use client";

import { useRef, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { Button } from "@/shared/ui";
import { isSubjectAccessError } from "./SubjectReadError";
import type { SubjectApiProblem, SubjectDetail } from "./types";

export type SubjectMutationProps = {
  subject: SubjectDetail;
  onSaved: (saved: SubjectDetail) => void;
  onReload: () => void;
  onAccessError?: (error: unknown) => void;
};

export function subjectProblem(error: unknown): SubjectApiProblem | null {
  return error instanceof ApiError ? error.problem as SubjectApiProblem : null;
}

export function needsSubjectReload(error: unknown): boolean {
  const problem = subjectProblem(error);
  return problem?.status === 409 && (problem.code === "SUBJECT_VERSION_CONFLICT" || problem.code === "SUBJECT_DATA_CONFLICT");
}

/** Không tự retry mutation; callback success nằm ngoài catch của HTTP. */
export function useSubjectMutation(onAccessError?: (error: unknown) => void) {
  const pending = useRef(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  async function run(operation: () => Promise<SubjectDetail>, onSaved: (saved: SubjectDetail) => void) {
    if (pending.current) return;
    pending.current = true;
    setBusy(true);
    setError(null);
    let saved: SubjectDetail;
    try { saved = await operation(); }
    catch (failure: unknown) {
      pending.current = false;
      setBusy(false);
      setError(failure);
      if (isSubjectAccessError(failure)) onAccessError?.(failure);
      return;
    }
    // Parent đóng editor và đọc lại snapshot/enrichment; giữ khóa đến khi unmount.
    onSaved(saved);
  }
  return { busy, error, run, canClose: () => !pending.current };
}

export function SubjectMutationFeedback({ error, onReload }: { error: unknown; onReload: () => void }) {
  if (error === null) return null;
  const problem = subjectProblem(error);
  const conflict = needsSubjectReload(error);
  const message = conflict ? "Dữ liệu môn đã thay đổi. Bản nhập được giữ lại; tải lại sẽ bỏ bản nháp hiện tại."
    : problem?.status === 401 ? "Phiên đăng nhập không còn hợp lệ. Vui lòng đăng nhập lại."
      : problem?.status === 403 ? "Bạn không có quyền thực hiện thao tác này."
        : problem?.status === 404 ? "Môn học không còn tồn tại. Hãy tải lại dữ liệu."
          : problem?.code === "SUBJECT_MANAGER_INVALID" ? "Chủ nhiệm đã chọn không còn hợp lệ. Hãy chọn lại hoặc bỏ phân công."
            : problem?.code === "SUBJECT_NAME_INVALID" ? "Tên môn là bắt buộc và tối đa 200 ký tự."
              : problem?.code === "SUBJECT_DESCRIPTION_INVALID" ? "Mô tả môn tối đa 2.000 ký tự."
                : problem?.code === "SUBJECT_CLASSES_UNAVAILABLE" ? "Chưa kiểm tra được lớp của môn. Chưa thể lưu trữ; hãy thử lại sau."
                  : "Chưa xác nhận lưu thay đổi thành công. Bản nhập được giữ lại; hãy tải lại dữ liệu để kiểm tra trước khi thử lại.";
  return <div className="flex flex-col items-start gap-2">
    <p role="alert" className="text-sm text-danger">{message}</p>
    <Button size="sm" onClick={onReload} data-testid="subject-mutation-reload">Tải lại dữ liệu và bỏ bản nháp</Button>
  </div>;
}
