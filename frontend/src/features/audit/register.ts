// U02: mục Nhật ký trên menu bên của Admin. Màn chưa làm thì link tạm chưa mở được.

import { navEntries } from "@/features/identity/slots";
import { msg } from "@/i18n/msg";

navEntries.register({ key: "admin-audit", order: 50, title: msg("audit.register.log"), href: "/admin/audit", icon: "history", roles: ["ADMIN"] });
