"use client";

import { useEffect, useId, useState } from "react";
import { Alert, Button, Dialog, Field, Switch, TextInput } from "@/shared/ui";
import { getFormation, updateFormation } from "../api";
import {
  fromLocalInput,
  MAX_GROUP_SIZE,
  MIN_GROUP_SIZE,
  proposalErrorText,
  toLocalInput,
  validateFormation,
  type FormationErrors,
} from "../model/proposals";
import type { FormationView } from "../model/types";

/**
 * Popup cài đặt đợt sinh viên tự chia nhóm (US-GRP-007, BR-U12-70): bật/tắt, sĩ số tối thiểu và tối đa (2–20), hạn chót tùy
 * chọn theo giờ Việt Nam. Lỗi hiện dưới đúng ô; backend kiểm lại và giữ `version`.
 */
export function SelfGroupingSettingsDialog({ classId, onClose, onSaved }: {
  classId: string;
  onClose: () => void;
  onSaved: (view: FormationView) => void;
}) {
  const fieldId = useId();
  const [loaded, setLoaded] = useState<FormationView | null>(null);
  const [enabled, setEnabled] = useState(false);
  const [minSize, setMinSize] = useState("");
  const [maxSize, setMaxSize] = useState("");
  const [deadline, setDeadline] = useState("");
  const [errors, setErrors] = useState<FormationErrors>({});
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let active = true;
    getFormation(classId).then((view) => {
      if (!active) return;
      setLoaded(view);
      setEnabled(view.enabled);
      setMinSize(String(view.minSize));
      setMaxSize(String(view.maxSize));
      setDeadline(toLocalInput(view.deadline));
    }, (e) => { if (active) setError(proposalErrorText(e, "Không tải được cài đặt.")); });
    return () => { active = false; };
  }, [classId]);

  const save = async () => {
    if (!loaded) return;
    const checked = validateFormation({ enabled, minSize, maxSize, deadline });
    setErrors(checked);
    if (Object.keys(checked).length > 0) return;
    setBusy(true);
    setError(null);
    try {
      const saved = await updateFormation(classId, {
        enabled, minSize: Number(minSize), maxSize: Number(maxSize), deadline: fromLocalInput(deadline), version: loaded.version,
      });
      onSaved(saved);
    } catch (e) {
      setError(proposalErrorText(e, "Không lưu được cài đặt."));
      setBusy(false);
    }
  };

  return (
    <Dialog open title="Sinh viên tự chia nhóm" onClose={onClose} dismissible={!busy}
      footer={<Button variant="primary" disabled={busy || !loaded} onClick={() => void save()} data-testid="formation-save">Lưu</Button>}>
      {!loaded && !error ? (
        <p role="status" className="text-[13px] text-muted">Đang tải cài đặt…</p>
      ) : (
        <form className="flex flex-col gap-4" onSubmit={(event) => { event.preventDefault(); void save(); }}>
          <Switch checked={enabled} onChange={setEnabled} label="Cho sinh viên tự chia nhóm" disabled={busy || !loaded} />
          <p className="text-[13px] text-muted">
            Sinh viên chưa có nhóm tự lập đề xuất và mời bạn; bạn duyệt thì đề xuất thành nhóm chính thức.
          </p>
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label="Sĩ số tối thiểu" htmlFor={`${fieldId}-min`} required error={errors.minSize}>
              <TextInput id={`${fieldId}-min`} type="number" inputMode="numeric" min={MIN_GROUP_SIZE} max={MAX_GROUP_SIZE}
                value={minSize} invalid={!!errors.minSize} disabled={busy}
                onChange={(event) => { setMinSize(event.target.value); setErrors({}); }} data-testid="formation-min" />
            </Field>
            <Field label="Sĩ số tối đa" htmlFor={`${fieldId}-max`} required error={errors.maxSize}>
              <TextInput id={`${fieldId}-max`} type="number" inputMode="numeric" min={MIN_GROUP_SIZE} max={MAX_GROUP_SIZE}
                value={maxSize} invalid={!!errors.maxSize} disabled={busy}
                onChange={(event) => { setMaxSize(event.target.value); setErrors({}); }} data-testid="formation-max" />
            </Field>
          </div>
          <Field label="Hạn chót (tùy chọn, giờ Việt Nam)" htmlFor={`${fieldId}-deadline`} error={errors.deadline}>
            <TextInput id={`${fieldId}-deadline`} type="datetime-local" value={deadline} invalid={!!errors.deadline} disabled={busy}
              onChange={(event) => { setDeadline(event.target.value); setErrors({}); }} data-testid="formation-deadline" />
          </Field>
          <p className="text-xs text-muted">Đề xuất đang chờ duyệt vẫn duyệt được sau khi đóng đợt.</p>
        </form>
      )}
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
    </Dialog>
  );
}
