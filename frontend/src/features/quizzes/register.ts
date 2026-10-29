// U10: mục Ngân hàng câu hỏi, Quiz trên menu bên của Subject Manager. Màn chưa làm thì link tạm chưa mở được.

import { navEntries } from "@/features/identity/slots";

navEntries.register({ key: "manager-questions", order: 30, title: "Ngân hàng câu hỏi", href: "/manager/questions", icon: "help_center", roles: ["SUBJECT_MANAGER"] });
navEntries.register({ key: "manager-quizzes", order: 40, title: "Quiz", href: "/manager/quizzes", icon: "quiz", roles: ["SUBJECT_MANAGER"] });
