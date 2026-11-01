"use client";

import { useCallback } from "react";
import { gradingApi } from "../api";
import type { SubmissionRow } from "../model/types";
import { useResource } from "./useResource";

export function useSubmissionDirectory(classId: string, assignmentId: string) {
  const load = useCallback(async () => {
    const rows: SubmissionRow[] = [];
    for (let page = 1; ; page += 1) {
      const result = await gradingApi.submissions(classId, assignmentId, "ALL", page);
      rows.push(...result.rows);
      if (!result.rows.length || rows.length >= result.total) return rows;
    }
  }, [classId, assignmentId]);
  return useResource(load);
}
