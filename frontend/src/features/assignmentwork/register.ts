// U08: mục Bài tập trên menu bên của Student; Bài tập cần làm (cá nhân / nhóm / luyện tập) trên trang lớp.

import { studentClassBoard } from "@/features/academics/slots";
import { navEntries } from "@/features/identity/slots";
import { ClassTodoCard } from "./ClassTodoCard";

navEntries.register({ key: "my-assignments", order: 20, title: "Bài tập", href: "/my-assignments", icon: "assignment", roles: ["STUDENT"] });
// Bài tập cần làm chiếm nửa phải hàng thẻ dưới Thông tin lớp.
studentClassBoard.register({ key: "assignments", order: 20, component: ClassTodoCard });
