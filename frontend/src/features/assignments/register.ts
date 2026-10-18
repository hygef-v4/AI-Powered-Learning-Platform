// U07: mục Bài tập trên menu bên của Subject Manager. Màn chưa làm thì link tạm chưa mở được.

import { navEntries } from "@/features/identity/slots";

navEntries.register({ key: "manager-assignments", order: 60, title: "Bài tập", href: "/manager/assignments", icon: "assignment", roles: ["SUBJECT_MANAGER"] });
