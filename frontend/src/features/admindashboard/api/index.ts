import { fetchApi } from "@/shared/api/client";
import type { ActivityView, AdminDashboardView, BackupHistoryPage } from "../model/types";

const BASE = "/api/v1/admin/dashboard";

// Đọc các số đếm và tình trạng sao lưu với dữ liệu mới mỗi lần gọi.
export function loadAdminDashboard(signal?: AbortSignal): Promise<AdminDashboardView> {
  return fetchApi(BASE, { signal, cache: "no-store" });
}

// Tải số liệu hoạt động qua endpoint riêng để vòng cập nhật không phụ thuộc nguồn tổng quan.
export function loadDashboardActivity(signal?: AbortSignal): Promise<ActivityView> {
  return fetchApi(`${BASE}/activity`, { signal, cache: "no-store" });
}

// Đọc một trang lịch sử sao lưu theo page và size, bỏ cache HTTP.
export function listBackupHistory(
  page: number,
  size: number,
  signal?: AbortSignal,
): Promise<BackupHistoryPage> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  return fetchApi(`${BASE}/backups?${params}`, { signal, cache: "no-store" });
}
