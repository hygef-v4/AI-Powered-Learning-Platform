"use client";

// U05: mục menu bên của lớp, môn và hai dashboard; sidebar của một môn và của một lớp.

import { contextNavEntries, navEntries, subjectNavEntries } from "@/features/identity/slots";
import { resolveClassContext, resolveStudentClassContext, resolveSubjectContext } from "./components/ClassNavContexts";
import { msg } from "@/i18n/msg";

// Chủ nhiệm môn dùng menu của Giảng viên, thêm Môn được phân công; các màn của một môn nằm trong sidebar của môn.
navEntries.register({ key: "manager-dashboard", order: 10, title: msg("academics.register.assignedSubjects"), href: "/manager", icon: "menu_book", roles: ["SUBJECT_MANAGER"] });
navEntries.register({ key: "manager-teaching-classes", order: 20, title: msg("academics.register.assignedClasses"), href: "/teaching/classes", icon: "class", roles: ["SUBJECT_MANAGER"] });
navEntries.register({ key: "teaching-classes", order: 10, title: msg("academics.register.assignedClasses"), href: "/teaching/classes", icon: "class", roles: ["TEACHER"] });
navEntries.register({ key: "my-classes", order: 10, title: msg("academics.register.myClasses"), href: "/classes", icon: "school", roles: ["STUDENT"] });

subjectNavEntries.register({ key: "subject-info", order: 10, title: msg("academics.register.subjectInformation"), icon: "info", path: "/manager/subjects", href: (id) => `/manager/subjects/${encodeURIComponent(id)}` });
subjectNavEntries.register({ key: "subject-classes", order: 20, title: msg("academics.register.classes"), icon: "groups", path: "/manager/classes" });

contextNavEntries.register({ key: "class", order: 10, roles: ["TEACHER", "SUBJECT_MANAGER"], resolve: resolveClassContext });
contextNavEntries.register({ key: "subject", order: 20, roles: ["SUBJECT_MANAGER"], resolve: resolveSubjectContext });
contextNavEntries.register({ key: "student-class", order: 30, roles: ["STUDENT"], resolve: resolveStudentClassContext });
