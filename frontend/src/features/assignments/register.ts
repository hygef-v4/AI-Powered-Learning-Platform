// U07: mục Bài tập trên menu bên của Subject Manager; phần Bài tập trong tab Đánh giá của lớp; số bài tập của môn.

import { subjectSummarySections, teacherClassTabs } from "@/features/academics/slots";
import { navEntries } from "@/features/identity/slots";
import { AssignmentSummary } from "./AssignmentSummary";
import { ClassAssignmentsSection } from "./ClassAssignmentsSection";

navEntries.register({ key: "manager-assignments", order: 60, title: "Bài tập", href: "/manager/assignments", icon: "assignment", roles: ["SUBJECT_MANAGER"] });
teacherClassTabs.register({ key: "assignments", order: 10, component: ClassAssignmentsSection });
subjectSummarySections.register({ key: "assignments", order: 20, component: AssignmentSummary });
