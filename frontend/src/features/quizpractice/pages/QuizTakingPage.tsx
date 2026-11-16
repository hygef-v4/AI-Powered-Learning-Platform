"use client";

import { useRouter } from "next/navigation";
import { useUrlState } from "@/shared/navigation/useUrlState";
import { useCallback, useEffect, useRef, useState } from "react";
import { useActivityTracker, type ActivityEventInput } from "@/shared/activity/useActivityTracker";
import { Alert, Button, Card, cx, Dialog, Icon, Pill } from "@/shared/ui";
import { getAttempt, recordQuizActivity, saveAnswers, submitAttempt } from "../api";
import { QuestionNav, QuestionPrevNext, resolveQuestionIndex } from "../components/QuestionPager";
import { readErrorCode, describeError, formatPoints, describePenaltyRule } from "../model/labels";
import type { TakingView } from "../model/types";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import { msg } from "@/i18n/msg";

export const AUTOSAVE_DELAY_MS = 2000;
/** Không để việc gửi nốt sự kiện rời trang giữ chân lần nộp bài. */
const ACTIVITY_DRAIN_LIMIT_MS = 2000;
type SaveState = "saved" | "dirty" | "saving" | "error";
/** `time-up`: máy chủ không nhận lưu vì đã quá hạn cộng ân hạn; `failed`: còn câu trả lời chưa lưu được. */
type SaveOutcome = "saved" | "time-up" | "failed";

function calculateRemainingSeconds(view: TakingView, skewMs: number): number | null {
  if (!view.deadlineAt) return null;
  return Math.max(0, Math.floor((new Date(view.deadlineAt).getTime() - (Date.now() + skewMs)) / 1000));
}

/** Mốc thời gian đọc cho trình đọc màn hình; chữ chỉ đổi khi qua mốc nên mỗi mốc được đọc một lần. */
function resolveTimeMilestone(seconds: number | null): string {
  if (seconds === null || seconds > 300) return "";
  if (seconds > 60) return msg("quizpractice.pages.QuizTakingPage.conDuoi5Phut");
  if (seconds > 10) return msg("quizpractice.pages.QuizTakingPage.conDuoi1Phut");
  return msg("quizpractice.pages.QuizTakingPage.con10Giay");
}

function formatClock(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}

/**
 * Màn #44 Quiz Taking (UC 51; BR-U11-12, 13, 14): đề không có đáp án, mỗi câu một trang (câu đang xem giữ trên URL để tải
 * lại không mất chỗ), tự lưu 2 giây sau lần chọn cuối và khi rời trang kèm `answerVersion`, nộp có xác nhận, hết giờ thì
 * lưu lần cuối rồi tự nộp. Thanh đồng hồ và nút Nộp bài dính dưới thanh trên của ứng dụng; lưu lỗi thì có nút Thử lại.
 */
