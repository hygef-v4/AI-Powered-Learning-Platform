"use client";

import { useEffect, useRef } from "react";
import { newId } from "@/shared/document/types";

export type ActivityKind = "SESSION_START" | "HIDDEN" | "VISIBLE" | "HEARTBEAT" | "SESSION_END";
export type ActivityEventInput = { sessionId: string; clientEventId: string; sequence: number; kind: ActivityKind; clientTime: string };
export type ActivitySender = (events: ActivityEventInput[], options: { keepalive: boolean }) => Promise<unknown>;

export const HEARTBEAT_MS = 60_000;
export const FLUSH_MS = 10_000;
const MAX_BATCH = 50;
const MAX_QUEUE = 500;

/**
 * Gửi sự kiện rời trang (BR-U08-51) khi chính sách bật: SESSION_START, HIDDEN và VISIBLE theo trạng thái hiển thị của
 * trang (không dùng blur, nên chuyển vào iframe Draw.io không tính là rời trang), HEARTBEAT mỗi 60 giây, SESSION_END
 * khi rời trang. Gom lô tối đa 50 sự kiện, gửi lại khi lỗi. Dùng chung U08, U11, U12.
 */
export function useActivityTracker({ enabled, send }: { enabled: boolean; send: ActivitySender }) {
  const sender = useRef(send);
  useEffect(() => {
    sender.current = send;
  }, [send]);

  useEffect(() => {
    if (!enabled || typeof document === "undefined") return;
    const sessionId = newId();
    let sequence = 0;
    let queue: ActivityEventInput[] = [];
    let sending = false;

    const push = (kind: ActivityKind) => {
      queue.push({ sessionId, clientEventId: newId(), sequence: sequence++, kind, clientTime: new Date().toISOString() });
      if (queue.length > MAX_QUEUE) queue = queue.slice(queue.length - MAX_QUEUE);
    };
    const flush = async (keepalive = false) => {
      if (sending || queue.length === 0) return;
      sending = true;
      const batch = queue.slice(0, MAX_BATCH);
      try {
        await sender.current(batch, { keepalive });
        queue = queue.slice(batch.length);
      } catch {
        // Giữ lại trong hàng đợi, lần sau gửi lại; server bỏ trùng theo clientEventId.
      } finally {
        sending = false;
      }
    };
    const onVisibility = () => {
      push(document.visibilityState === "hidden" ? "HIDDEN" : "VISIBLE");
      void flush();
    };
    const onLeave = () => {
      push("SESSION_END");
      void flush(true);
    };

    push("SESSION_START");
    void flush();
    document.addEventListener("visibilitychange", onVisibility);
    window.addEventListener("pagehide", onLeave);
    const heartbeat = window.setInterval(() => push("HEARTBEAT"), HEARTBEAT_MS);
    const flusher = window.setInterval(() => void flush(), FLUSH_MS);
    return () => {
      document.removeEventListener("visibilitychange", onVisibility);
      window.removeEventListener("pagehide", onLeave);
      window.clearInterval(heartbeat);
      window.clearInterval(flusher);
      push("SESSION_END");
      void flush(true);
    };
  }, [enabled]);
}
