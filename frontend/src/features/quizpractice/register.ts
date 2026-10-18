// U11: mục Luyện tập Quiz trên menu bên của Student. Màn chưa làm thì link tạm chưa mở được.

import { navEntries } from "@/features/identity/slots";

navEntries.register({ key: "quiz-practice", order: 30, title: "Luyện tập Quiz", href: "/quiz-practice", icon: "quiz", roles: ["STUDENT"] });
