import { Suspense } from "react";
import { ClassAnnouncementsPage } from "@/features/announcements/ClassAnnouncementsPage";
export default function Page() { return <Suspense fallback={<p>Đang tải thông báo…</p>}><ClassAnnouncementsPage /></Suspense>; }
