"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { getUsageSummary, type UsageSummary } from "@/features/ai/admin/api";
import { loadAdminDashboard, listBackupHistory, loadDashboardActivity } from "../api";
import type { ActivityView, AdminDashboardView, BackupRunItem } from "../model/types";

/** Số liệu của Tổng quan; ngoài số đếm chính, mỗi nguồn khác lỗi thì là `null` và chỉ thẻ của nó báo không khả dụng. */
export type Overview = {
  dashboard: AdminDashboardView;
  activity: ActivityView | null;
  usage: UsageSummary | null;
  backups: BackupRunItem[] | null;
  loading: { activity: boolean; usage: boolean; backups: boolean };
};

/** Tự tải lại mỗi 30 giây khi tab đang mở; quay lại tab thì tải ngay. */
export const REFRESH_MS = 30_000;
export const SOURCE_TIMEOUT_MS = 5_000;

/** Bound even a transport that does not settle its promise when aborted. */
function requestSource<T>(read: (signal: AbortSignal) => Promise<T>, parent: AbortSignal): Promise<T> {
  const child = new AbortController();
  let timer: ReturnType<typeof setTimeout>;
  let cancel: () => void;
  return new Promise<T>((resolve, reject) => {
    cancel = () => { child.abort(); reject(new DOMException("Cancelled", "AbortError")); };
    parent.addEventListener("abort", cancel, { once: true });
    timer = setTimeout(() => { child.abort(); reject(new Error("Dashboard source timed out")); }, SOURCE_TIMEOUT_MS);
    if (parent.aborted) cancel();
    else Promise.resolve().then(() => read(child.signal)).then(resolve, reject);
  }).finally(() => {
    clearTimeout(timer);
    parent.removeEventListener("abort", cancel);
  });
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
    const partial: Omit<Overview, "dashboard"> = {
      activity: null, usage: null, backups: null,
      loading: { activity: true, usage: true, backups: true },
    };
    let ready = false;
    const loadOptionalSource = <K extends "activity" | "usage" | "backups">(key: K, read: (signal: AbortSignal) => Promise<Overview[K]>) => {
      void requestSource(read, current.signal).catch(() => null).then((value) => {
        if (current.signal.aborted) return;
        partial[key] = value;
        partial.loading = { ...partial.loading, [key]: false };
        if (ready) setData((previous) => previous && ({ ...previous, [key]: value, loading: partial.loading }));
      });
    };
    loadOptionalSource("activity", loadDashboardActivity);
    loadOptionalSource("usage", (signal) => getUsageSummary(7, signal));
    loadOptionalSource("backups", async (signal) => (await listBackupHistory(0, 40, signal)).items);
    try {
      const dashboard = await requestSource(loadAdminDashboard, current.signal);
      if (current.signal.aborted) return;
      ready = true;
      hasData.current = true;
      setData({ dashboard, ...partial });
      setError(null);
      setLive(true);
    } catch (e) {
      if (current.signal.aborted) return;
      current.abort();
      if (isDenied(e)) {
        hasData.current = false;
        setData(null);
        setError(e);
      } else {
        setLive(false);
        setData((previous) => previous && ({...previous, loading: {activity: false, usage: false, backups: false}}));
        if (!hasData.current) setError(e);
      }
    }
  }, []);

  useEffect(() => {
    let active = true;
    queueMicrotask(() => { if (active) void refresh(); });
    return () => { active = false; controller.current?.abort(); };
  }, [refresh, attempt]);

  const stopped = isDenied(error);
  useEffect(() => {
    if (stopped) return;
    const refreshVisibleOverview = () => { if (document.visibilityState === "visible") void refresh(); };
    const timer = window.setInterval(refreshVisibleOverview, REFRESH_MS);
    document.addEventListener("visibilitychange", refreshVisibleOverview);
    return () => {
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", refreshVisibleOverview);
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
