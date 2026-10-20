"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Alert, Button, PageHeader, Section } from "@/shared/ui";
import { getResult, startAttempt } from "./api";
import { errorCode, errorMessage, formatDateTime, formatDuration, formatPoints } from "./labels";
import { ResultQuestions } from "./ResultQuestions";
import type { ResultView } from "./types";

/** Màn #45 Quiz Result (UC 52, BR-U11-20, 21): điểm và đáp án theo cài đặt; kết quả luyện tập không vào sổ điểm. */
export function QuizResultPage({ attemptId }: { attemptId: string }) {
  const router = useRouter();
  const [result, setResult] = useState<ResultView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let active = true;
    getResult(attemptId)
      .then((next) => active && setResult(next))
      .catch((failure: unknown) => {
        if (!active) return;
        if (errorCode(failure) === "ATTEMPT_IN_PROGRESS") router.replace(`/quiz-attempts/${attemptId}`);
        else setError(errorMessage(failure));
      });
    return () => {
      active = false;
    };
  }, [attemptId, router]);

  if (!result) {
    return error ? <Alert tone="danger" role="alert">{error}</Alert> : <p role="status" className="py-10 text-center text-sm text-muted">Đang tải kết quả…</p>;
  }
  const detailHref = `/quiz-practice/${result.quizId}?classId=${encodeURIComponent(result.classId)}`;
  const retry = async () => {
    setBusy(true);
    try {
      router.push(`/quiz-attempts/${(await startAttempt(result.quizId, result.classId)).attemptId}`);
    } catch (failure) {
      setError(errorMessage(failure));
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader backHref={detailHref} breadcrumb={[{ label: "Luyện tập Quiz", href: "/quiz-practice" }, { label: result.title, href: detailHref }]}
        title={`Kết quả · ${result.title}`}
        subtitle={`Lần ${result.attemptNo} · nộp lúc ${formatDateTime(result.submittedAt)} · làm trong ${formatDuration(result.startedAt, result.submittedAt)}`}
        actions={<div className="flex gap-2">
          <Button onClick={() => router.push("/quiz-practice")}>Về danh sách</Button>
          {result.canRetry && <Button variant="primary" disabled={busy} onClick={() => void retry()} data-testid="result-retry">Làm lại</Button>}
        </div>} />
      {error && <Alert tone="danger" role="alert" className="mb-3">{error}</Alert>}
      {result.showScore ? (
        <Section title="Điểm" icon="check_circle">
          <p className="text-2xl font-bold text-ink" data-testid="result-score">{formatPoints(result.score)} / {formatPoints(result.maxScore)}</p>
          <p className="text-[13px] text-body">Đúng {result.correctCount} · Sai {result.wrongCount} · Bỏ trống {result.blankCount}</p>
          <p className="text-xs text-muted">Practice · không tính vào sổ điểm</p>
        </Section>
      ) : (
        <Section title="Đã nộp bài" icon="check_circle">
          <p className="text-[13px] text-body" data-testid="result-hidden">Quiz này không hiện điểm sau khi nộp.</p>
        </Section>
      )}
      <div className="mt-4">
        {result.showAnswers
          ? <ResultQuestions questions={result.questions} showScore={result.showScore} />
          : <p className="text-[13px] text-muted">Quiz này không hiện đáp án sau khi nộp.</p>}
      </div>
    </>
  );
}
