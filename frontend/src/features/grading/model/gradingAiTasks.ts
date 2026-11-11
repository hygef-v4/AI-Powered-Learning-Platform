"use client";
import { ApiError } from "@/shared/api/client";
import { showErrorToast, showToast } from "@/shared/ui/Toast";
import { gradingApi } from "../api";
import { formatErrorMessage } from "./format";
import type { Proposal } from "./types";

export const POLL_INTERVAL_MS = 3000;
export const MAX_POLL_ATTEMPTS = 100;
export const MAX_TRANSIENT_POLL_ERRORS = 3;
type Snapshot = { proposals: Proposal[]; loaded: boolean; pollExhausted: boolean; error: string };
type Entry = {
  snapshot: Snapshot; listeners: Set<() => void>; timer?: ReturnType<typeof setTimeout>;
  inFlight: boolean; generation: number; revision: number; attempts: number; errors: number;
  notifyFor: Set<string>; diagnostics: Map<string, string>; label: string;
};
const entries = new Map<string, Entry>();
let trackingAccount: string | null = null;
let trackingActive = false;

function findEntry(evaluationId: string): Entry {
  let entry = entries.get(evaluationId);
  if (!entry) {
    entry = { snapshot: { proposals: [], loaded: false, pollExhausted: false, error: "" },
      listeners: new Set(), inFlight: false, generation: 0, revision: 0, attempts: 0, errors: 0,
      notifyFor: new Set(), diagnostics: new Map(), label: "bài chấm" };
    entries.set(evaluationId, entry);
  }
  return entry;
}
function updateSnapshot(entry: Entry, patch: Partial<Snapshot>) {
  entry.snapshot = { ...entry.snapshot, ...patch };
  entry.listeners.forEach((listener) => listener());
}
function stopPolling(entry: Entry) {
  if (entry.timer) clearTimeout(entry.timer);
  entry.timer = undefined;
  entry.generation += 1;
  entry.inFlight = false;
}
function schedulePolling(evaluationId: string, entry: Entry, delay = POLL_INTERVAL_MS) {
  if (entry.timer) clearTimeout(entry.timer);
  entry.timer = setTimeout(() => { entry.timer = undefined; void pollProposals(evaluationId, entry); }, delay);
}
async function pollProposals(evaluationId: string, entry: Entry) {
  if (entry.inFlight) return;
  entry.inFlight = true;
  const generation = entry.generation;
  const revision = entry.revision;
  try {
    const listed = await gradingApi.listProposals(evaluationId);
    const next = await Promise.all(listed.map(async (proposal) => {
      if (!proposal.taskId || typeof gradingApi.loadTask !== "function") return proposal;
      try {
        if (proposal.status === "FAILED" && /^AI task (failed|unavailable or failed)$/.test(proposal.safeError ?? "")) {
          let reason = entry.diagnostics.get(proposal.taskId);
          if (!reason) {
            reason = (await gradingApi.loadTask(proposal.taskId))?.safeError ?? undefined;
            if (reason) entry.diagnostics.set(proposal.taskId, reason);
          }
          return reason ? { ...proposal, safeError: reason } : proposal;
        }
        if (proposal.status === "PENDING" && !proposal.taskState) {
          const task = await gradingApi.loadTask(proposal.taskId);
          if (task?.state) return { ...proposal, taskState: task.state };
        }
      } catch { /* Stored proposals survive unavailable task diagnostics. */ }
      return proposal;
    }));
    if (generation !== entry.generation) return;
    // A list started before dispatch finished must not erase the newly accepted proposal.
    if (revision !== entry.revision) next.push(...entry.snapshot.proposals.filter((item) => !next.some((p) => p.id === item.id)));
    entry.errors = 0;
    for (const proposal of next) {
      if (proposal.status === "PENDING") entry.notifyFor.add(proposal.id);
      else if (entry.notifyFor.delete(proposal.id)) {
        if (proposal.status === "READY") showToast(`Đề xuất AI cho ${entry.label} đã sẵn sàng. Mở Xem đề xuất AI để kiểm tra.`);
        else if (proposal.status === "FAILED" || proposal.status === "REJECTED") {
          showErrorToast(proposal.safeError || `Không tạo được đề xuất AI cho ${entry.label}.`);
        }
      }
    }
    const pending = next.some((p) => p.status === "PENDING");
    const exhausted = pending && ++entry.attempts >= MAX_POLL_ATTEMPTS;
    updateSnapshot(entry, { proposals: next, loaded: true, pollExhausted: exhausted, error: "" });
    if (pending && !exhausted) schedulePolling(evaluationId, entry);
  } catch (error) {
    if (generation !== entry.generation) return;
    const status = error instanceof ApiError ? error.problem.status : undefined;
    const terminal = status === 401 || status === 403 || status === 404;
    const pending = entry.snapshot.proposals.some((p) => p.status === "PENDING");
    if (!terminal && pending && ++entry.errors <= MAX_TRANSIENT_POLL_ERRORS && ++entry.attempts < MAX_POLL_ATTEMPTS) {
      schedulePolling(evaluationId, entry, POLL_INTERVAL_MS * Math.min(4, 2 ** (entry.errors - 1)));
    } else updateSnapshot(entry, { pollExhausted: pending && !terminal, error: formatErrorMessage(error) });
  } finally {
    if (generation === entry.generation) entry.inFlight = false;
  }
}
export function readGradingProposals(evaluationId: string) {
  return findEntry(evaluationId).snapshot;
}
export function subscribeGradingProposals(evaluationId: string, listener: () => void) {
  const entry = findEntry(evaluationId);
  entry.listeners.add(listener);
  if (entry.listeners.size === 1 && !entry.timer) void pollProposals(evaluationId, entry);
  return () => {
    entry.listeners.delete(listener);
    if (!entry.listeners.size && (!trackingActive || !entry.snapshot.proposals.some((p) => p.status === "PENDING"))) {
      stopPolling(entry);
      if (entries.get(evaluationId) === entry) entries.delete(evaluationId);
    }
  };
}
export function addGradingProposal(evaluationId: string, proposal: Proposal) {
  const entry = findEntry(evaluationId);
  entry.revision += 1;
  const normalized = proposal.status === "PENDING" && !proposal.taskState ? { ...proposal, taskState: "QUEUED" } : proposal;
  updateSnapshot(entry, { proposals: [normalized, ...entry.snapshot.proposals.filter((p) => p.id !== proposal.id)],
    loaded: true, error: "", pollExhausted: false });
  if (proposal.status === "PENDING") {
    entry.notifyFor.add(proposal.id);
    entry.attempts = 0;
    entry.errors = 0;
    if (!entry.inFlight) void pollProposals(evaluationId, entry);
  }
}
export function resumeGradingPolling(evaluationId: string) {
  const entry = findEntry(evaluationId);
  entry.attempts = 0;
  entry.errors = 0;
  if (entry.timer) clearTimeout(entry.timer);
  entry.timer = undefined;
  updateSnapshot(entry, { pollExhausted: false, error: "" });
  void pollProposals(evaluationId, entry);
}
export function setGradingAiLabel(evaluationId: string, label: string) {
  findEntry(evaluationId).label = label;
}
export function readGradingAiAccount() {
  return trackingAccount;
}
export function activateGradingAiTracking(accountId: string) {
  if (trackingAccount !== accountId) { entries.forEach(stopPolling); entries.clear(); }
  trackingAccount = accountId;
  trackingActive = true;
  return () => {
    if (trackingAccount !== accountId) return;
    trackingActive = false;
    trackingAccount = null;
    entries.forEach((entry, id) => {
      if (!entry.listeners.size) { stopPolling(entry); entries.delete(id); }
    });
  };
}
