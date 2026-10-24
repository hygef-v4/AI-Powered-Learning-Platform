"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { useActivityTracker, type ActivityEventInput } from "@/shared/activity/useActivityTracker";
import { Alert, Button, cx, Dialog, Pill } from "@/shared/ui";
import { getAttempt, recordQuizActivity, saveAnswers, submitAttempt } from "./api";
import { errorCode, errorMessage, formatPoints } from "./labels";
import type { TakingView } from "./types";

export const AUTOSAVE_DELAY_MS = 2000;
type SaveState = "saved" | "dirty" | "saving" | "error";
/** `time-up`: máy chủ không nhận lưu vì đã quá hạn cộng ân hạn; `failed`: còn câu trả lời chưa lưu được. */
type SaveOutcome = "saved" | "time-up" | "failed";

function remainingSeconds(view: TakingView, skewMs: number): number | null {
  if (!view.deadlineAt) return null;
  return Math.max(0, Math.floor((new Date(view.deadlineAt).getTime() - (Date.now() + skewMs)) / 1000));
}

function clockText(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}

/**
 * Màn #44 Quiz Taking (UC 51; BR-U11-12, 13, 14): đề không có đáp án, tự lưu 2 giây sau lần chọn cuối và khi rời
 * trang kèm `answerVersion`, nộp có xác nhận, hết giờ thì lưu lần cuối rồi tự nộp.
 */
