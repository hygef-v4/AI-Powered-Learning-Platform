// U08 khai báo. Xem domain-entities U08, mục Contract.

import { createRegistry, createSlot } from "@/shared/slots/registry";

/** Điểm đã công bố và phân bố ẩn danh trên từng dòng Student Assignments (U09). */
export const studentAssignmentResults = createSlot<{ assignmentId: string; classId: string }>(
  "StudentAssignmentResult",
);

/** Một mục rubric đã chấm của lượt: phần (heading) chứa nó, điểm đạt / tối đa, nhận xét đã duyệt. */
export type AttemptResultItem = {
  rubricId: string;
  partId: string;
  criterionTitle: string;
  itemText: string;
  awarded: number;
  max: number;
  evidence: string | null;
  reason: string | null;
  suggestion: string | null;
};

/** Kết quả đã công bố của một lượt: điểm tổng, nhận xét chung, tóm tắt (làm tốt, cần sửa, bước tiếp) và các mục rubric. */
export type AttemptResult = {
  score: number | null;
  maxScore: number | null;
  publishedAt: string | null;
  updated: boolean;
  memberOverride: boolean;
  feedback: string | null;
  summary: { strengths: string[]; improvements: string[]; nextSteps: string[] } | null;
  items: AttemptResultItem[];
};

/**
 * Nguồn kết quả của lượt được chấm (U09 đăng ký): trang Assignment Detail hiện điểm tổng và điểm từng bài, trang Submission
 * History gắn nhận xét bên lề tài liệu như comment.
 */
export const attemptResultSources = createRegistry<{ key: string; order?: number; load: (attemptId: string) => Promise<AttemptResult | null> }>(
  "AttemptResultSource",
);

/** Nút mở bài nhóm trên Assignment Detail (U12). */
export const groupWorkEntries = createSlot<{ assignmentId: string; classId: string }>("GroupWorkEntry");

/** Tài liệu tham khảo gắn kèm trên Assignment Detail (U13). */
export const assignmentMaterialSections = createSlot<{ classId: string; materialIds: string[] }>(
  "AssignmentMaterialSection",
);
