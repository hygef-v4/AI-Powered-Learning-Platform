"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { ModuleNeighbours, useModulePlace, useModuleScope } from "@/features/materials/ModulePlace";
import { Alert, Button, ClickableRow, Icon, PageHeader, Section, TableFrame, Td, Th } from "@/shared/ui";
import { getPracticeHistory, getPracticeQuiz, startAttempt } from "./api";
import { errorMessage, formatDateTime, formatPoints } from "./labels";
import type { PracticeQuizDetail, PracticeQuizRow } from "./types";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";

const detailHref = (quizId: string, classId: string) => `/quiz-practice/${encodeURIComponent(quizId)}?classId=${encodeURIComponent(classId)}`;

/**
 * Màn #43 Quiz Practice Detail (UC 50, BR-U11-03): cột trái Thông tin (số câu, thời gian) rồi Mô tả; cột phải thẻ Làm bài
 * (lượt đã dùng cạnh tiêu đề như trang bài tập) rồi Kết quả (các lần làm, cao tối đa rồi cuộn). Hết lượt thì thẻ Làm bài vẫn
 * còn, chỉ ẩn nút.
 * Link thiếu `classId` (thông báo "Có Quiz mới" của U14) thì tìm lớp đang học có quiz này trước (J1).
 */
export function QuizPracticeDetailPage({ quizId, classId }: { quizId: string; classId: string | null }) {
  return classId ? <PracticeDetail quizId={quizId} classId={classId} /> : <ClassResolver quizId={quizId} />;
}

/** Một lớp thì mở luôn; quiz của môn ở nhiều lớp thì cho chọn lớp; không có thì báo. */
function ClassResolver({ quizId }: { quizId: string }) {
  const router = useRouter();
  const [rows, setRows] = useState<PracticeQuizRow[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    getPracticeHistory()
      .then((list) => {
        if (!active) return;
        const matches = list.filter((row) => row.quizId === quizId);
        if (matches.length === 1 && matches[0]) router.replace(detailHref(quizId, matches[0].classId));
        else setRows(matches);
      })
      .catch((failure: unknown) => active && setError(errorMessage(failure)));
    return () => {
      active = false;
    };
  }, [quizId, router]);

  if (error) return <PageLoadError>{error}</PageLoadError>;
  if (rows === null) return <PageLoading label="Đang tải quiz…" />;
  if (rows.length === 0) {
    return (
      <Alert tone="warning" role="alert" action={<Button size="sm" onClick={() => router.push("/quiz-practice")}>Luyện tập Quiz</Button>}>
        Quiz không có trong lớp bạn đang học hoặc đã ngưng.
      </Alert>
    );
  }
  return (
    <>
      <PageHeader backHref="/quiz-practice" breadcrumb={[{ label: "Luyện tập Quiz", href: "/quiz-practice" }, { label: rows[0]?.title ?? "" }]}
        title={rows[0]?.title ?? "Quiz"} />
      <ul className="flex flex-col gap-2" data-testid="practice-detail-classes">
        {rows.map((row) => (
          <li key={row.classId}>
            <Button onClick={() => router.push(detailHref(quizId, row.classId))} data-testid="practice-detail-class">
              {row.classCode} · {row.className}
            </Button>
          </li>
        ))}
      </ul>
    </>
  );
}

