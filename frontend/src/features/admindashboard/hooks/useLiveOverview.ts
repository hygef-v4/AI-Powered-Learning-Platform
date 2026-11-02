"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { getUsageSummary, type UsageSummary } from "@/features/ai/admin/api";
import { getAdminDashboard, getBackupHistory, getDashboardActivity } from "../api";
import type { ActivityView, AdminDashboardView, BackupRunItem } from "../model/types";

/** Số liệu của Tổng quan; ngoài số đếm chính, mỗi nguồn khác lỗi thì là `null` và chỉ thẻ của nó báo không khả dụng. */
export type Overview = {
  dashboard: AdminDashboardView;
  activity: ActivityView | null;
  usage: UsageSummary | null;
  backups: BackupRunItem[] | null;
};

/** Tự tải lại mỗi 30 giây khi tab đang mở; quay lại tab thì tải ngay. */
export const REFRESH_MS = 30_000;

const settled = <T,>(result: PromiseSettledResult<T>) => (result.status === "fulfilled" ? result.value : null);

async function loadOverview(signal: AbortSignal): Promise<Overview> {
  const [dashboard, activity, usage, backups] = await Promise.allSettled([
    getAdminDashboard(signal),
    getDashboardActivity(signal),
    getUsageSummary(7),
    getBackupHistory(0, 40, signal),
  ]);
  if (dashboard.status === "rejected") throw dashboard.reason;
  return { dashboard: dashboard.value, activity: settled(activity), usage: settled(usage), backups: settled(backups)?.items ?? null };
}

export const isDenied = (error: unknown) => error instanceof ApiError && [401, 403].includes(error.problem.status);

/**
 * Tải Tổng quan rồi cập nhật liên tục: lần đầu lỗi thì báo lỗi (có Thử lại); lần cập nhật sau lỗi thì giữ số cũ và
 * `live = false` (nhãn Trực tiếp chuyển sang Mất kết nối), lần sau được lại thì về bình thường. Bị từ chối quyền thì xóa
 * số đã hiện và ngừng cập nhật.
 */
export function useLiveOverview() {
  const [data, setData] = useState<Overview | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [live, setLive] = useState(true);
  const [attempt, setAttempt] = useState(0);
  const controller = useRef<AbortController | null>(null);
  // Đã có số để hiện chưa: lần cập nhật sau lỗi thì giữ số cũ, chưa có thì báo lỗi.
  const hasData = useRef(false);

  const refresh = useCallback(async () => {
    controller.current?.abort();
    const current = new AbortController();
    controller.current = current;
    try {
      const next = await loadOverview(current.signal);
      if (current.signal.aborted) return;
      hasData.current = true;
      setData(next);
      setError(null);
      setLive(true);
    } catch (e) {
      if (current.signal.aborted) return;
      if (isDenied(e)) {
        hasData.current = false;
        setData(null);
        setError(e);
      } else {
        setLive(false);
        if (!hasData.current) setError(e);
      }
    }
  }, []);

  useEffect(() => {
    queueMicrotask(() => void refresh());
    return () => controller.current?.abort();
  }, [refresh, attempt]);

  const stopped = isDenied(error);
  useEffect(() => {
    if (stopped) return;
    const tick = () => { if (document.visibilityState === "visible") void refresh(); };
    const timer = window.setInterval(tick, REFRESH_MS);
    document.addEventListener("visibilitychange", tick);
    return () => {
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", tick);
    };
  }, [refresh, stopped]);

  const retry = useCallback(() => {
    hasData.current = false;
    setError(null);
    setData(null);
    setAttempt((n) => n + 1);
  }, []);
  return { data, error, live, retry };
}
