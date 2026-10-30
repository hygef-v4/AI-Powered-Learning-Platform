// U07: mục Bài tập trên sidebar của môn (Subject Manager); tab Bài tập của lớp.

import { teacherClassTabs } from "@/features/academics/slots";
import { subjectNavEntries } from "@/features/identity/slots";
import { ClassAssignmentsSection } from "./components/ClassAssignmentsSection";

subjectNavEntries.register({ key: "manager-assignments", order: 40, title: "Bài tập", icon: "assignment", path: "/manager/assignments" });
teacherClassTabs.register({ key: "assignments", order: 10, component: ClassAssignmentsSection });
