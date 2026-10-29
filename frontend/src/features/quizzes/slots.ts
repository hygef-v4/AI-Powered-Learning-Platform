// U10 khai báo: lượt làm và dữ liệu hoạt động trên Quiz Detail cho giảng viên (U11).

import { createSlot } from "@/shared/slots/registry";

export const quizDetailSections = createSlot<{ quizId: string; classId?: string }>("QuizDetailSection");
