// U01: mục Tài khoản trên menu bên của Admin. Màn chưa làm thì link tạm chưa mở được.

import { navEntries } from "@/features/identity/slots";
import { msg } from "@/i18n/msg";

navEntries.register({ key: "admin-accounts", order: 30, title: msg("identity.register.account"), href: "/admin/accounts", icon: "group", roles: ["ADMIN"] });
