// U08: mục Bài tập trên menu bên của Student. Màn chưa làm thì link tạm chưa mở được.

import { navEntries } from "@/features/identity/slots";

navEntries.register({ key: "my-assignments", order: 20, title: "Bài tập", href: "/my-assignments", icon: "assignment", roles: ["STUDENT"] });
