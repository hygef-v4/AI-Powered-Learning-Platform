"use client";

import { useEffect, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Switch } from "@/shared/ui";
import { gradingApi, type GradeDistributionVisibility } from "../api";

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
    gradingApi.distributionVisibility(classId, assignmentId, controller.signal).then(
      (next) => { if (!controller.signal.aborted) { setValue(next); setError(null); } },
      (cause) => {
        if (controller.signal.aborted) return;
        if (cause instanceof ApiError && [401, 403, 404].includes(cause.problem.status)) return;
        setError("Không tải được cài đặt phổ điểm. Vui lòng tải lại để thử lại.");
      },
    );
    return () => controller.abort();
  }, [classId, assignmentId, epoch]);

  async function save(enabled: boolean) {
    if (!value?.canEdit || busy || error) return;
    setBusy(true);
    try {
      setValue(await gradingApi.saveDistributionVisibility(classId, assignmentId, enabled, value.version));
    } catch (cause) {
      if (cause instanceof ApiError && [401, 403, 404].includes(cause.problem.status)) setValue(null);
      setError(cause instanceof ApiError && cause.problem.status === 409
        ? "Cài đặt phổ điểm đã thay đổi ở nơi khác. Tải lại trước khi thay đổi."
        : "Chưa lưu được cài đặt phổ điểm. Tải lại để kiểm tra trạng thái rồi thử lại.");
    } finally {
      setBusy(false);
    }
  }

  if (!error && !value?.canEdit) return null;
  return <div className="flex flex-col gap-1" data-testid="grade-distribution-settings">
    {value?.canEdit && <>
      <Switch checked={value.enabled} label="Cho sinh viên xem phổ điểm ẩn danh" ariaLabel="Cho sinh viên xem phổ điểm ẩn danh"
        disabled={busy || !!error} onChange={(enabled) => save(enabled)} />
      <p className="text-[11px] text-muted">Chỉ hiển thị điểm đã công bố khi có ít nhất 20 kết quả. Cài đặt riêng cho bài này trong lớp, được lưu ngay.</p>
      {/* Công tắc tự cho thấy đang lưu; dòng báo chỉ cho trình đọc màn hình để thẻ không xô lên xuống khi lưu. */}
      <p className="sr-only" role="status">{busy ? "Đang lưu cài đặt phổ điểm…" : ""}</p>
    </>}
    {error && <Alert tone="danger" role="alert" action={<Button size="sm" data-testid="grade-distribution-settings-retry" onClick={() => {
      setValue(null);
      setError(null);
      setEpoch((current) => current + 1);
    }}>Tải lại cài đặt phổ điểm</Button>}>{error}</Alert>}
  </div>;
}
