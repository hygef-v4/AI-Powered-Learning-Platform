"use client";

import { type DiagramType } from "@/shared/document";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Field, InfoTip, Switch, Textarea } from "@/shared/ui";
import { Choice, ChoiceHint } from "./Choice";
import { assignmentFormSections, assignmentSettingsSections } from "../slots";
import { type AssignmentDetail, type GradingMode, type SubmissionMode } from "../model/types";
import { msg } from "@/i18n/msg";

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
  INDIVIDUAL: msg("assignments.components.AssignmentSettingsPanel.eachStudentSubmitsTheirOwn"),
  GROUP: msg("assignments.components.AssignmentSettingsPanel.eachGroupSubmitsOneShared"),
};
const GRADING_HINTS: Record<GradingMode, string> = {
  GRADED: msg("assignments.components.AssignmentSettingsPanel.countsTowardTheGradebook"),
  PRACTICE: msg("assignments.components.AssignmentSettingsPanel.selfPracticeNotGraded"),
  TEMPLATE: msg("assignments.components.AssignmentSettingsPanel.templateHasNoType"),
};

export function readSettings(detail: AssignmentDetail): Settings {
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
  // Đề mẫu của môn không có loại: giảng viên chọn loại khi tạo bài ở lớp (yêu cầu 2026-11-26).
  if (detail.gradingMode === "TEMPLATE") {
    return (
      <div className="flex flex-col gap-4" data-testid="assignment-settings">
        <ChoiceHint>{GRADING_HINTS.TEMPLATE}</ChoiceHint>
      </div>
    );
  }
  return (
    <div className="flex flex-col gap-4" data-testid="assignment-settings">
      <Field label={msg("assignments.components.AssignmentSettingsPanel.format")}>
        <Choice<SubmissionMode> name={msg("assignments.components.AssignmentSettingsPanel.format")} value={detail.submissionMode} disabled onChange={() => {}} options={[
          { value: "INDIVIDUAL", label: msg("assignments.components.AssignmentSettingsPanel.individual") },
          { value: "GROUP", label: msg("assignments.components.AssignmentSettingsPanel.group") },
        ]} />
        <ChoiceHint>{SUBMISSION_HINTS[detail.submissionMode]}</ChoiceHint>
      </Field>
      {group && detail.classId && <SlotOutlet slot={assignmentFormSections} props={{ assignmentId: detail.id, classId: detail.classId }} />}
      <Field label={msg("assignments.components.AssignmentSettingsPanel.mode")}>
        <Choice<GradingMode> name={msg("assignments.components.AssignmentSettingsPanel.mode")} value={value.gradingMode} disabled={readOnly || group}
          onChange={(gradingMode) => onChange({ ...value, gradingMode })} options={[
            { value: "GRADED", label: "Graded" },
            { value: "PRACTICE", label: "Practice" },
          ]} />
        <ChoiceHint key={value.gradingMode}>{GRADING_HINTS[value.gradingMode]}</ChoiceHint>
      </Field>
      {classId && <SlotOutlet slot={assignmentSettingsSections} props={{ assignmentId: detail.id, classId, gradingMode: value.gradingMode }} />}
      <div className="flex items-center gap-1">
        <Switch checked={value.activityMonitoringEnabled} label={msg("assignments.components.AssignmentSettingsPanel.activityMonitoring")}
          disabled={readOnly}
          onChange={(checked) => onChange({ ...value, activityMonitoringEnabled: checked })} />
        <InfoTip label={msg("assignments.components.AssignmentSettingsPanel.activityMonitoring")}
          text={msg("assignments.components.AssignmentSettingsPanel.recordsWhenStudentsLeaveThe")} />
      </div>
      {value.activityMonitoringEnabled && (
        <Field label={msg("assignments.components.AssignmentSettingsPanel.noticeToStudents")} htmlFor="activity-notice" required>
          <Textarea id="activity-notice" rows={2} maxLength={1000} value={value.activityNotice} disabled={readOnly}
            onChange={(e) => onChange({ ...value, activityNotice: e.target.value })} />
        </Field>
      )}
    </div>
  );
}
