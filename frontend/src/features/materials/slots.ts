// U13 khai báo: danh sách Quiz gắn tài liệu kèm nút Làm Quiz trên Learning Material (U11).

import { createSlot } from "@/shared/slots/registry";

export const learningMaterialSections = createSlot<{ materialId: string; classId: string }>(
  "LearningMaterialSection",
);
