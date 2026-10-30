"use client";
import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { gradingApi } from "../api";
import { message } from "./useResource";
import type { Proposal } from "../model/types";

export const POLL_INTERVAL_MS = 3000;
export const MAX_POLL_ATTEMPTS = 100;
export const MAX_TRANSIENT_POLL_ERRORS = 3;

function isTerminalHttpError(error: unknown): boolean {
  const status = error instanceof ApiError ? error.problem.status : undefined;
  return status === 401 || status === 403 || status === 404;
}

export function useGradingProposals(
  evaluationId: string,
  _busy: boolean,
  onError: (error: string) => void,
) {
  const [state, setState] = useState<{
    evaluationId: string;
    proposals: Proposal[];
    pollExhausted: boolean;
  }>({ evaluationId, proposals: [], pollExhausted: false });
  const [pollToken, setPollToken] = useState(0);
  const proposalsRef = useRef<Proposal[]>([]);
  const onErrorRef = useRef(onError);

  const proposals = state.evaluationId === evaluationId ? state.proposals : [];
  const pollExhausted = state.evaluationId === evaluationId ? state.pollExhausted : false;

  useEffect(() => {
    onErrorRef.current = onError;
  }, [onError]);

  useEffect(() => {
    proposalsRef.current = [];
  }, [evaluationId]);

  useEffect(() => {
    let stopped = false;
    let timer: ReturnType<typeof setTimeout> | undefined;
    let inFlight = false;
    let attempts = 0;
    let consecutiveErrors = 0;
    const diagnostics = new Map<string, string>();

    function scheduleNext(delayMs: number) {
      if (stopped) return;
      if (timer) clearTimeout(timer);
      timer = setTimeout(() => {
        void poll();
      }, delayMs);
    }

    async function poll() {
      if (stopped || inFlight) return;
      inFlight = true;
      try {
        const listed = await gradingApi.proposals(evaluationId);
        const next = await Promise.all(
          listed.map(async (proposal) => {
            if (
              proposal.status === "FAILED" &&
              proposal.taskId &&
              /^AI task (failed|unavailable or failed)$/.test(proposal.safeError ?? "")
            ) {
              try {
                let reason = diagnostics.get(proposal.taskId);
                if (!reason && typeof gradingApi.task === "function") {
                  const task = await gradingApi.task(proposal.taskId);
                  reason = task?.safeError ?? undefined;
                  if (reason) diagnostics.set(proposal.taskId, reason);
                }
                return reason ? { ...proposal, safeError: reason } : proposal;
              } catch {
                return proposal;
              }
            }
            if (
              proposal.status === "PENDING" &&
              !proposal.taskState &&
              proposal.taskId &&
              typeof gradingApi.task === "function"
            ) {
              try {
                const task = await gradingApi.task(proposal.taskId);
                if (task?.state) {
                  return { ...proposal, taskState: task.state };
                }
              } catch {
                return proposal;
              }
            }
            return proposal;
          }),
        );
        if (stopped) return;
        consecutiveErrors = 0;
        proposalsRef.current = next;
        const hasPending = next.some((proposal) => proposal.status === "PENDING");
        if (!hasPending) {
          setState({ evaluationId, proposals: next, pollExhausted: false });
        } else if (++attempts < MAX_POLL_ATTEMPTS) {
          setState({ evaluationId, proposals: next, pollExhausted: false });
          scheduleNext(POLL_INTERVAL_MS);
        } else {
          setState({ evaluationId, proposals: next, pollExhausted: true });
        }
      } catch (error) {
        if (stopped) return;
        if (isTerminalHttpError(error)) {
          setState((prev) => ({
            evaluationId,
            proposals: prev.evaluationId === evaluationId ? prev.proposals : [],
            pollExhausted: false,
          }));
          onErrorRef.current(message(error));
          return;
        }
        const hasPending = proposalsRef.current.some((proposal) => proposal.status === "PENDING");
        if (hasPending) {
          consecutiveErrors += 1;
          if (consecutiveErrors <= MAX_TRANSIENT_POLL_ERRORS && ++attempts < MAX_POLL_ATTEMPTS) {
            const backoffMs = POLL_INTERVAL_MS * Math.min(4, 2 ** (consecutiveErrors - 1));
            scheduleNext(backoffMs);
          } else {
            setState((prev) => ({
              evaluationId,
              proposals: prev.evaluationId === evaluationId ? prev.proposals : [],
              pollExhausted: true,
            }));
            onErrorRef.current(message(error));
          }
        } else {
          onErrorRef.current(message(error));
        }
      } finally {
        inFlight = false;
      }
    }

    void poll();
    return () => {
      stopped = true;
      if (timer) clearTimeout(timer);
    };
  }, [evaluationId, pollToken]);

  const addProposal = useCallback(
    (proposal: Proposal) => {
      const normalized: Proposal =
        proposal.status === "PENDING" && !proposal.taskState
          ? { ...proposal, taskState: "QUEUED" }
          : proposal;
      setState((current) => {
        const base = current.evaluationId === evaluationId ? current.proposals : [];
        const next = [normalized, ...base.filter((item) => item.id !== normalized.id)];
        proposalsRef.current = next;
        return {
          evaluationId,
          proposals: next,
          pollExhausted: normalized.status === "PENDING" ? false : current.pollExhausted,
        };
      });
      if (normalized.status === "PENDING") {
        setPollToken((token) => token + 1);
      }
    },
    [evaluationId],
  );

  const resumePolling = useCallback(() => {
    setState((current) => ({
      evaluationId,
      proposals: current.evaluationId === evaluationId ? current.proposals : [],
      pollExhausted: false,
    }));
    setPollToken((token) => token + 1);
  }, [evaluationId]);

  return { proposals, pollExhausted, addProposal, resumePolling };
}
