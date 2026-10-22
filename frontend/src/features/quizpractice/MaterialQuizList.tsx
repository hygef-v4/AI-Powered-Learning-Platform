"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useSession } from "@/features/identity/session";
import { Alert, Button } from "@/shared/ui";
import { getMaterialQuizzes, startAttempt } from "./api";
import { attemptsText, errorMessage, formatPoints } from "./labels";
import type { PracticeQuizRow } from "./types";

/**
 * Màn #37 Learning Material, phần "Quiz gắn với học liệu" (UC 50, 51; G4): U13 nhúng qua `LearningMaterialSection`.
 * Chỉ sinh viên thấy; không có quiz gắn tài liệu thì không hiện gì.
 */
export function MaterialQuizList({ materialId, classId }: { materialId: string; classId: string }) {
  const session = useSession();
  const student = session.status === "authenticated" && session.profile.role === "STUDENT";
  const router = useRouter();
  const [rows, setRows] = useState<PracticeQuizRow[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!student) return;
    const controller = new AbortController();
    getMaterialQuizzes(classId, materialId, controller.signal)
      .then((list) => !controller.signal.aborted && setRows(list))
      .catch((failure: unknown) => !controller.signal.aborted && setError(errorMessage(failure)));
    return () => controller.abort();
  }, [student, classId, materialId]);

  if (!student || (rows !== null && rows.length === 0)) return null;

  const start = async (row: PracticeQuizRow) => {
    setBusy(true);
    setError(null);
    try {
      router.push(`/quiz-attempts/${(await startAttempt(row.quizId, row.classId)).attemptId}`);
    } catch (failure) {
      setError(errorMessage(failure));
      setBusy(false);
    }
  };

  return (
    <section className="rounded-[13px] border border-line bg-white p-4" data-testid="material-quiz-list">
      <h2 className="mb-2 text-[14px] font-bold text-ink">Quiz gắn với học liệu</h2>
      {error && <Alert tone="danger" role="alert" className="mb-2">{error}</Alert>}
      {rows === null && !error && <p role="status" className="text-[12px] text-muted">Đang tải quiz…</p>}
      <ul className="flex flex-col gap-3">
        {rows?.map((row) => {
          const exhausted = row.maxAttempts !== null && row.attemptsUsed >= row.maxAttempts && !row.inProgressAttemptId;
          return (
            <li key={row.quizId} className="flex flex-col gap-1" data-testid="material-quiz-row">
              <p className="text-[13px] font-semibold text-ink">{row.title}</p>
              <p className="text-[12px] text-muted">
                Practice · {row.timeLimitMinutes ? `${row.timeLimitMinutes} phút` : "không giới hạn thời gian"}
                {" · "}Đã dùng {attemptsText(row.attemptsUsed, row.maxAttempts)}
                {row.bestScore !== null && <> · Điểm cao nhất {formatPoints(row.bestScore)}/{formatPoints(row.maxScore)}</>}
              </p>
              {exhausted ? (
                <p className="text-[12px] text-warning">Bạn đã dùng hết {row.maxAttempts}/{row.maxAttempts} lượt làm quiz này.</p>
              ) : (
                <div>
                  <Button size="sm" variant="primary" disabled={busy || (!row.canStart && !row.inProgressAttemptId)}
                    onClick={() => void start(row)} data-testid="material-quiz-start">
                    {row.inProgressAttemptId ? "Làm tiếp" : "Làm quiz"}
                  </Button>
                </div>
              )}
            </li>
          );
        })}
      </ul>
    </section>
  );
}
