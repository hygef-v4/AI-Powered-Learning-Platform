// U15: Tổng quan dùng chung khung Admin và registry navigation của U01.

import { navEntries } from "@/features/identity/slots";

navEntries.register({ key: "admin-dashboard", order: 10, title: "Tổng quan", href: "/admin", icon: "monitoring", roles: ["ADMIN"] });