function PracticeDetail({ quizId, classId }: { quizId: string; classId: string }) {
  const router = useRouter();
  // Quiz nằm trong module học liệu: tên module ở đầu trang, mục trước / sau ở đầu và cuối (sidebar là danh sách module).
  const scope = useModuleScope(undefined, classId);
  const place = useModulePlace(scope, (item) => item.kind === "quiz" && item.quiz.quizId === quizId);
  const [detail, setDetail] = useState<PracticeQuizDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let active = true;
    getPracticeQuiz(quizId, classId)
      .then((next) => active && setDetail(next))
      .catch((failure: unknown) => active && setError(errorMessage(failure)));
    return () => {
      active = false;
    };
  }, [quizId, classId]);

  const start = async () => {
    setBusy(true);
    setError(null);
    try {
      router.push(`/quiz-attempts/${(await startAttempt(quizId, classId)).attemptId}`);
    } catch (failure) {
      setError(errorMessage(failure));
      setBusy(false);
    }
  };

  if (!detail) {
    return error ? <PageLoadError>{error}</PageLoadError> : <PageLoading label="Đang tải quiz…" />;
  }
  const open = (attempt: PracticeQuizDetail["attempts"][number]) =>
    router.push(attempt.status === "SUBMITTED" ? `/quiz-attempts/${attempt.attemptId}/result` : `/quiz-attempts/${attempt.attemptId}`);
  // Hết lượt hay không làm được thì chỉ ẩn nút, không báo gì thêm.
  const action = detail.inProgressAttemptId || detail.canStart;

  return (
    <div className="flex flex-col gap-4">
      <PageHeader title={detail.title}
        breadcrumb={place.moduleTitle ? [{ label: place.moduleTitle }, { label: detail.title }]
          : [{ label: "Luyện tập Quiz", href: "/quiz-practice" }, { label: detail.title }]} />
      <ModuleNeighbours scope={scope} previous={place.previous} next={place.next} position="top" />
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      <div className="grid grid-cols-1 items-start gap-4 lg:grid-cols-[minmax(0,1fr)_360px]">
        <div className="flex min-w-0 flex-col gap-4">
          <Section title="Thông tin">
            <dl className="grid gap-x-6 gap-y-3 text-[13px] sm:grid-cols-2" data-testid="practice-detail-info">
              <div><dt className="text-xs text-muted">Số câu</dt><dd className="mt-0.5 font-semibold text-ink">{detail.questionCount} câu</dd></div>
              <div><dt className="text-xs text-muted">Thời gian</dt><dd className="mt-0.5 font-semibold text-ink">{detail.timeLimitMinutes ? `${detail.timeLimitMinutes} phút` : "Không giới hạn"}</dd></div>
            </dl>
          </Section>
          {detail.instructions && <Section title="Mô tả"><p className="text-[13.5px] leading-relaxed whitespace-pre-wrap text-body">{detail.instructions}</p></Section>}
        </div>
        <div className="flex min-w-0 flex-col gap-4">
          <Section title="Làm bài" className={action || detail.activityNotice ? undefined : "gap-0"}
            actions={<span className="text-[13px] text-muted tabular-nums" title="Lượt đã dùng / tối đa" data-testid="practice-detail-attempts">
              {detail.maxAttempts === null ? `${detail.attemptsUsed} lượt` : `${detail.attemptsUsed}/${detail.maxAttempts}`}</span>}>
            {detail.activityNotice && <Alert tone="info">{detail.activityNotice}</Alert>}
            {action && <Button variant="primary" disabled={busy} onClick={() => void start()} data-testid="practice-detail-start">
              {detail.inProgressAttemptId ? "Tiếp tục làm bài" : detail.attemptsUsed > 0 ? "Làm lại" : "Bắt đầu làm bài"}
            </Button>}
          </Section>
          <Section title="Kết quả">
            {/* Cao tối đa khoảng 6 lần làm; nhiều hơn thì cuộn trong thẻ, hàng tiêu đề đứng yên. */}
            <TableFrame filledHead className="max-h-[296px] overflow-y-auto overscroll-contain [&_thead_th]:sticky [&_thead_th]:top-0 [&_thead_th]:z-[1]">
              <thead><tr><Th>Lần</Th><Th>Bắt đầu</Th><Th>Điểm</Th><Th className="w-8"><span className="sr-only">Mở</span></Th></tr></thead>
              <tbody>
                {detail.attempts.length === 0 && <tr><Td colSpan={4} className="py-6 text-center! text-muted">Chưa có lần làm</Td></tr>}
                {detail.attempts.map((a) => (
                  <ClickableRow key={a.attemptId} data-testid="practice-detail-attempt" onClick={() => open(a)}>
                    <Td className="font-semibold whitespace-nowrap">Lần {a.attemptNo}</Td>
                    <Td className="whitespace-nowrap text-muted">{formatDateTime(a.startedAt)}</Td>
                    <Td className="whitespace-nowrap tabular-nums">{a.status !== "SUBMITTED" ? "Đang làm" : detail.showScoreAfterSubmit && a.score !== null ? `${formatPoints(a.score)} / ${formatPoints(a.maxScore)}` : "-"}</Td>
                    <Td className="text-faint"><Icon name="chevron_right" size={18} /></Td>
                  </ClickableRow>
                ))}
              </tbody>
            </TableFrame>
          </Section>
        </div>
      </div>
      <ModuleNeighbours scope={scope} previous={place.previous} next={place.next} />
    </div>
  );
}
