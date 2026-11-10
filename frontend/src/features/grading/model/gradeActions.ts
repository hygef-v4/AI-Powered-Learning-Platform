import type { ItemVersion, SubmissionRow } from "./types";

export function buildGradeActionItems(rows: SubmissionRow[], action: "ai" | "finalize" | "publish"): ItemVersion[] {
  return rows.filter((row) => row.evaluationId && (action === "publish"
    ? row.status === "DRAFT" || row.status === "FINALIZED"
    : action === "finalize" ? row.status === "DRAFT" : row.status === "PENDING" || row.status === "DRAFT"))
    .map((row) => ({ evaluationId: row.evaluationId!, version: row.version }));
}
