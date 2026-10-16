// U08 khai báo. Xem domain-entities U08, mục Contract.

import { createSlot } from "@/shared/slots/registry";

/** Điểm đã công bố và phân bố ẩn danh trên từng dòng Student Assignments (U09). */
export const studentAssignmentResults = createSlot<{ assignmentId: string; classId: string }>(
  "StudentAssignmentResult",
);

/** Điểm và nhận xét của lượt được chấm trên Submission History (U09). */
export const submissionResultSections = createSlot<{ attemptId: string }>("SubmissionResultSection");

/** Nút mở bài nhóm trên Assignment Detail (U12). */
export const groupWorkEntries = createSlot<{ assignmentId: string; classId: string }>("GroupWorkEntry");

/** Tài liệu tham khảo gắn kèm trên Assignment Detail (U13). */
export const assignmentMaterialSections = createSlot<{ classId: string; materialIds: string[] }>(
  "AssignmentMaterialSection",
);
