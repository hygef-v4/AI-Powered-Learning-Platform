"use client";

import { type DiagramType } from "@/shared/document";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Field, InfoTip, Switch } from "@/shared/ui";
import { Choice, ChoiceHint } from "./Choice";
import { assignmentFormSections, assignmentSettingsSections } from "../slots";
import { type AssignmentDetail, type GradingMode, type SubmissionMode } from "../model/types";

export type Settings = {
  gradingMode: GradingMode;
  diagramEnabled: boolean;
  requiredDiagrams: Partial<Record<DiagramType, number>>;
  activityMonitoringEnabled: boolean;
  activityNotice: string;
  referenceMaterialIds: string[];
};

/** Một câu ngắn dưới mỗi lựa chọn, giống popup tạo bài (yêu cầu 2026-11-10). */
const SUBMISSION_HINTS: Record<SubmissionMode, string> = {
  INDIVIDUAL: "Mỗi sinh viên nộp một bài.",
  GROUP: "Mỗi nhóm nộp chung một bài.",
};
const GRADING_HINTS: Record<GradingMode, string> = {
  GRADED: "Tính vào sổ điểm.",
  PRACTICE: "Tự luyện, không tính điểm.",
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
 * Popup Cài đặt bài (BR-U07-13, 16) giống popup tạo bài (yêu cầu 2026-10-22): thẻ chọn Hình thức (chọn khi tạo, chỉ
 * xem), Chế độ; nút gạt Ghi nhận hoạt động kèm lời báo. Không còn mục sơ đồ bắt buộc và học liệu gắn kèm: giá trị đã
 * có giữ nguyên khi lưu. Sơ đồ luôn dùng được (yêu cầu 2026-10-21 thay BR-U07-14).
 */
export function AssignmentSettingsPanel({ detail, value, onChange, readOnly, classId = detail.classId }: {
  detail: AssignmentDetail;
  value: Settings;
  onChange: (value: Settings) => void;
  readOnly: boolean;
  classId?: string | null;
}) {
  const group = detail.submissionMode === "GROUP";
  return (
    <div className="flex flex-col gap-4" data-testid="assignment-settings">
      <Field label="Hình thức">
        <Choice<SubmissionMode> name="Hình thức" value={detail.submissionMode} disabled onChange={() => {}} options={[
          { value: "INDIVIDUAL", label: "Cá nhân" },
          { value: "GROUP", label: "Nhóm" },
        ]} />
        <ChoiceHint>{SUBMISSION_HINTS[detail.submissionMode]}</ChoiceHint>
      </Field>
      {group && detail.classId && <SlotOutlet slot={assignmentFormSections} props={{ assignmentId: detail.id, classId: detail.classId }} />}
      <Field label="Chế độ">
        <Choice<GradingMode> name="Chế độ" value={value.gradingMode} disabled={readOnly || group}
          onChange={(gradingMode) => onChange({ ...value, gradingMode })} options={[
            { value: "GRADED", label: "Graded" },
            { value: "PRACTICE", label: "Practice" },
          ]} />
        <ChoiceHint key={value.gradingMode}>{GRADING_HINTS[value.gradingMode]}</ChoiceHint>
      </Field>
      {classId && <SlotOutlet slot={assignmentSettingsSections} props={{ assignmentId: detail.id, classId, gradingMode: value.gradingMode }} />}
      <div className="flex items-center gap-1">
        <Switch checked={value.activityMonitoringEnabled} label="Ghi nhận hoạt động"
          disabled={readOnly}
          onChange={(checked) => onChange({ ...value, activityMonitoringEnabled: checked })} />
        <InfoTip label="Ghi nhận hoạt động"
          text="Ghi lại khi sinh viên rời trang lúc làm bài." />
      </div>
      {value.activityMonitoringEnabled && (
        <Field label="Lời báo cho sinh viên" htmlFor="activity-notice" required>
          <textarea id="activity-notice" rows={2} maxLength={1000} value={value.activityNotice} disabled={readOnly}
            onChange={(e) => onChange({ ...value, activityNotice: e.target.value })}
            className="w-full rounded-lg border border-field px-3 py-2 text-[13px] outline-none focus:outline-2 focus:outline-focus" />
        </Field>
      )}
    </div>
  );
}
