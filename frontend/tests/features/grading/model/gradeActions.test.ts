import { describe, expect, it } from "vitest";
import { buildGradeActionItems } from "@/features/grading/model/gradeActions";
import type { SubmissionRow } from "@/features/grading/model/types";

const rows: SubmissionRow[] = ["PENDING", "DRAFT", "FINALIZED", "PUBLISHED", "SUPERSEDED"].map((status, index) => ({
  student: { accountId: `s${index}`, name: "Sinh viên", email: "s@school.test" },
  evaluationId: `e${index}`, attemptId: null, status, score: 0, maxScore: 10,
  late: false, submittedAt: null, attemptCount: 1, version: index + 7,
}));

describe("batch grade action eligibility", () => {
  it.each([
    ["ai", [{ evaluationId: "e0", version: 7 }, { evaluationId: "e1", version: 8 }]],
    ["finalize", [{ evaluationId: "e1", version: 8 }]],
    ["publish", [{ evaluationId: "e1", version: 8 }, { evaluationId: "e2", version: 9 }]],
  ] as const)("selects only eligible %s items and retains their original server version", (action, expected) => {
    expect(buildGradeActionItems([...rows, { ...rows[1]!, evaluationId: null }], action)).toEqual(expected);
  });
});
