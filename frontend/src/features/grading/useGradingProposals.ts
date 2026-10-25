"use client";
import { useEffect, useState } from "react";
import { gradingApi } from "./api";
import { message } from "./useResource";
import type { Proposal } from "./types";

const POLL_INTERVAL_MS = 3000;
const MAX_POLL_ATTEMPTS = 100;

export function useGradingProposals(
  evaluationId: string,
  busy: boolean,
  onError: (error: string) => void,
) {
  const [proposals, setProposals] = useState<Proposal[]>([]);
  useEffect(() => {
    let stopped = false;
    let timer: ReturnType<typeof setTimeout> | undefined;
    let attempts = 0;
    const diagnostics = new Map<string, string>();
    async function poll() {
      try {
        const listed = await gradingApi.proposals(evaluationId);
        const next = await Promise.all(listed.map(async (proposal) => {
          if (proposal.status !== "FAILED" || !proposal.taskId || !/^AI task (failed|unavailable or failed)$/.test(proposal.safeError ?? "")) return proposal;
          try {
            let reason = diagnostics.get(proposal.taskId);
            if (!reason) {
              const task = await gradingApi.task(proposal.taskId);
              reason = task.safeError ?? undefined;
              if (reason) diagnostics.set(proposal.taskId, reason);
            }
            return reason ? { ...proposal, safeError: reason } : proposal;
          } catch { return proposal; }
        }));
        if (stopped) return;
        setProposals(next);
        if (
          next.some((proposal) => proposal.status === "PENDING") &&
          ++attempts < MAX_POLL_ATTEMPTS
        ) {
          timer = setTimeout(poll, POLL_INTERVAL_MS);
        }
      } catch (error) {
        if (!stopped) onError(message(error));
      }
    }
    void poll();
    return () => {
      stopped = true;
      if (timer) clearTimeout(timer);
    };
  }, [evaluationId, busy, onError]);

  function addProposal(proposal: Proposal) {
    setProposals((current) => [proposal, ...current.filter((item) => item.id !== proposal.id)]);
  }
  return { proposals, addProposal };
}
