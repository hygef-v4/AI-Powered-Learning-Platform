"use client";

import { useEffect, useId, useState } from "react";
import { Alert, Button, DateInput, Dialog, Field, Switch, TextInput } from "@/shared/ui";
import { getFormation, updateFormation } from "../api";
import {
  convertFromLocalInput,
  MAX_GROUP_SIZE,
  MIN_GROUP_SIZE,
  describeProposalError,
  convertToLocalInput,
  validateFormation,
  type FormationErrors,
} from "../model/proposals";
import type { FormationView } from "../model/types";
import { msg } from "@/i18n/msg";

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
  const [deadlineDate, setDeadlineDate] = useState("");
  const [deadlineTime, setDeadlineTime] = useState("00:00");
  const [dateValid, setDateValid] = useState(true);
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
      const local = convertToLocalInput(view.deadline);
      setDeadlineDate(local.slice(0, 10));
      setDeadlineTime(local.slice(11, 16) || "00:00");
      setDateValid(true);
    }, (e) => { if (active) setError(describeProposalError(e, msg("groups.components.SelfGroupingSettingsDialog.khongTaiDuocCaiDat"))); });
    return () => { active = false; };
  }, [classId]);

  const saveSettings = async () => {
    if (!loaded || !dateValid) return;
    const deadline = deadlineDate ? `${deadlineDate}T${deadlineTime || "00:00"}` : "";
    const checked = validateFormation({ enabled, minSize, maxSize, deadline });
    setErrors(checked);
    if (Object.keys(checked).length > 0) return;
    setBusy(true);
    setError(null);
    try {
      const saved = await updateFormation(classId, {
        enabled, minSize: Number(minSize), maxSize: Number(maxSize), deadline: convertFromLocalInput(deadline), version: loaded.version,
      });
      onSaved(saved);
    } catch (e) {
      setError(describeProposalError(e, msg("groups.components.SelfGroupingSettingsDialog.khongLuuDuocCaiDat")));
      setBusy(false);
    }
  };

  return (
    <Dialog open title={msg("groups.components.SelfGroupingSettingsDialog.sinhVienTuChiaNhom")} onClose={onClose} dismissible={!busy}
      footer={<Button variant="primary" disabled={busy || !loaded || !dateValid} onClick={() => void saveSettings()} data-testid="formation-save">{msg("groups.components.SelfGroupingSettingsDialog.luu")}</Button>}>
      {!loaded && !error ? (
        <p role="status" className="text-[13px] text-muted">{msg("groups.components.SelfGroupingSettingsDialog.dangTaiCaiDat")}</p>
      ) : (
        <form className="flex flex-col gap-4" onSubmit={(event) => { event.preventDefault(); void saveSettings(); }}>
          <Switch checked={enabled} onChange={setEnabled} label={msg("groups.components.SelfGroupingSettingsDialog.choSinhVienTuChiaNhom")} disabled={busy || !loaded} />
          <p className="text-[13px] text-muted">
            {msg("groups.components.SelfGroupingSettingsDialog.sinhVienChuaCoNhomTu")}</p>
          <div className="grid gap-3 sm:grid-cols-2">
            <Field label={msg("groups.components.SelfGroupingSettingsDialog.siSoToiThieu")} htmlFor={`${fieldId}-min`} required error={errors.minSize}>
              <TextInput id={`${fieldId}-min`} type="number" inputMode="numeric" min={MIN_GROUP_SIZE} max={MAX_GROUP_SIZE}
                value={minSize} invalid={!!errors.minSize} disabled={busy}
                onChange={(event) => { setMinSize(event.target.value); setErrors({}); }} data-testid="formation-min" />
            </Field>
            <Field label={msg("groups.components.SelfGroupingSettingsDialog.siSoToiDa")} htmlFor={`${fieldId}-max`} required error={errors.maxSize}>
              <TextInput id={`${fieldId}-max`} type="number" inputMode="numeric" min={MIN_GROUP_SIZE} max={MAX_GROUP_SIZE}
                value={maxSize} invalid={!!errors.maxSize} disabled={busy}
                onChange={(event) => { setMaxSize(event.target.value); setErrors({}); }} data-testid="formation-max" />
            </Field>
          </div>
          <Field label={msg("groups.components.SelfGroupingSettingsDialog.hanChotTuyChonGioViet")} htmlFor={`${fieldId}-deadline`} error={errors.deadline}>
            <div className="flex gap-2">
              <DateInput id={`${fieldId}-deadline`} value={deadlineDate} invalid={!!errors.deadline}
                disabled={busy} wrapperClassName="flex-1" onValidityChange={setDateValid}
                onChange={(date) => { setDeadlineDate(date); setErrors({}); }} data-testid="formation-deadline" />
              <TextInput type="time" aria-label={msg("groups.components.SelfGroupingSettingsDialog.hanChotGio")} value={deadlineTime}
                disabled={busy} wrapperClassName="w-28"
                onChange={(event) => { setDeadlineTime(event.target.value); setErrors({}); }}
                data-testid="formation-deadline-time" />
            </div>
          </Field>
          <p className="text-xs text-muted">{msg("groups.components.SelfGroupingSettingsDialog.deXuatDangChoDuyetVan")}</p>
        </form>
      )}
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
    </Dialog>
  );
}
