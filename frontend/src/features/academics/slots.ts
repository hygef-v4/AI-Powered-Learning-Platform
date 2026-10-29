// U05 khai báo; U07–U14 đăng ký vào. Xem domain-entities U05, mục Contract frontend.

import { createSlot } from "@/shared/slots/registry";

/** Phần nội dung trên Student Class Detail (U13, U08, U14, U12). */
export const studentClassSections = createSlot<{ classId: string }>("StudentClassSection");

/** Tab hoặc phần trên Teacher Class Detail: Thông tin lớp, Sinh viên, Học liệu, Đánh giá, Sổ điểm. */
export const teacherClassTabs = createSlot<{
  classId: string;
  tab: "info" | "students" | "materials" | "evals" | "gradebook";
}>("TeacherClassTab");

/** Phần trong popup Student Detail (U12: nhóm; U09: bài nộp và điểm). */
export const studentDetailSections = createSlot<{ classId: string; studentId: string }>("StudentDetailSection");

/** Số liệu theo môn (U13, U10, U07): dòng trên thẻ môn của Manager Dashboard hoặc thẻ trên My Subject. */
export const subjectSummarySections = createSlot<{ subjectId: string; variant: "inline" | "card" }>(
  "SubjectSummarySection",
);

/** Dòng tóm tắt trên thẻ lớp ở Class Dashboard (U08: bài sắp đến hạn). */
export const classCardSections = createSlot<{ classId: string }>("ClassCardSection");