export function QuizTakingPage({ attemptId }: { attemptId: string }) {
  const router = useRouter();
  const [view, setView] = useState<TakingView | null>(null);
  const [answers, setAnswers] = useState<Record<string, string[]>>({});
  const [index, setIndex] = useState(0);
  const [saveState, setSaveState] = useState<SaveState>("saved");
  const [remaining, setRemaining] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [confirming, setConfirming] = useState(false);
  const [finishing, setFinishing] = useState<"manual" | "timeup" | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const answersRef = useRef(answers);
  const versionRef = useRef(0);
  const dirtyRef = useRef(false);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const savingRef = useRef<Promise<SaveOutcome> | null>(null);
  const skewRef = useRef(0);
  const finishingRef = useRef(false);

  // Giữ router trong ref để các hàm lưu/nộp không đổi khi router đổi danh tính (effect không chạy lại vô cớ).
  const routerRef = useRef(router);
  useEffect(() => {
    routerRef.current = router;
  }, [router]);
  const toResult = useCallback(() => routerRef.current.replace(`/quiz-attempts/${attemptId}/result`), [attemptId]);

  useEffect(() => {
    let active = true;
    getAttempt(attemptId)
      .then((next) => {
        if (!active) return;
        skewRef.current = new Date(next.serverTime).getTime() - Date.now();
        versionRef.current = next.answerVersion;
        answersRef.current = next.answers;
        dirtyRef.current = false;
        setView(next);
        setAnswers(next.answers);
        setSaveState("saved");
        setRemaining(remainingSeconds(next, skewRef.current));
      })
      .catch((failure: unknown) => {
        if (!active) return;
        if (errorCode(failure) === "ATTEMPT_SUBMITTED") toResult();
        else setError(errorMessage(failure));
      });
    return () => {
      active = false;
    };
  }, [attemptId, reloadKey, toResult]);

  /** Một lần lưu câu trả lời đang chờ kèm `answerVersion` hiện có. */
  const saveOnce = useCallback(async (): Promise<SaveOutcome> => {
    dirtyRef.current = false;
    setSaveState("saving");
    try {
      const saved = await saveAnswers(attemptId, answersRef.current, versionRef.current);
      versionRef.current = saved.answerVersion;
      setSaveState(dirtyRef.current ? "dirty" : "saved");
      return "saved";
    } catch (failure) {
      const code = errorCode(failure);
      if (code === "ATTEMPT_TIME_UP") return "time-up";
      if (code === "ANSWER_VERSION_CONFLICT") {
        setError(errorMessage(failure));
        setReloadKey((k) => k + 1);
      } else if (code === "ATTEMPT_SUBMITTED") {
        toResult();
      } else {
        dirtyRef.current = true;
        setSaveState("error");
        setError(errorMessage(failure));
      }
      return "failed";
    }
  }, [attemptId, toResult]);

  /**
   * Gửi câu trả lời đang chờ, từng lần một: lần lưu đang chạy xong (có `answerVersion` mới) rồi mới gửi lần sau, để
   * hai lần lưu không cùng gửi một version (409 làm mất lựa chọn mới) và nộp bài không chạy song song với lần lưu.
   */
  const flush = useCallback(async (): Promise<SaveOutcome> => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
    for (;;) {
      const pending = savingRef.current;
      if (pending) {
        const outcome = await pending;
        if (outcome !== "saved") return outcome;
        continue;
      }
      if (!dirtyRef.current) return "saved";
      const run = saveOnce().finally(() => {
        savingRef.current = null;
      });
      savingRef.current = run;
    }
  }, [saveOnce]);

  const finish = useCallback(async (mode: "manual" | "timeup") => {
    if (finishingRef.current) return;
    finishingRef.current = true;
    setFinishing(mode);
    setConfirming(false);
    // Còn câu trả lời chưa lưu được thì không nộp bằng bản cũ trên máy chủ; hết giờ thì máy chủ đã khóa lưu, vẫn nộp.
    if ((await flush()) === "failed" && mode === "manual") {
      finishingRef.current = false;
      setFinishing(null);
      return;
    }
    try {
      await submitAttempt(attemptId);
      toResult();
    } catch (failure) {
      // Hết giờ quá ân hạn: máy chủ tự nộp trong vòng 1 phút, vẫn chuyển sang kết quả.
      if (mode === "timeup" || errorCode(failure) === "ATTEMPT_SUBMITTED") {
        toResult();
        return;
      }
      finishingRef.current = false;
      setFinishing(null);
      setError(errorMessage(failure));
    }
  }, [attemptId, flush, toResult]);

  useEffect(() => {
    if (!view?.deadlineAt) return;
    const tick = setInterval(() => {
      const left = remainingSeconds(view, skewRef.current);
      setRemaining(left);
      if (left === 0) void finish("timeup");
    }, 1000);
    return () => clearInterval(tick);
  }, [view, finish]);

  useEffect(() => {
    const leave = () => void flush();
    window.addEventListener("pagehide", leave);
    return () => {
      window.removeEventListener("pagehide", leave);
      void flush();
    };
  }, [flush]);

  // G3 (K5): gửi sự kiện rời trang khi lượt bật ghi nhận hoạt động; lỗi gửi không chặn làm bài.
  const sendActivity = useCallback((events: ActivityEventInput[], { keepalive }: { keepalive: boolean }) =>
    recordQuizActivity(attemptId, events, keepalive), [attemptId]);
  useActivityTracker({ enabled: !!view?.activityMonitoringEnabled && finishing === null, send: sendActivity });

  const choose = (questionId: string, optionId: string, multi: boolean) => {
    const current = answersRef.current[questionId] ?? [];
    const next = multi
      ? (current.includes(optionId) ? current.filter((id) => id !== optionId) : [...current, optionId])
      : [optionId];
    const updated = { ...answersRef.current, [questionId]: next };
    answersRef.current = updated;
    dirtyRef.current = true;
    setAnswers(updated);
    setSaveState("dirty");
    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = setTimeout(() => void flush(), AUTOSAVE_DELAY_MS);
  };

  if (!view) {
    return error ? <Alert tone="danger" role="alert">{error}</Alert> : <p role="status" className="py-10 text-center text-sm text-muted">Đang tải đề…</p>;
  }
  const question = view.questions[index];
  const answered = (id: string) => (answers[id]?.length ?? 0) > 0;
  const unanswered = view.questions.map((q, i) => (answered(q.questionId) ? null : i + 1)).filter((n): n is number => n !== null);
  const saveLabel = { saved: "Đã tự động lưu", dirty: "Chưa lưu…", saving: "Đang lưu…", error: "Lưu chưa được" }[saveState];

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center gap-3 rounded-xl border border-line bg-white p-3">
        <Button size="sm" variant="ghost" onClick={() => void flush().then(() => router.push("/quiz-practice"))} data-testid="quiz-taking-exit">Thoát</Button>
        <span className="font-semibold text-ink">{view.title}</span>
        <Pill tone="info">Practice</Pill>
        <span className="text-xs text-muted" role="status" data-testid="quiz-taking-save-state">{saveLabel}</span>
        {remaining !== null && (
          <span className={cx("ml-auto font-mono text-sm font-bold", remaining < 60 ? "text-danger" : "text-ink")} data-testid="quiz-taking-timer">
            Còn {clockText(remaining)}
          </span>
        )}
        <Button variant="primary" className={remaining === null ? "ml-auto" : undefined} disabled={finishing !== null}
          onClick={() => setConfirming(true)} data-testid="quiz-taking-submit">Nộp bài</Button>
      </div>
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      {view.activityMonitoringEnabled && (
        <div data-testid="quiz-taking-activity-notice">
          <Alert tone="info">Quiz này ghi nhận hoạt động khi làm bài{view.activityNotice ? `: ${view.activityNotice}` : "."}</Alert>
        </div>
      )}
      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_240px]">
        {question && (
          <section className="rounded-xl border border-line bg-white p-4" aria-label={`Câu ${index + 1}`}>
            <p className="mb-2 text-xs text-muted">
              Câu {index + 1} / {view.questions.length} · {question.type === "MCQ_MULTI" ? "Nhiều đáp án" : "Một đáp án"} · {formatPoints(question.points)} điểm
            </p>
            <p className="mb-3 whitespace-pre-wrap text-[14px] text-ink">{question.stem}</p>
            <ul className="flex flex-col gap-2">
              {question.options.map((option) => {
                const multi = question.type === "MCQ_MULTI";
                const checked = (answers[question.questionId] ?? []).includes(option.optionId);
                return (
                  <li key={option.optionId}>
                    <label className={cx("flex cursor-pointer items-center gap-2 rounded-lg border px-3 py-2 text-[13px]",
                      checked ? "border-primary bg-primary-soft" : "border-line hover:bg-shell")}>
                      <input type={multi ? "checkbox" : "radio"} name={`q-${question.questionId}`} checked={checked} disabled={finishing !== null}
                        onChange={() => choose(question.questionId, option.optionId, multi)} data-testid={`quiz-taking-option-${option.optionId}`} />
                      {option.text}
                    </label>
                  </li>
                );
              })}
            </ul>
            <div className="mt-4 flex justify-between">
              <Button size="sm" disabled={index === 0} onClick={() => setIndex(index - 1)} data-testid="quiz-taking-prev">Câu trước</Button>
              <Button size="sm" disabled={index === view.questions.length - 1} onClick={() => setIndex(index + 1)} data-testid="quiz-taking-next">Câu tiếp</Button>
            </div>
          </section>
        )}
        <nav aria-label="Danh sách câu" className="rounded-xl border border-line bg-white p-3">
          <p className="mb-2 text-xs text-muted">Đã làm {view.questions.length - unanswered.length} / {view.questions.length}</p>
          <div className="flex flex-wrap gap-1.5">
            {view.questions.map((q, i) => (
              <button key={q.questionId} type="button" onClick={() => setIndex(i)} aria-current={i === index ? "true" : undefined}
                aria-label={`Câu ${i + 1}${answered(q.questionId) ? " đã trả lời" : ""}`}
                className={cx("h-8 w-8 rounded-lg border text-xs font-semibold",
                  i === index ? "border-primary text-primary" : answered(q.questionId) ? "border-success bg-success-soft text-success" : "border-line text-body")}>
                {i + 1}
              </button>
            ))}
          </div>
        </nav>
      </div>
      {confirming && (
        <Dialog open onClose={() => setConfirming(false)} title="Nộp bài?" tone="warning"
          footer={<>
            <Button onClick={() => setConfirming(false)}>Làm tiếp</Button>
            <Button variant="primary" onClick={() => void finish("manual")} data-testid="quiz-taking-confirm-submit">Nộp bài</Button>
          </>}>
          <div className="flex flex-col gap-2 text-[13px]" data-testid="quiz-taking-confirm">
            {unanswered.length > 0
              ? <p>Còn {unanswered.length} câu chưa trả lời: câu {unanswered.join(", ")}. Câu chưa trả lời được tính sai.</p>
              : <p>Bạn đã trả lời tất cả các câu.</p>}
            {remaining !== null && <p>Thời gian còn lại: {clockText(remaining)}.</p>}
            <p>Sau khi nộp, lượt này kết thúc.</p>
          </div>
        </Dialog>
      )}
      {finishing === "timeup" && (
        <Dialog open onClose={() => undefined} dismissible={false} title="Hết thời gian làm bài" tone="warning">
          <p className="text-[13px]" data-testid="quiz-taking-timeup">Các câu đã chọn được tự lưu và nộp. Đang chuyển sang trang kết quả…</p>
        </Dialog>
      )}
    </div>
  );
}