export function QuizTakingPage({ attemptId }: { attemptId: string }) {
  const router = useRouter();
  const [view, setView] = useState<TakingView | null>(null);
  const [answers, setAnswers] = useState<Record<string, string[]>>({});
  const [questionParam, setQuestionParam] = useUrlState<string>("q", "1", undefined, "replace");
  const [saveState, setSaveState] = useState<SaveState>("saved");
  const [remaining, setRemaining] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [confirming, setConfirming] = useState(false);
  const [leaving, setLeaving] = useState(false);
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
        setRemaining(calculateRemainingSeconds(next, skewRef.current));
      })
      .catch((failure: unknown) => {
        if (!active) return;
        if (readErrorCode(failure) === "ATTEMPT_SUBMITTED") toResult();
        else setError(describeError(failure));
      });
    return () => {
      active = false;
    };
  }, [attemptId, reloadKey, toResult]);

  // G3 (K5): gửi sự kiện rời trang khi lượt bật ghi nhận hoạt động; lỗi gửi không chặn làm bài.
  const sendActivity = useCallback((events: ActivityEventInput[], { keepalive }: { keepalive: boolean }) =>
    recordQuizActivity(attemptId, events, keepalive), [attemptId]);
  const drainActivity = useActivityTracker({ enabled: !!view?.activityMonitoringEnabled, send: sendActivity, trackFocus: true });

  /** Một lần lưu câu trả lời đang chờ kèm `answerVersion` hiện có. */
  const saveOnce = useCallback(async (keepalive: boolean): Promise<SaveOutcome> => {
    dirtyRef.current = false;
    setSaveState("saving");
    try {
      const saved = await saveAnswers(attemptId, answersRef.current, versionRef.current, keepalive);
      versionRef.current = saved.answerVersion;
      setSaveState(dirtyRef.current ? "dirty" : "saved");
      return "saved";
    } catch (failure) {
      const code = readErrorCode(failure);
      if (code === "ATTEMPT_TIME_UP") return "time-up";
      if (code === "ANSWER_VERSION_CONFLICT") {
        setError(describeError(failure));
        setReloadKey((k) => k + 1);
      } else if (code === "ATTEMPT_SUBMITTED") {
        toResult();
      } else {
        dirtyRef.current = true;
        setSaveState("error");
        setError(describeError(failure));
      }
      return "failed";
    }
  }, [attemptId, toResult]);

  /**
   * Gửi câu trả lời đang chờ, từng lần một: lần lưu đang chạy xong (có `answerVersion` mới) rồi mới gửi lần sau, để
   * hai lần lưu không cùng gửi một version (409 làm mất lựa chọn mới) và nộp bài không chạy song song với lần lưu.
   * `keepalive` khi rời hoặc ẩn trang: request vẫn được gửi xong dù trang đóng.
   */
  const flush = useCallback(async (keepalive = false): Promise<SaveOutcome> => {
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
      const run = saveOnce(keepalive).finally(() => {
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
    // Lượt đã nộp thì máy chủ không nhận sự kiện nữa: gửi nốt trước để số lần rời trang được tính đủ (BR-U11-33).
    await Promise.race([drainActivity(), new Promise((resolve) => setTimeout(resolve, ACTIVITY_DRAIN_LIMIT_MS))]);
    try {
      await submitAttempt(attemptId);
      toResult();
    } catch (failure) {
      // Hết giờ quá ân hạn: máy chủ tự nộp trong vòng 1 phút, vẫn chuyển sang kết quả.
      if (mode === "timeup" || readErrorCode(failure) === "ATTEMPT_SUBMITTED") {
        toResult();
        return;
      }
      finishingRef.current = false;
      setFinishing(null);
      setError(describeError(failure));
    }
  }, [attemptId, drainActivity, flush, toResult]);

  useEffect(() => {
    if (!view?.deadlineAt) return;
    const tick = setInterval(() => {
      const left = calculateRemainingSeconds(view, skewRef.current);
      setRemaining(left);
      if (left === 0) void finish("timeup");
    }, 1000);
    return () => clearInterval(tick);
  }, [view, finish]);

  // Rời trang, đóng tab hoặc chuyển ứng dụng trên điện thoại (trang bị ẩn, có thể bị hủy mà không có pagehide).
  useEffect(() => {
    const leave = () => void flush(true);
    const handleHidden = () => {
      if (document.visibilityState === "hidden") void flush(true);
    };
    window.addEventListener("pagehide", leave);
    document.addEventListener("visibilitychange", handleHidden);
    return () => {
      window.removeEventListener("pagehide", leave);
      document.removeEventListener("visibilitychange", handleHidden);
      void flush();
    };
  }, [flush]);

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
    return error ? <PageLoadError>{error}</PageLoadError> : <PageLoading label={msg("quizpractice.pages.QuizTakingPage.dangTaiDe")} />;
  }
  const index = resolveQuestionIndex(questionParam, view.questions.length);
  const setIndex = (next: number) => setQuestionParam(String(next + 1));
  const question = view.questions[index];
  const isAnswered = (id: string) => (answers[id]?.length ?? 0) > 0;
  const unanswered = view.questions.map((q, i) => (isAnswered(q.questionId) ? null : i + 1)).filter((n): n is number => n !== null);
  const saveLabel = { saved: msg("quizpractice.pages.QuizTakingPage.daTuDongLuu"), dirty: msg("quizpractice.pages.QuizTakingPage.chuaLuu"), saving: msg("quizpractice.pages.QuizTakingPage.dangLuu"), error: msg("quizpractice.pages.QuizTakingPage.chuaLuuDuoc") }[saveState];
  const exit = () => void flush().then((outcome) => {
    if (outcome === "saved") router.push("/quiz-practice");
    else setLeaving(false);
  });

  return (
    <div className="flex flex-col gap-4">
      <TakingToolbar title={view.title} saveState={saveState} saveLabel={saveLabel} remaining={remaining}
        busy={finishing !== null} onExit={() => (remaining === null ? exit() : setLeaving(true))}
        onRetrySave={() => void flush()} onSubmit={() => setConfirming(true)} />
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      {view.activityMonitoringEnabled && (
        <div data-testid="quiz-taking-activity-notice">
          <Alert tone="info">
            {msg("quizpractice.pages.QuizTakingPage.quizNayGhiNhanHoatDong")}{view.activityNotice ? `: ${view.activityNotice}` : "."}
            {view.leavePenalty && <span className="block" data-testid="quiz-taking-penalty-rule">{describePenaltyRule(view.leavePenalty)}</span>}
          </Alert>
        </div>
      )}
      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_240px]">
        {question && (
          <QuestionCard question={question} index={index} total={view.questions.length} answers={answers}
            busy={finishing !== null} choose={choose} setIndex={setIndex} />
        )}
        <QuestionListCard count={view.questions.length} index={index} doneCount={view.questions.length - unanswered.length}
          answered={(i) => isAnswered(view.questions[i]?.questionId ?? "")} setIndex={setIndex} />
      </div>
      {confirming && (
        <Dialog open onClose={() => setConfirming(false)} title={msg("quizpractice.pages.QuizTakingPage.nopBai")} tone="warning"
          footer={<>
            <Button onClick={() => setConfirming(false)}>{msg("quizpractice.pages.QuizTakingPage.lamTiep")}</Button>
            <Button variant="primary" onClick={() => void finish("manual")} data-testid="quiz-taking-confirm-submit">{msg("quizpractice.pages.QuizTakingPage.nopBai2")}</Button>
          </>}>
          <div className="flex flex-col gap-2 text-[13px]" data-testid="quiz-taking-confirm">
            {unanswered.length > 0
              ? <p>{msg("quizpractice.pages.QuizTakingPage.con")}{unanswered.length} {msg("quizpractice.pages.QuizTakingPage.cauChuaTraLoiCau")}{unanswered.join(", ")}{msg("quizpractice.pages.QuizTakingPage.cauChuaTraLoiDuocTinh")}</p>
              : <p>{msg("quizpractice.pages.QuizTakingPage.banDaTraLoiTatCa")}</p>}
            {remaining !== null && <p>{msg("quizpractice.pages.QuizTakingPage.thoiGianConLai")}{formatClock(remaining)}.</p>}
            <p>{msg("quizpractice.pages.QuizTakingPage.sauKhiNopLuotNayKet")}</p>
          </div>
        </Dialog>
      )}
      {leaving && (
        <Dialog open onClose={() => setLeaving(false)} title={msg("quizpractice.pages.QuizTakingPage.thoatKhoiBai")} tone="warning"
          footer={<Button variant="primary" onClick={exit} data-testid="quiz-taking-confirm-exit">{msg("quizpractice.pages.QuizTakingPage.thoat")}</Button>}>
          <p className="text-[13px]" data-testid="quiz-taking-exit-notice">
            {msg("quizpractice.pages.QuizTakingPage.cacCauDaChonDuocLuu")}</p>
        </Dialog>
      )}
      {finishing === "timeup" && (
        <Dialog open onClose={() => undefined} dismissible={false} title={msg("quizpractice.pages.QuizTakingPage.hetThoiGianLamBai")} tone="warning">
          <p className="text-[13px]" data-testid="quiz-taking-timeup">{msg("quizpractice.pages.QuizTakingPage.cacCauDaChonDuocTu")}</p>
        </Dialog>
      )}
    </div>
  );
}

