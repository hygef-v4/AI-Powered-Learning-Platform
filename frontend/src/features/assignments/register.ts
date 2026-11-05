// U07: mục Bài tập trên sidebar của môn (Subject Manager); tab Bài tập của lớp; sidebar các phần của bài khi soạn một bài.

import { teacherClassTabs } from "@/features/academics/slots";
import { contextNavEntries, subjectNavEntries } from "@/features/identity/slots";
import { assignmentContext } from "./components/assignmentNav";
import { ClassAssignmentsSection } from "./components/ClassAssignmentsSection";

subjectNavEntries.register({ key: "manager-assignments", order: 40, title: "Bài tập", icon: "assignment", path: "/manager/assignments" });
teacherClassTabs.register({ key: "assignments", order: 10, component: ClassAssignmentsSection });
contextNavEntries.register({ key: "assignment-outline", order: 4, roles: ["SUBJECT_MANAGER", "TEACHER"], resolve: assignmentContext });
