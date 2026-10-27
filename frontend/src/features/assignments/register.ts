// U07: mục Bài tập trên sidebar của môn (Subject Manager); phần Bài tập trong tab Đánh giá của lớp; số bài tập của môn.

import { subjectSummarySections, teacherClassTabs } from "@/features/academics/slots";
import { subjectNavEntries } from "@/features/identity/slots";
import { AssignmentSummary } from "./AssignmentSummary";
import { ClassAssignmentsSection } from "./ClassAssignmentsSection";

subjectNavEntries.register({ key: "manager-assignments", order: 40, title: "Bài tập", icon: "assignment", path: "/manager/assignments" });
teacherClassTabs.register({ key: "assignments", order: 10, component: ClassAssignmentsSection });
subjectSummarySections.register({ key: "assignments", order: 20, component: AssignmentSummary });
