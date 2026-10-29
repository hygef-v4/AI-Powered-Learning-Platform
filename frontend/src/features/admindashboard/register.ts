// U15: mục Tổng quan trên menu bên của Admin. Màn chưa làm thì link tạm chưa mở được.

import { navEntries } from "@/features/identity/slots";

navEntries.register({ key: "admin-dashboard", order: 10, title: "Tổng quan", href: "/admin", icon: "monitoring", roles: ["ADMIN"] });