/** Thanh dính trên cùng khi làm quiz: thoát, tên bài, trạng thái lưu, đồng hồ và nút Nộp bài. */
function TakingToolbar({ title, saveState, saveLabel, remaining, busy, onExit, onRetrySave, onSubmit }: {
  title: string;
  saveState: SaveState;
  saveLabel: string;
  remaining: number | null;
  busy: boolean;
  onExit: () => void;
  onRetrySave: () => void;
  onSubmit: () => void;
}) {
  return (
    <Card padding="sm" lifted className="sticky top-[4.25rem] z-20 flex flex-wrap items-center gap-3">
      <Button size="sm" variant="ghost" onClick={onExit} data-testid="quiz-taking-exit">{msg("quizpractice.pages.QuizTakingPage.thoat")}</Button>
      <span className="font-semibold text-ink">{title}</span>
      <Pill tone="info">{msg("quizpractice.pages.QuizTakingPage.luyenTap")}</Pill>
      <span className={cx("text-xs", saveState === "error" ? "font-semibold text-warning" : "text-muted")} role="status" data-testid="quiz-taking-save-state">
        {saveLabel}
      </span>
      {saveState === "error" && (
        <Button size="sm" icon="refresh" disabled={busy} onClick={onRetrySave} data-testid="quiz-taking-save-retry">
          {msg("quizpractice.pages.QuizTakingPage.thuLai")}</Button>
      )}
      {remaining !== null && (
        <span
          className={cx("ml-auto inline-flex items-center gap-1 font-mono text-sm font-bold", remaining < 60 ? "text-danger" : "text-ink")}
          data-testid="quiz-taking-timer"
        >
          {remaining < 60 && <Icon name="warning" size={15} />}
          {msg("quizpractice.pages.QuizTakingPage.con")}{formatClock(remaining)}
        </span>
      )}
      <span className="sr-only" aria-live="assertive" data-testid="quiz-taking-time-milestone">{resolveTimeMilestone(remaining)}</span>
      <Button variant="primary" className={remaining === null ? "ml-auto" : undefined} disabled={busy}
        onClick={onSubmit} data-testid="quiz-taking-submit">{msg("quizpractice.pages.QuizTakingPage.nopBai2")}</Button>
    </Card>
  );
}

