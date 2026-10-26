// U08: mục Bài tập trên menu bên của Student; bài sắp đến hạn trên Student Class Detail và thẻ lớp.

import { classCardSections, studentClassSections } from "@/features/academics/slots";
import { navEntries } from "@/features/identity/slots";
import { ClassAssignmentsCard, ClassCardAssignments } from "./ClassAssignmentsCard";

navEntries.register({ key: "my-assignments", order: 20, title: "Bài tập", href: "/my-assignments", icon: "assignment", roles: ["STUDENT"] });
// Bài cần làm đứng đầu trang lớp của sinh viên, trên học liệu.
studentClassSections.register({ key: "assignments", order: 5, component: ClassAssignmentsCard });
classCardSections.register({ key: "assignments", order: 10, component: ClassCardAssignments });
