// U05: mục menu bên của lớp, môn và hai dashboard. Màn chưa làm thì link tạm chưa mở được.

import { navEntries } from "@/features/identity/slots";

navEntries.register({ key: "manager-dashboard", order: 10, title: "Tổng quan", href: "/manager", icon: "space_dashboard", roles: ["SUBJECT_MANAGER"] });
navEntries.register({ key: "manager-subject", order: 20, title: "Môn học của tôi", href: "/manager/subjects", icon: "menu_book", roles: ["SUBJECT_MANAGER"] });
navEntries.register({ key: "manager-classes", order: 70, title: "Lớp học", href: "/manager/classes", icon: "groups", roles: ["SUBJECT_MANAGER"] });
navEntries.register({ key: "manager-teaching-classes", order: 80, title: "Lớp được phân công", href: "/teaching/classes", icon: "class", roles: ["SUBJECT_MANAGER"], section: "Giảng dạy" });
navEntries.register({ key: "teaching-classes", order: 10, title: "Lớp được phân công", href: "/teaching/classes", icon: "class", roles: ["TEACHER"] });
navEntries.register({ key: "my-classes", order: 10, title: "Lớp của tôi", href: "/classes", icon: "school", roles: ["STUDENT"] });
