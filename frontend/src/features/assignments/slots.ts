// U07 khai báo. Xem domain-entities U07, mục Contract.

import { createSlot } from "@/shared/slots/registry";
import type { GradingMode } from "./model/types";

/** Lối vào bài nộp trên từng dòng bài (U09). */
export const assignmentRowActions = createSlot<{ assignmentId: string; classId?: string; practice?: boolean }>("AssignmentRowAction");

/** Phần nhóm trên Assignment Form của bài GROUP (U12). */
export const assignmentFormSections = createSlot<{ assignmentId?: string; classId: string }>(
  "AssignmentFormSection",
);

/** Cài đặt theo lớp, độc lập với quyền sửa nội dung bài. */
export const assignmentSettingsSections = createSlot<{ assignmentId: string; classId: string; gradingMode: GradingMode }>(
  "AssignmentSettingsSection",
);
