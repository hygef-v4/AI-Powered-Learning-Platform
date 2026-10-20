import {apiFetch} from "@/shared/api/client";
import type {AdminDashboardView, BackupHistoryPage} from "./types";

const BASE = "/api/v1/admin/dashboard";
export function getAdminDashboard(signal?: AbortSignal): Promise<AdminDashboardView> {
  return apiFetch(BASE, {signal, cache: "no-store"});
}
export function getBackupHistory(page: number, size: number, signal?: AbortSignal): Promise<BackupHistoryPage> {
  return apiFetch(`${BASE}/backups?${new URLSearchParams({page: String(page), size: String(size)})}`, {signal, cache: "no-store"});
}
