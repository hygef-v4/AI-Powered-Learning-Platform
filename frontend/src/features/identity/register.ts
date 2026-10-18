// U01: mục Tài khoản trên menu bên của Admin. Màn chưa làm thì link tạm chưa mở được.

import { navEntries } from "@/features/identity/slots";

navEntries.register({ key: "admin-accounts", order: 30, title: "Tài khoản", href: "/admin/accounts", icon: "group", roles: ["ADMIN"] });
