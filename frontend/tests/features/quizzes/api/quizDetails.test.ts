import { expect, it, vi } from "vitest";
import { apiFetch } from "@/shared/api/client";
import { requestQuizDraft, updateQuizDetails } from "@/features/quizzes/api";
import type { QuizDetailsInput } from "@/features/quizzes/model/types";

vi.mock("@/shared/api/client", () => ({ apiFetch: vi.fn().mockResolvedValue({}) }));

it("sends structured instructions with the same versioned quiz update", async () => {
  const input: QuizDetailsInput = {
    title: "Ôn tập", instructions: "Mô tả",
    instructionsDocument: { blocks: [{ type: "PARAGRAPH", align: "CENTER", indent: 0, runs: [{ text: "Mô tả", bold: true }] }] },
    settings: { shuffleQuestions: false, shuffleOptions: false, timeLimitMinutes: null, maxAttempts: null,
      showScoreAfterSubmit: true, showCorrectAnswers: false, leavePenalty: null },
    activityMonitoringEnabled: false, activityNotice: null,
  };
  await updateQuizDetails("quiz1", input, 4);
  const [url, request] = vi.mocked(apiFetch).mock.calls[0]!;
  expect(url).toBe("/api/v1/quizzes/quiz1");
  expect(JSON.parse(String(request?.body))).toMatchObject({ instructionsDocument: input.instructionsDocument, version: 4 });
});

it("keeps document IDs separate from the automatically selected AI source", async () => {
  await requestQuizDraft("quiz1", {
    instructions: "Tạo câu theo tài liệu", count: 5, type: null, difficulty: null, materialIds: ["material1"],
  }, 3);
  const [url, request] = vi.mocked(apiFetch).mock.calls.at(-1)!;
  expect(url).toBe("/api/v1/quizzes/quiz1/ai-drafts");
  expect(JSON.parse(String(request?.body))).toMatchObject({
    source: null, provider: null, materialIds: ["material1"], version: 3,
  });
});
