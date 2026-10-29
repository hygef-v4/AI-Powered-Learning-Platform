// U06: nút Kết nối AI trên thanh trên cùng cho Student, Teacher, Subject Manager. Màn chưa làm thì link tạm chưa mở được.

import { accountMenuEntries } from "@/features/identity/slots";

accountMenuEntries.register({
  key: "ai-connection",
  title: "Kết nối AI",
  href: "/ai/connection",
  icon: "smart_toy",
  roles: ["STUDENT", "TEACHER", "SUBJECT_MANAGER"],
});
