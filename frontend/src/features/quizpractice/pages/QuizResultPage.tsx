"use client";

import { useRouter } from "next/navigation";
import { useUrlState } from "@/shared/navigation/useUrlState";
import { useEffect, useState } from "react";
import { Alert, Button, PageHeader, Section } from "@/shared/ui";
import { getResult, startAttempt } from "../api";
import { readErrorCode, describeError, formatDateTime, formatDuration, formatPoints, describePenaltyResult, submitModeText } from "../model/labels";
import { resolveQuestionIndex } from "../components/QuestionPager";
import { ResultQuestions } from "../components/ResultQuestions";
import type { ResultView } from "../model/types";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import { msg } from "@/i18n/msg";

/**
 * Màn #45 Quiz Result (UC 52, BR-U11-20, 21, 33): điểm, quyết định trừ điểm khi rời trang và đáp án theo cài đặt; xem lại mỗi câu một
 * trang; kết quả luyện tập không vào sổ điểm.
 */
export function QuizResultPage({ attemptId }: { attemptId: string }) {
  const router = useRouter();
  const [result, setResult] = useState<ResultView | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [questionParam, setQuestionParam] = useUrlState<string>("q", "1", undefined, "replace");

  useEffect(() => {
    let active = true;
    getResult(attemptId)
      .then((next) => active && setResult(next))
      .catch((failure: unknown) => {
        if (!active) return;
        if (readErrorCode(failure) === "ATTEMPT_IN_PROGRESS") router.replace(`/quiz-attempts/${attemptId}`);
        else setError(describeError(failure));
      });
    return () => {
      active = false;
    };
  }, [attemptId, router]);

  if (!result) {
    return error ? <PageLoadError>{error}</PageLoadError> : <PageLoading label={msg("quizpractice.pages.QuizResultPage.loadingResult")} />;
  }
  const detailHref = `/quiz-practice/${result.quizId}?classId=${encodeURIComponent(result.classId)}`;
  const retry = async () => {
    setBusy(true);
    try {
      router.push(`/quiz-attempts/${(await startAttempt(result.quizId, result.classId)).attemptId}`);
    } catch (failure) {
      setError(describeError(failure));
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader backHref={detailHref} breadcrumb={[{ label: msg("quizpractice.pages.QuizResultPage.quizPractice"), href: "/quiz-practice" }, { label: result.title, href: detailHref }]}
        title={msg("quizpractice.pages.QuizResultPage.result", { title: String(result.title) })}
        subtitle={[msg("quizpractice.pages.QuizResultPage.attempt", { attemptNo: String(result.attemptNo) }), msg("quizpractice.pages.QuizResultPage.submittedAt", { value: String(formatDateTime(result.submittedAt)) }),
          msg("quizpractice.pages.QuizResultPage.took", { value: String(formatDuration(result.startedAt, result.submittedAt)) }), submitModeText(result.submitMode)].filter(Boolean).join(" · ")}
        actions={<div className="flex gap-2">
          <Button onClick={() => router.push("/quiz-practice")}>{msg("quizpractice.pages.QuizResultPage.backToList")}</Button>
          {result.canRetry && <Button variant="primary" disabled={busy} onClick={() => void retry()} data-testid="result-retry">{msg("quizpractice.pages.QuizResultPage.retake")}</Button>}
        </div>} />
      {error && <Alert tone="danger" role="alert" className="mb-3">{error}</Alert>}
      {result.showScore ? (
        <Section title={msg("quizpractice.pages.QuizResultPage.score")} icon="check_circle">
          <p className="text-2xl font-bold text-ink" data-testid="result-score">{formatPoints(result.score)} / {formatPoints(result.maxScore)}</p>
          <p className="text-[13px] text-body">{msg("quizpractice.pages.QuizResultPage.correct")}{result.correctCount}{msg("quizpractice.pages.QuizResultPage.wrong")}{result.wrongCount}{msg("quizpractice.pages.QuizResultPage.unanswered")}{result.blankCount}</p>
          {result.penalty && describePenaltyResult(result.penalty) && (
            <p className="text-[13px] text-body" data-testid="result-penalty">{describePenaltyResult(result.penalty)}</p>
          )}
          <p className="text-xs text-muted">{msg("quizpractice.pages.QuizResultPage.practiceNotCountedInThe")}</p>
        </Section>
      ) : (
        <Section title={msg("quizpractice.pages.QuizResultPage.submitted")} icon="check_circle">
          <p className="text-[13px] text-body" data-testid="result-hidden">{msg("quizpractice.pages.QuizResultPage.thisQuizDoesNotShow")}</p>
        </Section>
      )}
      <div className="mt-4">
        {result.showAnswers
          ? <ResultQuestions questions={result.questions} showScore={result.showScore}
              index={resolveQuestionIndex(questionParam, result.questions.length)} onIndex={(next) => setQuestionParam(String(next + 1))} />
          : <p className="text-[13px] text-muted">{msg("quizpractice.pages.QuizResultPage.thisQuizDoesNotShow2")}</p>}
      </div>
    </>
  );
}
