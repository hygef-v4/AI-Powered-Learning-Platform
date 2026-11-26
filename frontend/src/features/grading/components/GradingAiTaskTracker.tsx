"use client";
import { useEffect } from "react";
import { useSession } from "@/features/identity/hooks/SessionProvider";
import { activateGradingAiTracking } from "../model/gradingAiTasks";

// Gắn theo dõi đề xuất AI với phiên tài khoản để giải phóng theo dõi khi phiên đổi.
export function GradingAiTaskTracker() {
  const session = useSession();
  const accountId = session.status === "authenticated" ? session.profile.accountId : null;
  useEffect(() => {
    if (accountId) return activateGradingAiTracking(accountId);
  }, [accountId]);
  return null;
}
