// U11 khai báo: tài liệu gắn kèm Quiz trên Quiz Practice Detail (U13).

import { createSlot } from "@/shared/slots/registry";

export const quizMaterialSections = createSlot<{ classId: string; materialIds: string[] }>("QuizMaterialSection");
