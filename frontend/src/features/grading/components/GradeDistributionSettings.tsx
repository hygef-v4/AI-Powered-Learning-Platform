"use client";

import { useEffect, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, InfoTip, Switch } from "@/shared/ui";
import { gradingApi, type GradeDistributionVisibility } from "../api";
import { msg } from "@/i18n/msg";

export function GradeDistributionSettings({ classId, assignmentId, gradingMode }: {
  classId: string;
  assignmentId: string;
  gradingMode: "GRADED" | "PRACTICE";
}) {
  return gradingMode === "GRADED"
    ? <VisibilitySetting key={`${classId}:${assignmentId}`} classId={classId} assignmentId={assignmentId} />
    : null;
}

function VisibilitySetting({ classId, assignmentId }: { classId: string; assignmentId: string }) {
  const [value, setValue] = useState<GradeDistributionVisibility | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [epoch, setEpoch] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    gradingApi.loadDistributionVisibility(classId, assignmentId, controller.signal).then(
      (next) => { if (!controller.signal.aborted) { setValue(next); setError(null); } },
      (cause) => {
        if (controller.signal.aborted) return;
        if (cause instanceof ApiError && [401, 403, 404].includes(cause.problem.status)) return;
        setError(msg("grading.components.GradeDistributionSettings.couldNotLoadTheGrade"));
      },
    );
    return () => controller.abort();
  }, [classId, assignmentId, epoch]);

  async function handleSave(enabled: boolean) {
    if (!value?.canEdit || busy || error) return;
    setBusy(true);
    try {
      setValue(await gradingApi.saveDistributionVisibility(classId, assignmentId, enabled, value.version));
    } catch (cause) {
      if (cause instanceof ApiError && [401, 403, 404].includes(cause.problem.status)) setValue(null);
      setError(cause instanceof ApiError && cause.problem.status === 409
        ? msg("grading.components.GradeDistributionSettings.theGradeDistributionSettingsChanged")
        : msg("grading.components.GradeDistributionSettings.couldNotSaveTheGrade"));
    } finally {
      setBusy(false);
    }
  }

  if (!error && !value?.canEdit) return null;
  return <div className="flex flex-col gap-1" data-testid="grade-distribution-settings">
    {value?.canEdit && <>
      <span className="flex items-center gap-1">
        <Switch checked={value.enabled} label={msg("grading.components.GradeDistributionSettings.showGradeDistributionToStudents")} ariaLabel={msg("grading.components.GradeDistributionSettings.showGradeDistributionToStudents")}
          disabled={busy || !!error} onChange={(enabled) => handleSave(enabled)} />
        <InfoTip label={msg("grading.components.GradeDistributionSettings.gradeDistribution")} text={msg("grading.components.GradeDistributionSettings.studentsCanSeeTheGrade")} />
      </span>
      {/* Công tắc tự cho thấy đang lưu; dòng báo chỉ cho trình đọc màn hình để thẻ không xô lên xuống khi lưu. */}
      <p className="sr-only" role="status">{busy ? msg("grading.components.GradeDistributionSettings.savingGradeDistributionSettings") : ""}</p>
    </>}
    {error && <Alert tone="danger" role="alert" action={<Button size="sm" data-testid="grade-distribution-settings-retry" onClick={() => {
      setValue(null);
      setError(null);
      setEpoch((current) => current + 1);
    }}>{msg("grading.components.GradeDistributionSettings.reloadGradeDistributionSettings")}</Button>}>{error}</Alert>}
  </div>;
}
