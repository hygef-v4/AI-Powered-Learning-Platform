// U14: mục Thông báo lớp trên menu bên của Student, Teacher. Màn chưa làm thì link tạm chưa mở được.

import { navEntries } from "@/features/identity/slots";

navEntries.register({ key: "class-announcements", order: 40, title: "Thông báo lớp", href: "/announcements", icon: "campaign", roles: ["TEACHER", "STUDENT"] });
