"use client";

import { useCallback, useEffect, useRef } from "react";
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
 * khi rời trang. Gom lô tối đa 50 sự kiện, gửi lại khi lỗi. Dùng chung U08, U11, U12. `trackFocus` (chỉ Quiz, không có
 * iframe): mất tiêu điểm cửa sổ cũng tính là rời trang, để bắt trường hợp cửa sổ khác che mà trang vẫn "visible". Trả về
 * hàm gửi nốt sự kiện đang chờ, dùng trước khi nộp vì lượt đã nộp thì máy chủ không nhận sự kiện nữa.
 */
export function useActivityTracker({ enabled, send, trackFocus = false }: { enabled: boolean; send: ActivitySender; trackFocus?: boolean }) {
  const drainRef = useRef<() => Promise<void>>(async () => undefined);
  const sender = useRef(send);
  useEffect(() => {
    sender.current = send;
  }, [send]);

  useEffect(() => {
    if (!enabled || typeof document === "undefined") return;
    const sessionId = newId();
    let sequence = 0;
    let queue: ActivityEventInput[] = [];
    let inflight: Promise<void> | null = null;

    const push = (kind: ActivityKind) => {
      queue.push({ sessionId, clientEventId: newId(), sequence: sequence++, kind, clientTime: new Date().toISOString() });
      if (queue.length > MAX_QUEUE) queue = queue.slice(queue.length - MAX_QUEUE);
    };
    const flush = (keepalive = false): Promise<void> => {
      if (inflight || queue.length === 0) return inflight ?? Promise.resolve();
      const batch = queue.slice(0, MAX_BATCH);
      inflight = (async () => {
        try {
          await sender.current(batch, { keepalive });
          queue = queue.slice(batch.length);
        } catch {
          // Giữ lại trong hàng đợi, lần sau gửi lại; server bỏ trùng theo clientEventId.
        } finally {
          inflight = null;
        }
      })();
      return inflight;
    };
    drainRef.current = async () => {
      if (inflight) await inflight;
      while (queue.length > 0) {
        const before = queue.length;
        await flush();
        if (queue.length >= before) return;
      }
    };
    const onBlur = () => {
      push("HIDDEN");
      void flush();
    };
    const onFocus = () => {
      push("VISIBLE");
      void flush();
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
    if (trackFocus) {
      window.addEventListener("blur", onBlur);
      window.addEventListener("focus", onFocus);
    }
    const heartbeat = window.setInterval(() => push("HEARTBEAT"), HEARTBEAT_MS);
    const flusher = window.setInterval(() => void flush(), FLUSH_MS);
    return () => {
      document.removeEventListener("visibilitychange", onVisibility);
      window.removeEventListener("pagehide", onLeave);
      window.removeEventListener("blur", onBlur);
      window.removeEventListener("focus", onFocus);
      drainRef.current = async () => undefined;
      window.clearInterval(heartbeat);
      window.clearInterval(flusher);
      push("SESSION_END");
      void flush(true);
    };
  }, [enabled, trackFocus]);

  return useCallback(() => drainRef.current(), []);
}
