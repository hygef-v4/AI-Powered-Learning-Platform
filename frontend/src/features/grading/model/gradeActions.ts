import type { ItemVersion, SubmissionRow } from "./types";

// Chọn các hàng có đủ điều kiện theo thao tác và giữ version để gửi batch.
export function buildGradeActionItems(rows: SubmissionRow[], action: "ai" | "finalize" | "publish"): ItemVersion[] {
  return rows.filter((row) => row.evaluationId && (action === "publish"
    ? row.status === "DRAFT" || row.status === "FINALIZED"
    : action === "finalize" ? row.status === "DRAFT" : ["PENDING", "DRAFT", "FINALIZED", "PUBLISHED"].includes(row.status)))
    .map((row) => ({ evaluationId: row.evaluationId!, version: row.version }));
}
