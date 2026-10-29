// U02: mục Nhật ký trên menu bên của Admin. Màn chưa làm thì link tạm chưa mở được.

import { navEntries } from "@/features/identity/slots";

navEntries.register({ key: "admin-audit", order: 50, title: "Nhật ký", href: "/admin/audit", icon: "history", roles: ["ADMIN"] });
