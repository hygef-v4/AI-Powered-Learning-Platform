"use client";
// U12: nút mở tài liệu nhóm trên Assignment Detail bài nhóm (slot GroupWorkEntry của U08); sidebar mục lục của tài liệu nhóm.

import { groupWorkEntries } from "@/features/assignmentwork/slots";
import { contextNavEntries } from "@/features/identity/slots";
import { OpenGroupWorkButton } from "./components/OpenGroupWorkButton";
import { groupDocContext } from "./components/groupDocNav";

groupWorkEntries.register({ key: "u12-open-group-work", order: 10, component: OpenGroupWorkButton });
contextNavEntries.register({ key: "group-doc-outline", order: 4, roles: ["STUDENT", "TEACHER", "SUBJECT_MANAGER"], resolve: groupDocContext });
