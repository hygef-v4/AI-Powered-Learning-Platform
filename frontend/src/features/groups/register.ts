"use client";
// U12: phần Nhóm trên Teacher Class Detail (tab Sinh viên, Thông tin lớp), popup Student Detail, Assignment Form bài
// nhóm; Nhóm của tôi dưới thẻ Giảng viên ở trang lớp sinh viên; nút mở tài liệu nhóm trên Assignment Detail.

import { createElement } from "react";
import { studentClassAside, studentDetailSections, teacherClassTabs } from "@/features/academics/slots";
import { assignmentFormSections } from "@/features/assignments/slots";
import { ClassGroupsPanel } from "./ClassGroupsPanel";
import { MyGroupCard } from "./MyGroup";
import { GroupAssignmentPanel, GroupCountCard, StudentGroupInfo } from "./TeacherGroupWidgets";

function TeacherGroupTab({ classId, tab, onGroupNamesLoaded }: { classId: string; tab: "info" | "students" | "materials" | "evals" | "gradebook"; onGroupNamesLoaded?: (names: Record<string, string>) => void }) {
  if (tab === "students") return createElement(ClassGroupsPanel, { classId, onGroupNamesLoaded });
  if (tab === "info") return createElement(GroupCountCard, { classId, variant: "overview" });
  return null;
}

teacherClassTabs.register({ key: "u12-groups", order: 30, component: TeacherGroupTab });
studentDetailSections.register({ key: "u12-student-group", order: 10, component: StudentGroupInfo });
assignmentFormSections.register({ key: "u12-group-readiness", order: 10, component: GroupAssignmentPanel });
studentClassAside.register({ key: "u12-my-group", order: 10, component: MyGroupCard });
