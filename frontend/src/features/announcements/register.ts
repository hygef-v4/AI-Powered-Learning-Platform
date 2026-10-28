"use client";

import { studentClassBoard } from "@/features/academics/slots";
import { navEntries } from "@/features/identity/slots";
import { ClassRecentAnnouncements } from "./ClassRecentAnnouncements";

// Thông báo gần đây chiếm nửa trái hàng thẻ dưới Thông tin lớp của trang lớp sinh viên.
studentClassBoard.register({ key: "u14-announcements", order: 10, component: ClassRecentAnnouncements });

navEntries.register({ key: "class-announcements", order: 40, title: "Thông báo lớp", href: "/announcements", icon: "campaign", roles: ["TEACHER", "STUDENT"] });
navEntries.register({ key: "manager-class-announcements", order: 40, title: "Thông báo lớp", href: "/announcements", icon: "campaign", roles: ["SUBJECT_MANAGER"] });
