// U05 khai báo; U07–U14 đăng ký vào. Xem domain-entities U05, mục Contract frontend.

import { createSlot } from "@/shared/slots/registry";

/** Mục Học liệu của trang lớp sinh viên (U13). */
export const studentClassSections = createSlot<{ classId: string }>("StudentClassSection");

/** Thẻ ở cột phải trang lớp sinh viên, dưới thẻ Giảng viên (U12: Nhóm của tôi). */
export const studentClassAside = createSlot<{ classId: string }>("StudentClassAside");

/** Hàng thẻ chia đôi dưới Thông tin lớp của trang lớp sinh viên (U14: thông báo gần đây; U08: bài cần làm). */
export const studentClassBoard = createSlot<{ classId: string }>("StudentClassBoard");

/** Tab hoặc phần trên Teacher Class Detail: Thông tin lớp, Sinh viên, Học liệu, Bài tập (`evals`), Chấm điểm. */
export const teacherClassTabs = createSlot<{
  classId: string;
  tab: "info" | "students" | "materials" | "evals" | "gradebook";
  /** U12 báo tên nhóm của từng sinh viên đang học và danh sách trưởng nhóm (hiện trong bảng sinh viên). */
  onGroupNamesLoaded?: (groupNames: Record<string, string>, leaders?: string[]) => void;
  /** Chỗ nút thao tác của tab trên dòng tiêu đề trang lớp (Tạo bài tập, Xuất bảng điểm), để bảng lên sát tiêu đề. */
  headerActions?: HTMLElement | null;
  /** Sinh viên đang học (tab Sinh viên), để U12 dựng bảng sinh viên kèm nhóm. */
  students?: { accountId: string; name: string; email: string; enrolledAt: string }[];
  /** Mở popup Student Detail của một sinh viên. */
  onViewStudent?: (accountId: string) => void;
  /** U12 báo đang hiện bảng sinh viên kèm nhóm của mình: trang ẩn bảng sinh viên chỉ xem cho khỏi lặp. */
  onStudentTable?: (shown: boolean) => void;
}>("TeacherClassTab");

/** Phần trong popup Student Detail (U12: nhóm; U09: bài nộp và điểm). */
export const studentDetailSections = createSlot<{ classId: string; studentId: string }>("StudentDetailSection");

/** Dòng tóm tắt trên thẻ lớp ở Class Dashboard (U08: bài sắp đến hạn). */
export const classCardSections = createSlot<{ classId: string }>("ClassCardSection");

/** Ô thêm trong thẻ Thông tin lớp của giảng viên, sau ô Sinh viên (U13: số học liệu đang hoạt động). */
export const teacherClassInfoFields = createSlot<{ classId: string }>("TeacherClassInfoField");
