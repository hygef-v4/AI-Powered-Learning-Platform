// U06: nút Kết nối AI trên thanh trên cùng cho Student, Teacher, Subject Manager.

import { accountMenuEntries, navEntries } from "@/features/identity/slots";
import { settingsSectionRegistry } from "@/features/settings/slots";
import { AiConnectionsPanel, TeacherQuotaPanel } from "./admin/components/AiSettingsPanels";

accountMenuEntries.register({
  key: "ai-connection",
  title: "Kết nối AI",
  href: "/ai/connection",
  icon: "smart_toy",
  roles: ["STUDENT", "TEACHER", "SUBJECT_MANAGER"],
});

// U06: Admin cấu hình API/quota tại Settings; MCP chỉ ở Kết nối AI của Student/Teacher/Subject Manager.
settingsSectionRegistry.register({ key: "ai-connections", component: AiConnectionsPanel, icon: "smart_toy", tone: "accent" });
settingsSectionRegistry.register({ key: "teacher-quota", component: TeacherQuotaPanel, icon: "speed", tone: "primary" });
