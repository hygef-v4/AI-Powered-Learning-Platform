// U14: mục Thông báo lớp trên menu bên của Student, Teacher và Subject Manager (nhóm Giảng dạy, cho lớp được giao dạy). Màn chưa làm thì link tạm chưa mở được.

import { navEntries } from "@/features/identity/slots";

navEntries.register({ key: "class-announcements", order: 40, title: "Thông báo lớp", href: "/announcements", icon: "campaign", roles: ["TEACHER", "STUDENT"] });
navEntries.register({ key: "manager-class-announcements", order: 90, title: "Thông báo lớp", href: "/announcements", icon: "campaign", roles: ["SUBJECT_MANAGER"], section: "Giảng dạy" });
