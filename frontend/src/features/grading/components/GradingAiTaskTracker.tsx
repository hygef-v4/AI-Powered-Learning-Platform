"use client";
import { useEffect } from "react";
import { useSession } from "@/features/identity/hooks/session";
import { activateGradingAiTracking } from "../model/gradingAiTasks";

export function GradingAiTaskTracker() {
  const session = useSession();
  const accountId = session.status === "authenticated" ? session.profile.accountId : null;
  useEffect(() => {
    if (accountId) return activateGradingAiTracking(accountId);
  }, [accountId]);
  return null;
}
