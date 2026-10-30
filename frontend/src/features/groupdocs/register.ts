"use client";
// U12: nút mở tài liệu nhóm trên Assignment Detail bài nhóm (slot GroupWorkEntry của U08).

import { groupWorkEntries } from "@/features/assignmentwork/slots";
import { OpenGroupWorkButton } from "./components/OpenGroupWorkButton";

groupWorkEntries.register({ key: "u12-open-group-work", order: 10, component: OpenGroupWorkButton });
