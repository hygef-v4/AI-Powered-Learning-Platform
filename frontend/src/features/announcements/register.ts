"use client";

import { navEntries } from "@/features/identity/slots";
import { msg } from "@/i18n/msg";

navEntries.register({
  key: "class-announcements",
  order: 40,
  title: msg("announcements.register.allAnnouncements"),
  href: "/announcements",
  icon: "campaign",
  roles: ["TEACHER", "STUDENT"],
});
navEntries.register({
  key: "manager-class-announcements",
  order: 40,
  title: msg("announcements.register.allAnnouncements"),
  href: "/announcements",
  icon: "campaign",
  roles: ["SUBJECT_MANAGER"],
});
