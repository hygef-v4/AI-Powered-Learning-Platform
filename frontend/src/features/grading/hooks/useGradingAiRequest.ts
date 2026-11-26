import { useEffect, useRef } from "react";
import { ApiError } from "@/shared/api/client";
import { readGradingAiSessionRevision } from "../model/gradingAiTasks";
import type { AiRequest } from "../model/types";

// Giữ requestRef cho lần thử lại trong cùng phạm vi và xóa khi phạm vi thay đổi.
export function useGradingAiRequest(scope: string) {
  const pending = useRef<{ key: string; requestRef: string } | null>(null);
  useEffect(() => {
    pending.current = null;
  }, [scope]);

  // Dùng lại requestRef cho cùng phiên và cấu hình AI để lần thử lại không tạo tác vụ trùng.
  return async function submitAiRequest<T>(
    input: AiRequest,
    sendRequest: (request: AiRequest) => Promise<T>,
  ): Promise<T> {
    const key = JSON.stringify([readGradingAiSessionRevision(), scope, input.source, input.provider]);
    const request = pending.current?.key === key
      ? pending.current
      : { key, requestRef: input.requestRef };
    pending.current = request;

    let shouldClear = true;
    try {
      return await sendRequest({ ...input, requestRef: request.requestRef });
    } catch (error) {
      shouldClear = error instanceof ApiError && error.problem.status < 500;
      throw error;
    } finally {
      if (shouldClear && pending.current === request) pending.current = null;
    }
  };
}
