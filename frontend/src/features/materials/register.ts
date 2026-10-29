// U13: mục Học liệu trên menu bên của Subject Manager. Màn chưa làm thì link tạm chưa mở được.

import { navEntries } from "@/features/identity/slots";

navEntries.register({ key: "manager-materials", order: 50, title: "Học liệu", href: "/manager/materials", icon: "folder_open", roles: ["SUBJECT_MANAGER"] });
