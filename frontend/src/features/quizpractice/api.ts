import type { ActivityEventInput } from "@/shared/activity/useActivityTracker";
import { apiFetch } from "@/shared/api/client";
import type { ClassAttempts, PracticeQuizDetail, PracticeQuizRow, ResultView, SaveResult, TakingView, TeacherAttemptDetail } from "./types";

const enc = encodeURIComponent;
const attemptPath = (id: string) => `/api/v1/quiz-attempts/${enc(id)}`;

export function getPracticeHistory(classId?: string | null, signal?: AbortSignal): Promise<PracticeQuizRow[]> {
  return apiFetch<PracticeQuizRow[]>(`/api/v1/me/quizzes${classId ? `?classId=${enc(classId)}` : ""}`, { signal });
}

/** Quiz đang mở gắn một học liệu cho lớp đang học (G4). */
export function getMaterialQuizzes(classId: string, materialId: string, signal?: AbortSignal): Promise<PracticeQuizRow[]> {
  return apiFetch<PracticeQuizRow[]>(`/api/v1/me/quizzes?classId=${enc(classId)}&materialId=${enc(materialId)}`, { signal });
}

export function getPracticeQuiz(quizId: string, classId: string, signal?: AbortSignal): Promise<PracticeQuizDetail> {
  return apiFetch<PracticeQuizDetail>(`/api/v1/me/quizzes/${enc(quizId)}?classId=${enc(classId)}`, { signal });
}

/** Bắt đầu lượt mới hoặc mở lại lượt đang làm (BR-U11-10). */
export function startAttempt(quizId: string, classId: string): Promise<TakingView> {
  return apiFetch<TakingView>(`/api/v1/quizzes/${enc(quizId)}/attempts`, { method: "POST", body: JSON.stringify({ classId }) });
}

export function getAttempt(attemptId: string, signal?: AbortSignal): Promise<TakingView> {
  return apiFetch<TakingView>(attemptPath(attemptId), { signal });
}

/** Tự lưu câu trả lời; `keepalive` khi trang sắp đóng hoặc bị ẩn để trình duyệt vẫn gửi xong request. */
export function saveAnswers(attemptId: string, answers: Record<string, string[]>, answerVersion: number,
  keepalive = false): Promise<SaveResult> {
  return apiFetch<SaveResult>(`${attemptPath(attemptId)}/answers`,
    { method: "PUT", body: JSON.stringify({ answers, answerVersion }), keepalive });
}

export function submitAttempt(attemptId: string): Promise<ResultView> {
  return apiFetch<ResultView>(`${attemptPath(attemptId)}/submit`, { method: "POST" });
}

/** Lô sự kiện rời trang của lượt (F8); `keepalive` khi trang sắp đóng. */
export function recordQuizActivity(attemptId: string, events: ActivityEventInput[], keepalive: boolean): Promise<void> {
  return apiFetch<void>(`${attemptPath(attemptId)}/activity`, { method: "POST", body: JSON.stringify({ events }), keepalive });
}

export function getResult(attemptId: string, signal?: AbortSignal): Promise<ResultView> {
  return apiFetch<ResultView>(`${attemptPath(attemptId)}/result`, { signal });
}

export function getClassAttempts(classId: string, quizId: string, signal?: AbortSignal): Promise<ClassAttempts> {
  return apiFetch<ClassAttempts>(`/api/v1/classes/${enc(classId)}/quizzes/${enc(quizId)}/attempts`, { signal });
}

export function getTeacherAttempt(classId: string, attemptId: string): Promise<TeacherAttemptDetail> {
  return apiFetch<TeacherAttemptDetail>(`/api/v1/classes/${enc(classId)}/quiz-attempts/${enc(attemptId)}`);
}