/** Thẻ câu hỏi đang làm: đề, các đáp án chọn được và nút chuyển câu. */
function QuestionCard({ question, index, total, answers, busy, choose, setIndex }: {
  question: TakingView["questions"][number];
  index: number;
  total: number;
  answers: Record<string, string[]>;
  busy: boolean;
  choose: (questionId: string, optionId: string, multi: boolean) => void;
  setIndex: (next: number) => void;
}) {
  return (
    <Card as="section" aria-label={msg("quizpractice.pages.QuizTakingPage.cau", { value: String(index + 1) })}>
      <p className="mb-2 text-xs text-muted">
        {msg("quizpractice.pages.QuizTakingPage.cau2")}{index + 1} / {total} · {question.type === "MCQ_MULTI" ? msg("quizpractice.pages.QuizTakingPage.nhieuDapAn") : msg("quizpractice.pages.QuizTakingPage.motDapAn")} · {formatPoints(question.points)} {msg("quizpractice.pages.QuizTakingPage.diem")}</p>
      <p className="mb-3 whitespace-pre-wrap text-[14px] text-ink">{question.stem}</p>
      <ul className="flex flex-col gap-2">
        {question.options.map((option) => {
          const multi = question.type === "MCQ_MULTI";
          const checked = (answers[question.questionId] ?? []).includes(option.optionId);
          return (
            <li key={option.optionId}>
              <label className={cx("flex min-h-11 cursor-pointer items-center gap-2.5 rounded-lg border px-3 py-2.5 text-[14px]",
                checked ? "border-primary bg-primary-soft" : "border-line hover:bg-shell")}>
                <input type={multi ? "checkbox" : "radio"} name={`q-${question.questionId}`} checked={checked} disabled={busy}
                  onChange={() => choose(question.questionId, option.optionId, multi)} data-testid={`quiz-taking-option-${option.optionId}`} />
                {option.text}
              </label>
            </li>
          );
        })}
      </ul>
      <QuestionPrevNext count={total} index={index} onChange={setIndex} testIdPrefix="quiz-taking" />
    </Card>
  );
}

/** Thẻ danh sách câu bên phải: số câu đã làm và dải số chuyển nhanh. */
function QuestionListCard({ count, index, doneCount, answered, setIndex }: {
  count: number;
  index: number;
  doneCount: number;
  answered: (i: number) => boolean;
  setIndex: (next: number) => void;
}) {
  return (
    <Card as="nav" padding="sm" className="order-first min-w-0 lg:order-none" aria-label={msg("quizpractice.pages.QuizTakingPage.danhSachCau")}>
      <p className="mb-2 text-xs text-muted">{msg("quizpractice.pages.QuizTakingPage.daLam")}{doneCount} / {count}</p>
      <QuestionNav count={count} index={index} onChange={setIndex}
        toneOf={(i) => (i === index ? "current" : answered(i) ? "done" : "blank")}
        labelOf={(i) => msg("quizpractice.pages.QuizTakingPage.cau3", { value: String(i + 1), value2: String(answered(i) ? msg("quizpractice.pages.QuizTakingPage.daTraLoi") : "") })} />
    </Card>
  );
}
