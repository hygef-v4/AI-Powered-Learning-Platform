// U15: Tổng quan dùng chung khung Admin và registry navigation của U01.

import { navEntries } from "@/features/identity/slots";
import { msg } from "@/i18n/msg";

navEntries.register({ key: "admin-dashboard", order: 10, title: msg("admindashboard.register.overview"), href: "/admin", icon: "monitoring", roles: ["ADMIN"] });
