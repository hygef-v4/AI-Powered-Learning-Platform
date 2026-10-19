import type { CourseClassView, EnrollmentView, PersonView, SubjectView } from "../types";

/** Chỉ dùng trong preview development; không phải tài khoản/lớp thật. */
export const subjects: SubjectView[] = [
  { id: "subject-swp", code: "SWP391", name: "Software Development Project", description: "Phát triển phần mềm theo nhóm, từ phân tích yêu cầu đến triển khai sản phẩm." },
  { id: "subject-swr", code: "SWR302", name: "Software Requirements", description: "Phân tích và đặc tả yêu cầu phần mềm." },
];
export const teachers: PersonView[] = [
  { id: "teacher-1", name: "Nguyễn Minh Kiên", email: "kiennm@example.test" },
  { id: "teacher-2", name: "Ngô Thị Lan", email: "lannt@example.test" },
  { id: "teacher-3", name: "Phạm Minh Đức", email: "ducpm@example.test" },
  { id: "teacher-4", name: "Trần Thị Hoa", email: "hoatt@example.test" },
];
export const studentCandidates: PersonView[] = [
  { id: "student-1", name: "Võ Minh Tài", email: "taivm@example.test" },
  { id: "student-2", name: "Trần Đức Huy", email: "huytd@example.test" },
  { id: "student-3", name: "Lê Hoàng Nam", email: "namlh@example.test" },
  { id: "student-4", name: "Phạm Thu Hà", email: "hapt@example.test" },
  { id: "student-5", name: "Bùi Hải Yến", email: "yenbh@example.test" },
  { id: "student-6", name: "Nguyễn Văn An", email: "annv@example.test" },
];
function createClass(id: string, code: string, name: string, status: CourseClassView["status"], teacherIndex: number | null, subjectIndex = 0): CourseClassView {
  return { id, subject: subjects[subjectIndex]!, code, name, status, term: status === "ARCHIVED" ? "2025-3" : status === "DRAFT" ? "2026-2" : "2026-1", description: "Lớp học sáng thứ Hai, phòng BE-208.", teacher: teacherIndex === null ? null : teachers[teacherIndex]!, activeStudentCount: 0, removedStudentCount: 0, createdAt: "2026-09-01", showGradeDistribution: false, version: 1 };
}
export const initialClasses: CourseClassView[] = [
  createClass("class-1", "SE1801", "SWP391 · Nhóm thứ Hai", "OPEN", 0),
  createClass("class-2", "SE1802", "SWP391 · Nhóm thứ Ba", "OPEN", 1),
  createClass("class-3", "SE1803", "SWP391 · Nhóm thứ Tư", "OPEN", 2),
  createClass("class-4", "SE1805", "SWP391 · Nhóm thứ Sáu", "OPEN", 3),
  createClass("class-5", "SE1901", "SWP391 · Học kỳ sau", "DRAFT", null),
  createClass("class-6", "SE1701", "SWP391 · Khóa trước", "ARCHIVED", 0),
  createClass("class-7", "SE1801", "SWR302 · Nhóm thứ Năm", "OPEN", 0, 1),
];
function enrollment(person: PersonView, removed = false): EnrollmentView {
  return { accountId: person.id, name: person.name, email: person.email, enrolledAt: "2026-09-05", status: removed ? "REMOVED" : "ACTIVE" };
}
export const initialEnrollments: Record<string, EnrollmentView[]> = {
  "class-1": studentCandidates.slice(0, 5).map((person, index) => enrollment(person, index === 4)),
  "class-6": studentCandidates.slice(0, 2).map((person) => enrollment(person)),
  "class-7": [enrollment(studentCandidates[0]!), enrollment(studentCandidates[5]!)],
};
