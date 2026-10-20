"use client";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { gradingApi } from "./api";
import { message } from "./useResource";
import { useGradingProposals } from "./useGradingProposals";
import type { AiRequest, Checks, Detail, Evaluation, GradingFormValues } from "./types";

const UNSAVED_CHANGES_MESSAGE = "Bạn có thay đổi chưa lưu. Rời bài này?";

function initialForm(evaluation: Evaluation): GradingFormValues {
  let checks: Checks;
  try {
    checks = JSON.parse(evaluation.rubricChecks);
  } catch {
    checks = {};
  }
  return {
    checks,
    adjustment: String(evaluation.adjustment ?? 0),
    adjustmentReason: evaluation.adjustmentReason ?? "",
    feedback: evaluation.feedback ?? "",
    changeReason: "",
    usedProposalId: null,
  };
}

export function useGradingWorkspace(initial: Detail, refresh: () => Promise<Detail>) {
  const router = useRouter();
  const [detail, setDetail] = useState(initial);
  const [form, setForm] = useState(() => initialForm(initial.evaluation));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [dirty, setDirty] = useState(false);
  const evaluation = detail.evaluation;
  const dirtyRef = useRef(dirty);
  const { proposals, addProposal } = useGradingProposals(evaluation.id, busy, setError);

  useEffect(() => {
    dirtyRef.current = dirty;
  }, [dirty]);
  useEffect(() => {
    function beforeUnload(event: BeforeUnloadEvent) {
      if (dirtyRef.current) {
        event.preventDefault();
        event.returnValue = "";
      }
    }
    window.addEventListener("beforeunload", beforeUnload);
    return () => window.removeEventListener("beforeunload", beforeUnload);
  }, []);

  function updateForm(patch: Partial<GradingFormValues>) {
    setForm((current) => ({ ...current, ...patch }));
    setDirty(true);
  }

  async function reload() {
    try {
      // Keep the unsaved form so a version conflict does not discard the teacher's work.
      setDetail(await refresh());
    } catch (error) {
      setError(message(error));
    }
  }

  function confirmNavigation() {
    return !dirty || window.confirm(UNSAVED_CHANGES_MESSAGE);
  }
  function navigate(id: string) {
    if (confirmNavigation()) router.push(`/teaching/evaluations/${id}`);
  }

  async function save() {
    if (busy) return;
    setBusy(true);
    setError("");
    try {
      const saved = await gradingApi.save(evaluation.id, {
        version: evaluation.version,
        checkedItemsByRubric: form.checks,
        adjustment: form.adjustment,
        adjustmentReason: form.adjustmentReason || null,
        feedback: form.feedback,
        changeReason: form.changeReason || null,
        usedProposalId: form.usedProposalId,
      });
      setDetail({ ...detail, evaluation: saved });
      setDirty(false);
      dirtyRef.current = false;
      setForm((current) => ({ ...current, usedProposalId: null }));
      const next = detail.navigationIds[detail.navigationIds.indexOf(evaluation.id) + 1];
      if (next) router.push(`/teaching/evaluations/${next}`);
    } catch (error) {
      setError(message(error));
    } finally {
      setBusy(false);
    }
  }

  async function requestProposal(input: AiRequest) {
    setBusy(true);
    setError("");
    try {
      addProposal(await gradingApi.request(evaluation.id, input));
    } catch (error) {
      setError(message(error));
    } finally {
      setBusy(false);
    }
  }

  function applyProposal(checks: Checks, feedback: string, proposalId: string) {
    updateForm({ checks, feedback, usedProposalId: proposalId });
  }

  return {
    detail,
    evaluation,
    form,
    busy,
    error,
    dirty,
    proposals,
    disabled: busy || !detail.current || evaluation.state === "SUPERSEDED",
    official: evaluation.state === "FINALIZED" || evaluation.state === "PUBLISHED",
    currentIndex: detail.navigationIds.indexOf(evaluation.id),
    updateForm,
    reload,
    navigate,
    confirmNavigation,
    save,
    requestProposal,
    applyProposal,
  };
}
