"use client";
import { useCallback, useEffect, useSyncExternalStore } from "react";
import { addGradingProposal, readGradingProposals, resumeGradingPolling,
  subscribeGradingProposals } from "../model/gradingAiTasks";
import type { Proposal } from "../model/types";

export { POLL_INTERVAL_MS, MAX_POLL_ATTEMPTS, MAX_TRANSIENT_POLL_ERRORS } from "../model/gradingAiTasks";

export function useGradingProposals(evaluationId: string, _busy: boolean, onError: (error: string) => void) {
  const subscribe = useCallback((listener: () => void) => subscribeGradingProposals(evaluationId, listener), [evaluationId]);
  const read = useCallback(() => readGradingProposals(evaluationId), [evaluationId]);
  const state = useSyncExternalStore(subscribe, read, read);
  useEffect(() => { if (state.error) onError(state.error); }, [state.error, onError]);
  const addProposal = useCallback((proposal: Proposal) => addGradingProposal(evaluationId, proposal), [evaluationId]);
  const resumePolling = useCallback(() => resumeGradingPolling(evaluationId), [evaluationId]);
  return { proposals: state.proposals, proposalsLoaded: state.loaded,
    pollExhausted: state.pollExhausted, addProposal, resumePolling };
}
