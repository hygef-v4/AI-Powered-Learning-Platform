import { Suspense } from "react";
import { ClassAnnouncementsPage } from "@/features/announcements/ClassAnnouncementsPage";
import { PageLoading } from "@/shared/ui/PageStatus";
export default function Page() { return <Suspense fallback={<PageLoading label="Đang tải thông báo…" />}><ClassAnnouncementsPage /></Suspense>; }
