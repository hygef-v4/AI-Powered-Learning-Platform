"use client";
import { useCallback, useEffect, useSyncExternalStore } from "react";
import { addGradingProposal, readGradingProposals, resumeGradingPolling,
  subscribeGradingProposals } from "../model/gradingAiTasks";
import type { Proposal } from "../model/types";

export { POLL_INTERVAL_MS, MAX_POLL_ATTEMPTS, MAX_TRANSIENT_POLL_ERRORS } from "../model/gradingAiTasks";

// Kết nối bản chấm đang mở với store theo dõi đề xuất theo ID.
export function useGradingProposals(evaluationId: string, _busy: boolean, onError: (error: string) => void) {
  // Đăng ký nhận thay đổi chỉ cho bản chấm hiện hành.
  const subscribe = useCallback((listener: () => void) => subscribeGradingProposals(evaluationId, listener), [evaluationId]);
  // Đọc snapshot mới nhất của đề xuất trong store dùng chung.
  const read = useCallback(() => readGradingProposals(evaluationId), [evaluationId]);
  const state = useSyncExternalStore(subscribe, read, read);
  useEffect(() => { if (state.error) onError(state.error); }, [state.error, onError]);
  // Đưa phản hồi của lệnh tạo đề xuất vào store trước lượt tải kế tiếp.
  const addProposal = useCallback((proposal: Proposal) => addGradingProposal(evaluationId, proposal), [evaluationId]);
  // Khởi động lại thăm dò cho bản chấm hiện hành sau khi người dùng thử lại.
  const resumePolling = useCallback(() => resumeGradingPolling(evaluationId), [evaluationId]);
  return { proposals: state.proposals, proposalsLoaded: state.loaded,
    pollExhausted: state.pollExhausted, addProposal, resumePolling };
}
