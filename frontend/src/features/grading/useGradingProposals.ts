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
    async function poll() {
      try {
        const next = await gradingApi.proposals(evaluationId);
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
