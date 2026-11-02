import {apiFetch} from "@/shared/api/client";
import type {ActivityView, AdminDashboardView, BackupHistoryPage} from "../model/types";

const BASE = "/api/v1/admin/dashboard";
export function getAdminDashboard(signal?: AbortSignal): Promise<AdminDashboardView> {
  return apiFetch(BASE, {signal, cache: "no-store"});
}
export function getDashboardActivity(signal?: AbortSignal): Promise<ActivityView> {
  return apiFetch(`${BASE}/activity`, {signal, cache: "no-store"});
}
export function getBackupHistory(page: number, size: number, signal?: AbortSignal): Promise<BackupHistoryPage> {
  return apiFetch(`${BASE}/backups?${new URLSearchParams({page: String(page), size: String(size)})}`, {signal, cache: "no-store"});
}
