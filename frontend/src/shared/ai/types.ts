// Kiểu dữ liệu AI dùng chung (U06): nguồn AI và task, để màn của U07–U10 nhúng AiSourcePicker, AiTaskStatus.

export type AiProvider = "GEMINI" | "OPENAI" | "CLAUDE";
export type AiSource = "SHARED" | "PERSONAL";

export type SourceQuota = { used: number; limit: number; left: number; resetAt: string };

/** Một nguồn trong popup Chọn nguồn AI (GET /api/v1/ai/sources). */
export type SourceOption = {
  source: AiSource;
  provider: AiProvider | null;
  model: string | null;
  keyHint: string | null;
  available: boolean;
  reason: string | null;
  quota: SourceQuota | null;
  costBearer: "SCHOOL" | "SELF";
};

/** Lựa chọn người dùng gửi kèm yêu cầu; unit gọi truyền vào `IAiTask.submit` qua API của mình. */
export type SourceChoice = { source: AiSource; provider: AiProvider | null };

export type AiTaskState = "QUEUED" | "RUNNING" | "READY" | "FAILED" | "UNKNOWN" | "CANCELLED";

export type AiTaskView = {
  taskId: string;
  state: AiTaskState;
  source: AiSource;
  provider: AiProvider | null;
  model: string | null;
  applyResult: "APPLIED" | "STALE" | "REJECTED" | null;
  safeError: string | null;
  usage: { inputTokens: number | null; outputTokens: number | null; measurement: string } | null;
};
