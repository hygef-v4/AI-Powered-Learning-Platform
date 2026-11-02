"use client";

import { useEffect, useRef } from "react";
import { eventsUrl } from "./api";
import type { StreamEvent } from "./types";

/**
 * Nghe kênh SSE của tài liệu nhóm (BR-U12-37). Mỗi sự kiện gọi `onEvent`; kết nối lại sau khi mất thì gọi
 * `onReconnect` để tải lại toàn bộ (không đồng bộ từng phím gõ).
 */
export function useGroupDocStream(documentId: string | null, onEvent: (event: StreamEvent) => void, onReconnect: () => void) {
  const handlers = useRef({ onEvent, onReconnect });
  useEffect(() => {
    handlers.current = { onEvent, onReconnect };
  }, [onEvent, onReconnect]);

  useEffect(() => {
    if (!documentId || typeof EventSource === "undefined") return;
    const source = new EventSource(eventsUrl(documentId), { withCredentials: true });
    let dropped = false;
    source.onmessage = (message) => {
      try {
        handlers.current.onEvent(JSON.parse(message.data) as StreamEvent);
      } catch {
        // dữ liệu không đọc được: bỏ qua, lần tải lại sau sẽ đồng bộ
      }
    };
    source.onerror = () => {
      dropped = true;
    };
    source.onopen = () => {
      if (dropped) {
        dropped = false;
        handlers.current.onReconnect();
      }
    };
    return () => source.close();
  }, [documentId]);
}
