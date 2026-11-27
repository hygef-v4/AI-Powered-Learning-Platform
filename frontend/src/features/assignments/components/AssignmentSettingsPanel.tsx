"use client";

import { type DiagramType } from "@/shared/document";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Field, InfoTip, Switch, Textarea } from "@/shared/ui";
import { Choice, ChoiceHint } from "./Choice";
import { assignmentFormSections, assignmentSettingsSections } from "../slots";
import { type AssignmentDetail, type GradingMode, type SubmissionMode } from "../model/types";
import { msg } from "@/i18n/msg";

export type Settings = {
  submissionMode: SubmissionMode;
  gradingMode: GradingMode;
  diagramEnabled: boolean;
  requiredDiagrams: Partial<Record<DiagramType, number>>;
  activityMonitoringEnabled: boolean;
  activityNotice: string;
  referenceMaterialIds: string[];
};

/** Loại bài gộp Hình thức và Chế độ (yêu cầu 2026-11-27): Cá nhân, Nhóm (đều tính điểm) hoặc Luyện tập. */
export type Kind = "INDIVIDUAL" | "GROUP" | "PRACTICE";

/** Một câu ngắn dưới mỗi lựa chọn, giống popup tạo bài (yêu cầu 2026-11-10). */
const KIND_HINTS: Record<Kind, string> = {
  INDIVIDUAL: msg("assignments.components.AssignmentSettingsPanel.eachStudentSubmitsTheirOwn"),
  GROUP: msg("assignments.components.AssignmentSettingsPanel.eachGroupSubmitsOneShared"),
  PRACTICE: msg("assignments.components.AssignmentSettingsPanel.selfPracticeNotGraded"),
};

export function readKind(value: Pick<Settings, "submissionMode" | "gradingMode">): Kind {
  return value.gradingMode === "PRACTICE" ? "PRACTICE" : value.submissionMode;
}

function applyKind(value: Settings, kind: Kind): Settings {
  if (kind === "PRACTICE") return { ...value, submissionMode: "INDIVIDUAL", gradingMode: "PRACTICE" };
  return { ...value, submissionMode: kind, gradingMode: "GRADED" };
}

export function readSettings(detail: AssignmentDetail): Settings {
  return {
    submissionMode: detail.submissionMode,
    gradingMode: detail.gradingMode,
    diagramEnabled: detail.diagramEnabled,
    requiredDiagrams: detail.requiredDiagrams ?? {},
    activityMonitoringEnabled: detail.activityMonitoringEnabled,
    activityNotice: detail.activityNotice ?? "",
    referenceMaterialIds: detail.referenceMaterialIds ?? [],
  };
}

/**
 * Popup Cài đặt bài (BR-U07-13, 16) giống popup tạo bài (yêu cầu 2026-10-22): một thẻ chọn Loại (Cá nhân, Nhóm,
 * Luyện tập) đổi qua lại tự do khi bài còn sửa được (yêu cầu 2026-11-27); nút gạt Ghi nhận hoạt động kèm lời báo. Không còn mục sơ đồ bắt buộc và học liệu gắn kèm: giá trị đã
 * có giữ nguyên khi lưu. Sơ đồ luôn dùng được (yêu cầu 2026-10-21 thay BR-U07-14).
 */
export function AssignmentSettingsPanel({ detail, value, onChange, readOnly, classId = detail.classId }: {
  detail: AssignmentDetail;
  value: Settings;
  onChange: (value: Settings) => void;
  readOnly: boolean;
  classId?: string | null;
}) {
  const kind = readKind(value);
  // Bài đang là nhóm thì vẫn giữ được Nhóm dù lớp tạm thời chưa dùng được bài nhóm.
  const groupAllowed = detail.groupSupported || detail.submissionMode === "GROUP";
  // Đề mẫu của môn không có loại: giảng viên chọn loại khi tạo bài ở lớp (yêu cầu 2026-11-26).
  if (detail.gradingMode === "TEMPLATE") {
    return (
      <div className="flex flex-col gap-4" data-testid="assignment-settings">
        <ChoiceHint>{msg("assignments.components.AssignmentSettingsPanel.templateHasNoType")}</ChoiceHint>
      </div>
    );
  }
  return (
    <div className="flex flex-col gap-4" data-testid="assignment-settings">
      <Field label={msg("assignments.components.AssignmentSettingsPanel.type")}>
        <Choice<Kind> name={msg("assignments.components.AssignmentSettingsPanel.type")} value={kind} disabled={readOnly}
          onChange={(next) => onChange(applyKind(value, next))} options={[
            { value: "INDIVIDUAL", label: msg("assignments.components.AssignmentSettingsPanel.individual") },
            { value: "GROUP", label: msg("assignments.components.AssignmentSettingsPanel.group"), disabled: !groupAllowed,
              title: groupAllowed ? undefined : msg("assignments.components.AssignmentSettingsPanel.thisClassCannotUseGroup") },
            { value: "PRACTICE", label: msg("assignments.components.AssignmentSettingsPanel.practice") },
          ]} />
        <ChoiceHint key={kind}>{KIND_HINTS[kind]}</ChoiceHint>
      </Field>
      {kind === "GROUP" && detail.classId && <SlotOutlet slot={assignmentFormSections} props={{ assignmentId: detail.id, classId: detail.classId }} />}
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
