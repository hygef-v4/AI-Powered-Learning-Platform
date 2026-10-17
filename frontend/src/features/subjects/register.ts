// U04: mục Môn học trên menu bên của Admin. Màn chưa làm thì link tạm chưa mở được.

import { navEntries } from "@/features/identity/slots";

navEntries.register({ key: "admin-subjects", order: 40, title: "Môn học", href: "/admin/subjects", icon: "menu_book", roles: ["ADMIN"] });
