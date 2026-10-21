"use client";

import { useEffect, useState } from "react";
import { diagramTypeLabels, type DiagramType } from "@/shared/document";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Button, Field, Select, Switch, TextInput } from "@/shared/ui";
import { assignmentsApi } from "./api";
import { assignmentFormSections } from "./slots";
import { gradingLabels, submissionLabels, type AssignmentDetail, type GradingMode, type Material } from "./types";

export type Settings = {
  gradingMode: GradingMode;
  diagramEnabled: boolean;
  requiredDiagrams: Partial<Record<DiagramType, number>>;
  activityMonitoringEnabled: boolean;
  activityNotice: string;
  referenceMaterialIds: string[];
};

export function settingsOf(detail: AssignmentDetail): Settings {
  return {
    gradingMode: detail.gradingMode,
    diagramEnabled: detail.diagramEnabled,
    requiredDiagrams: detail.requiredDiagrams ?? {},
    activityMonitoringEnabled: detail.activityMonitoringEnabled,
    activityNotice: detail.activityNotice ?? "",
    referenceMaterialIds: detail.referenceMaterialIds ?? [],
  };
}

/**
 * Cài đặt bài trong popup Cài đặt (BR-U07-13, 16, 17): chế độ, số sơ đồ bắt buộc, ghi nhận hoạt động, học liệu. Không
 * còn công tắc sơ đồ: sơ đồ luôn dùng được (yêu cầu 2026-10-21 thay BR-U07-14).
 */
export function AssignmentSettingsPanel({ detail, value, onChange, readOnly }: {
  detail: AssignmentDetail;
  value: Settings;
  onChange: (value: Settings) => void;
  readOnly: boolean;
}) {
  const [materials, setMaterials] = useState<Material[]>([]);
  useEffect(() => {
    if (readOnly || !detail.permissions.canManage) return;
    const controller = new AbortController();
    assignmentsApi.materials(detail.id, controller.signal).then((list) => {
      if (!controller.signal.aborted) setMaterials(list);
    }, () => {});
    return () => controller.abort();
  }, [detail.id, readOnly, detail.permissions.canManage]);

  const required = Object.entries(value.requiredDiagrams) as [DiagramType, number][];
  const unused = (Object.keys(diagramTypeLabels) as DiagramType[]).filter((type) => !(type in value.requiredDiagrams));
  const group = detail.submissionMode === "GROUP";

  return (
    <div className="flex flex-col gap-4" data-testid="assignment-settings">
      <Field label="Chế độ" hint="Graded vào sổ điểm; Practice cho sinh viên nhận góp ý AI.">
        <Select aria-label="Chế độ" value={value.gradingMode} disabled={readOnly || group}
          onChange={(e) => onChange({ ...value, gradingMode: e.target.value as GradingMode })}>
          {(Object.keys(gradingLabels) as GradingMode[]).map((mode) => <option key={mode} value={mode}>{gradingLabels[mode]}</option>)}
        </Select>
      </Field>
      <Field label="Hình thức">
        <p className="text-[13px] text-ink">
          {submissionLabels[detail.submissionMode]}
          {detail.scope === "SUBJECT" ? " · bài của môn chỉ có cá nhân" : group ? " · theo nhóm cố định của lớp, trưởng nhóm nộp" : ""}
        </p>
      </Field>
      {group && detail.classId && <SlotOutlet slot={assignmentFormSections} props={{ assignmentId: detail.id, classId: detail.classId }} />}
      <Field label="Số sơ đồ bắt buộc theo loại" hint="Tùy chọn; 1–20 mỗi loại; kiểm khi sinh viên nộp. Sơ đồ trống giảng viên chèn vào phần cũng tính khi sinh viên vẽ.">
        <div className="flex flex-col gap-1.5">
          {required.map(([type, count]) => (
            <div key={type} className="flex items-center gap-2">
              <span className="w-40 text-[13px]">{diagramTypeLabels[type]}</span>
              <TextInput aria-label={`Số sơ đồ ${diagramTypeLabels[type]}`} type="number" min={1} max={20} value={count} disabled={readOnly}
                wrapperClassName="w-20" onChange={(e) => onChange({ ...value, requiredDiagrams: { ...value.requiredDiagrams, [type]: Math.min(20, Math.max(1, Number(e.target.value) || 1)) } })} />
              {!readOnly && <Button size="sm" variant="ghost" onClick={() => {
                const next = { ...value.requiredDiagrams };
                delete next[type];
                onChange({ ...value, requiredDiagrams: next });
              }}>Bỏ</Button>}
            </div>
          ))}
          {!readOnly && unused.length > 0 && (
            <Select aria-label="Thêm loại sơ đồ bắt buộc" value="" wrapperClassName="w-56"
              onChange={(e) => { if (e.target.value) onChange({ ...value, requiredDiagrams: { ...value.requiredDiagrams, [e.target.value]: 1 } }); }}>
              <option value="">+ Thêm loại sơ đồ</option>
              {unused.map((type) => <option key={type} value={type}>{diagramTypeLabels[type]}</option>)}
            </Select>
          )}
        </div>
      </Field>
      <Field label="Ghi nhận hoạt động"
        hint={detail.activityRetentionConfigured ? "Ghi số lần, thời gian sinh viên rời trang khi làm bài; chỉ là dấu hiệu, không kết luận gian lận."
          : "Admin chưa đặt thời hạn giữ dữ liệu hoạt động nên chưa bật được."}>
        <Switch checked={value.activityMonitoringEnabled} label="Ghi nhận hoạt động"
          disabled={readOnly || (!detail.activityRetentionConfigured && !value.activityMonitoringEnabled)}
          onChange={(checked) => onChange({ ...value, activityMonitoringEnabled: checked })} />
      </Field>
      {value.activityMonitoringEnabled && (
        <Field label="Lời báo cho sinh viên" htmlFor="activity-notice" required>
          <textarea id="activity-notice" rows={2} maxLength={1000} value={value.activityNotice} disabled={readOnly}
            onChange={(e) => onChange({ ...value, activityNotice: e.target.value })}
            className="w-full rounded-[9px] border border-line-strong px-3 py-2 text-[13px] outline-none focus:outline-2 focus:outline-primary" />
        </Field>
      )}
      {materials.length > 0 && (
        <Field label="Học liệu gắn kèm" hint="Tối đa 10.">
          <div className="flex flex-col gap-1">
            {materials.map((m) => (
              <label key={m.materialId} className="flex items-center gap-2 text-[13px]">
                <input type="checkbox" disabled={readOnly} checked={value.referenceMaterialIds.includes(m.materialId)}
                  onChange={(e) => onChange({ ...value, referenceMaterialIds: e.target.checked
                    ? [...value.referenceMaterialIds, m.materialId].slice(0, 10)
                    : value.referenceMaterialIds.filter((id) => id !== m.materialId) })} />
                {m.title}
              </label>
            ))}
          </div>
        </Field>
      )}
    </div>
  );
}
