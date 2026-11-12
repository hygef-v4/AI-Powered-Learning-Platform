// U08: mục Bài tập trên menu bên của Student và tổng quan bài tập trên trang lớp.

import { studentClassBoard } from "@/features/academics/slots";
import { navEntries } from "@/features/identity/slots";
import { ClassTodoCard } from "./components/ClassTodoCard";

navEntries.register({ key: "my-assignments", order: 20, title: "Bài tập", href: "/my-assignments", icon: "assignment", roles: ["STUDENT"] });
// Tổng quan đủ chiều rộng bên dưới Thông tin lớp.
studentClassBoard.register({ key: "assignments", order: 20, component: ClassTodoCard });
