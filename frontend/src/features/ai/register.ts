// U06: nút Kết nối AI trên thanh trên cùng cho Student, Teacher, Subject Manager.

import { accountMenuEntries, navEntries } from "@/features/identity/slots";
import { settingsSectionRegistry } from "@/features/settings/slots";
import { AiConnectionsPanel, TeacherQuotaPanel } from "./admin/AiSettingsPanels";

accountMenuEntries.register({
  key: "ai-connection",
  title: "Kết nối AI",
  href: "/ai/connection",
  icon: "smart_toy",
  roles: ["STUDENT", "TEACHER", "SUBJECT_MANAGER"],
});

// U06: mục "Cài đặt AI" trên Setting List (U03) mở trang cài đặt AI; Agent MCP của Admin là một tab trong trang đó.
settingsSectionRegistry.register({ key: "ai-connections", component: AiConnectionsPanel, icon: "smart_toy", tone: "accent" });
settingsSectionRegistry.register({ key: "teacher-quota", component: TeacherQuotaPanel, icon: "speed", tone: "primary" });
