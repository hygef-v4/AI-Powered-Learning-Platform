"use client";
// U12: phần Nhóm trên Teacher Class Detail (tab Sinh viên, Thông tin lớp), popup Student Detail, Assignment Form bài
// nhóm; Nhóm của tôi dưới thẻ Giảng viên ở trang lớp sinh viên; nút mở tài liệu nhóm trên Assignment Detail.

import { createElement } from "react";
import { studentClassAside, studentDetailSections, teacherClassTabs } from "@/features/academics/slots";
import { assignmentFormSections } from "@/features/assignments/slots";
import { ClassGroupsPanel } from "./components/ClassGroupsPanel";
import { MyGroupCard } from "./components/MyGroup";
import { GroupAssignmentPanel, StudentGroupInfo } from "./components/TeacherGroupWidgets";

function TeacherGroupTab({ classId, tab, students, onViewStudent, onGroupNamesLoaded, onStudentTable }: {
  classId: string;
  tab: "info" | "students" | "materials" | "evals" | "gradebook";
  students?: { accountId: string; name: string; email: string; enrolledAt: string }[];
  onViewStudent?: (accountId: string) => void;
  onGroupNamesLoaded?: (names: Record<string, string>, leaders?: string[]) => void;
  onStudentTable?: (shown: boolean) => void;
}) {
  return tab === "students"
    ? createElement(ClassGroupsPanel, { classId, students, onView: onViewStudent, onGroupNamesLoaded, onStudentTable })
    : null;
}

teacherClassTabs.register({ key: "u12-groups", order: 30, component: TeacherGroupTab });
studentDetailSections.register({ key: "u12-student-group", order: 10, component: StudentGroupInfo });
assignmentFormSections.register({ key: "u12-group-readiness", order: 10, component: GroupAssignmentPanel });
studentClassAside.register({ key: "u12-my-group", order: 10, component: MyGroupCard });
