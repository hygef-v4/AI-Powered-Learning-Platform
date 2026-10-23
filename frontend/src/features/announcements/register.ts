"use client";

import { navEntries } from "@/features/identity/slots";
import { studentClassSections } from "@/features/academics/slots";
import { ClassAnnouncementsCard } from "./ClassAnnouncementsCard";

studentClassSections.register({ key: "u14-announcements", order: 30, component: ClassAnnouncementsCard });

navEntries.register({ key: "class-announcements", order: 40, title: "Thông báo lớp", href: "/announcements", icon: "campaign", roles: ["TEACHER", "STUDENT"] });
navEntries.register({ key: "manager-class-announcements", order: 90, title: "Thông báo lớp", href: "/announcements", icon: "campaign", roles: ["SUBJECT_MANAGER"], section: "Giảng dạy" });
