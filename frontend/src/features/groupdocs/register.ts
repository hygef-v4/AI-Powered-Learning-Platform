"use client";
// U12: thẻ Tiến độ và nút mở tài liệu nhóm trên Assignment Detail bài nhóm (slot của U08); sidebar mục lục của tài liệu nhóm.

import { groupProgressSections, groupWorkEntries } from "@/features/assignmentwork/slots";
import { contextNavEntries } from "@/features/identity/slots";
import { GroupProgressCard } from "./components/GroupProgressCard";
import { OpenGroupWorkButton } from "./components/OpenGroupWorkButton";
import { groupDocContext } from "./components/groupDocNav";

groupWorkEntries.register({ key: "u12-open-group-work", order: 10, component: OpenGroupWorkButton });
groupProgressSections.register({ key: "u12-group-progress", order: 10, component: GroupProgressCard });
contextNavEntries.register({ key: "group-doc-outline", order: 4, roles: ["STUDENT", "TEACHER", "SUBJECT_MANAGER"], resolve: groupDocContext });
