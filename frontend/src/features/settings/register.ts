// U03: mục Cài đặt trên menu bên của Admin.

import { navEntries } from "@/features/identity/slots";
import { msg } from "@/i18n/msg";

navEntries.register({ key: "admin-settings", order: 45, title: msg("settings.register.settings"), href: "/admin/settings", icon: "tune", roles: ["ADMIN"] });
