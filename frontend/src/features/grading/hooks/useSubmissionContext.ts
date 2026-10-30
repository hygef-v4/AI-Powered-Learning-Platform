"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { gradingApi } from "../api";
import type { AssignmentContext } from "../model/types";

const CONTEXT_ERROR = "Không tải được thông tin bài tập/lớp.";
type ContextState = { key: string; value: AssignmentContext | null; error: string };

/** Independent of submission filters; stale route responses never supply page labels. */
export function useSubmissionContext(classId: string, assignmentId: string) {
  const key = JSON.stringify([classId, assignmentId]);
  const [state, setState] = useState<ContextState>({ key, value: null, error: "" });
  const sequence = useRef(0);
  const refresh = useCallback(async () => {
    const current = ++sequence.current;
    setState({ key, value: null, error: "" });
    try {
      const value = await gradingApi.assignmentContext(assignmentId);
      if (value.id !== assignmentId || value.scope !== "CLASS" || value.classId !== classId) {
        throw new Error(CONTEXT_ERROR);
      }
      if (sequence.current === current) setState({ key, value, error: "" });
    } catch {
      if (sequence.current === current) setState({ key, value: null, error: CONTEXT_ERROR });
    }
  }, [classId, assignmentId, key]);

  useEffect(() => {
    let active = true;
    queueMicrotask(() => {
      if (active) void refresh();
    });
    return () => {
      active = false;
      sequence.current += 1;
    };
  }, [refresh]);

  return {
    value: state.key === key ? state.value : null,
    error: state.key === key ? state.error : "",
    refresh,
  };
}
