"use client";

// U05: mục menu bên của lớp, môn và hai dashboard; sidebar của một môn và của một lớp.

import { contextNavEntries, navEntries, subjectNavEntries } from "@/features/identity/slots";
import { classContext, studentClassContext, subjectContext } from "./navContexts";

// Chủ nhiệm môn dùng menu của Giảng viên, thêm Môn được phân công; các màn của một môn nằm trong sidebar của môn.
navEntries.register({ key: "manager-dashboard", order: 10, title: "Môn được phân công", href: "/manager", icon: "menu_book", roles: ["SUBJECT_MANAGER"] });
navEntries.register({ key: "manager-teaching-classes", order: 20, title: "Lớp được phân công", href: "/teaching/classes", icon: "class", roles: ["SUBJECT_MANAGER"] });
navEntries.register({ key: "teaching-classes", order: 10, title: "Lớp được phân công", href: "/teaching/classes", icon: "class", roles: ["TEACHER"] });
navEntries.register({ key: "my-classes", order: 10, title: "Lớp của tôi", href: "/classes", icon: "school", roles: ["STUDENT"] });

subjectNavEntries.register({ key: "subject-info", order: 10, title: "Thông tin môn", icon: "info", path: "/manager/subjects", href: (id) => `/manager/subjects/${encodeURIComponent(id)}` });
subjectNavEntries.register({ key: "subject-classes", order: 20, title: "Lớp học", icon: "groups", path: "/manager/classes" });

contextNavEntries.register({ key: "class", order: 10, roles: ["TEACHER", "SUBJECT_MANAGER"], resolve: classContext });
contextNavEntries.register({ key: "subject", order: 20, roles: ["SUBJECT_MANAGER"], resolve: subjectContext });
contextNavEntries.register({ key: "student-class", order: 30, roles: ["STUDENT"], resolve: studentClassContext });
