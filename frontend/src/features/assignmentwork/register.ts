// U08: mục Bài tập trên menu bên của Student; số bài sắp đến hạn trên thẻ lớp; Bài cần làm (cá nhân / nhóm) trên trang lớp.

import { classCardSections, studentClassBoard } from "@/features/academics/slots";
import { navEntries } from "@/features/identity/slots";
import { ClassCardAssignments } from "./ClassAssignmentsCard";
import { ClassTodoCard } from "./ClassTodoCard";

navEntries.register({ key: "my-assignments", order: 20, title: "Bài tập", href: "/my-assignments", icon: "assignment", roles: ["STUDENT"] });
classCardSections.register({ key: "assignments", order: 10, component: ClassCardAssignments });
// Bài cần làm chiếm nửa phải hàng thẻ dưới Thông tin lớp.
studentClassBoard.register({ key: "assignments", order: 20, component: ClassTodoCard });
