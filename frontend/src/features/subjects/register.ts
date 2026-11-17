// U04: menu Admin dùng hai route đọc môn; quyền thật vẫn do U01/backend kiểm.

import { navEntries } from "@/features/identity/slots";
import { msg } from "@/i18n/msg";

navEntries.register({
  key: "admin-subjects",
  order: 40,
  title: msg("subjects.register.subjects"),
  href: "/admin/subjects",
  icon: "menu_book",
  roles: ["ADMIN"],
});
