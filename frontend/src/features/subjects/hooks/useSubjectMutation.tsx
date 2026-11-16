"use client";

import { useRef, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { Button } from "@/shared/ui";
import { isSubjectAccessError } from "../components/SubjectReadError";
import type { SubjectApiProblem, SubjectDetail } from "../model/types";
import { msg } from "@/i18n/msg";

export type SubjectMutationProps = {
  subject: SubjectDetail;
  onSaved: (saved: SubjectDetail) => void;
  onReload: () => void;
  onAccessError?: (error: unknown) => void;
};

export function readSubjectProblem(error: unknown): SubjectApiProblem | null {
  return error instanceof ApiError ? (error.problem as SubjectApiProblem) : null;
}

export function shouldReloadSubject(error: unknown): boolean {
  const problem = readSubjectProblem(error);
  return (
    problem?.status === 409 &&
    (problem.code === "SUBJECT_VERSION_CONFLICT" || problem.code === "SUBJECT_DATA_CONFLICT")
  );
}

/** Không tự retry mutation; callback success nằm ngoài catch của HTTP. */
export function useSubjectMutation(onAccessError?: (error: unknown) => void) {
  const pending = useRef(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  async function runMutation(
    operation: () => Promise<SubjectDetail>,
    onSaved: (saved: SubjectDetail) => void,
  ) {
    if (pending.current) return;
    pending.current = true;
    setBusy(true);
    setError(null);
    let saved: SubjectDetail;
    try {
      saved = await operation();
    } catch (failure: unknown) {
      pending.current = false;
      setBusy(false);
      setError(failure);
      if (isSubjectAccessError(failure)) onAccessError?.(failure);
      return;
    }
    // Parent đóng editor và đọc lại snapshot/enrichment; giữ khóa đến khi unmount.
    onSaved(saved);
  }
  return { busy, error, run: runMutation, canClose: () => !pending.current };
}

export function SubjectMutationFeedback({
  error,
  onReload,
}: {
  error: unknown;
  onReload: () => void;
}) {
  if (error === null) return null;
  const problem = readSubjectProblem(error);
  const conflict = shouldReloadSubject(error);
  const message = conflict
    ? msg("subjects.hooks.useSubjectMutation.duLieuMonDaThayDoi")
    : problem?.status === 401
      ? msg("subjects.hooks.useSubjectMutation.phienDangNhapKhongConHop")
      : problem?.status === 403
        ? msg("subjects.hooks.useSubjectMutation.banKhongCoQuyenThucHien")
        : problem?.status === 404
          ? msg("subjects.hooks.useSubjectMutation.monHocKhongConTonTai")
          : problem?.code === "SUBJECT_MANAGER_INVALID"
            ? msg("subjects.hooks.useSubjectMutation.chuNhiemDaChonKhongCon")
            : problem?.code === "SUBJECT_NAME_INVALID"
              ? msg("subjects.hooks.useSubjectMutation.tenMonLaBatBuocVa")
              : problem?.code === "SUBJECT_DESCRIPTION_INVALID"
                ? msg("subjects.hooks.useSubjectMutation.moTaMonToiDa2")
                : problem?.code === "SUBJECT_CLASSES_UNAVAILABLE"
                  ? msg("subjects.hooks.useSubjectMutation.chuaKiemTraDuocLopCua")
                  : msg("subjects.hooks.useSubjectMutation.chuaXacNhanLuuThayDoi");
  return (
    <div className="flex flex-col items-start gap-2">
      <p role="alert" className="text-sm text-danger">
        {message}
      </p>
      <Button size="sm" onClick={onReload} data-testid="subject-mutation-reload">
        {msg("subjects.hooks.useSubjectMutation.taiLaiDuLieuVaBo")}</Button>
    </div>
  );
}
