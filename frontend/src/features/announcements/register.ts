"use client";

import { navEntries } from "@/features/identity/slots";

navEntries.register({
  key: "class-announcements",
  order: 40,
  title: "Tất cả thông báo",
  href: "/announcements",
  icon: "campaign",
  roles: ["TEACHER", "STUDENT"],
});
navEntries.register({
  key: "manager-class-announcements",
  order: 40,
  title: "Tất cả thông báo",
  href: "/announcements",
  icon: "campaign",
  roles: ["SUBJECT_MANAGER"],
});
