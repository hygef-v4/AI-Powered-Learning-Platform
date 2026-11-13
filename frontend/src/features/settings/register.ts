// U03: mục Cài đặt trên menu bên của Admin.

import { navEntries } from "@/features/identity/slots";

navEntries.register({ key: "admin-settings", order: 45, title: "Cài đặt", href: "/admin/settings", icon: "tune", roles: ["ADMIN"] });
