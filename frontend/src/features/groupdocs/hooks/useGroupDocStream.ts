"use client";

import { useEffect, useRef } from "react";
import { refreshSession } from "@/shared/api/client";
import { eventsUrl } from "../api";
import type { StreamEvent } from "../model/types";

const MAX_RETRY_MS = 30_000;

/**
 * Nghe kênh SSE của tài liệu nhóm (BR-U12-37). Mỗi sự kiện gọi `onEvent`; kết nối lại sau khi mất thì gọi
 * `onReconnect` để tải lại toàn bộ (không đồng bộ từng phím gõ). Trình duyệt không tự nối lại khi server trả lỗi HTTP
 * (access token hết hạn sau 15 phút trả 401): khi đó làm mới phiên rồi tự mở kênh mới, chờ lâu dần tới 30 giây.
 */
export function useGroupDocStream(documentId: string | null, onEvent: (event: StreamEvent) => void, onReconnect: () => void) {
  const handlers = useRef({ onEvent, onReconnect });
  useEffect(() => {
    handlers.current = { onEvent, onReconnect };
  }, [onEvent, onReconnect]);

  useEffect(() => {
    if (!documentId || typeof EventSource === "undefined") return;
    let stopped = false;
    let source: EventSource | undefined;
    let timer: ReturnType<typeof setTimeout> | undefined;
    let attempts = 0;
    let dropped = false;

    const connect = () => {
      if (stopped) return;
      source = new EventSource(eventsUrl(documentId), { withCredentials: true });
      source.onmessage = (message) => {
        try {
          handlers.current.onEvent(JSON.parse(message.data) as StreamEvent);
        } catch {
          // dữ liệu không đọc được: bỏ qua, lần tải lại sau sẽ đồng bộ
        }
      };
      source.onerror = () => {
        dropped = true;
        if (source?.readyState !== EventSource.CLOSED) return; // trình duyệt đang tự nối lại
        source.close();
        const wait = Math.min(MAX_RETRY_MS, 1000 * 2 ** Math.min(attempts++, 5));
        timer = setTimeout(() => {
          void refreshSession().finally(connect);
        }, wait);
      };
      source.onopen = () => {
        attempts = 0;
        if (dropped) {
          dropped = false;
          handlers.current.onReconnect();
        }
      };
    };

    connect();
    return () => {
      stopped = true;
      if (timer) clearTimeout(timer);
      source?.close();
    };
  }, [documentId]);
}
