"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Alert, Button, PageHeader, Pill, Section, TableFrame, Td, Th } from "@/shared/ui";
import { getPracticeHistory, getPracticeQuiz, startAttempt } from "./api";
import { attemptsText, errorMessage, formatDateTime, formatDuration, formatPoints } from "./labels";
import { quizMaterialSections } from "./slots";
import type { PracticeQuizDetail, PracticeQuizRow } from "./types";

const detailHref = (quizId: string, classId: string) => `/quiz-practice/${encodeURIComponent(quizId)}?classId=${encodeURIComponent(classId)}`;

/**
 * Màn #43 Quiz Practice Detail (UC 50, BR-U11-03): thông tin, các lần làm của mình, nút Làm quiz hoặc lý do không làm được.
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

  if (error) return <Alert tone="danger" role="alert">{error}</Alert>;
  if (rows === null) return <p role="status" className="py-10 text-center text-sm text-muted">Đang tải quiz…</p>;
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
        title={rows[0]?.title ?? "Quiz"} subtitle="Quiz của môn mở ở nhiều lớp bạn đang học. Chọn lớp để làm quiz." />
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
    return error ? <Alert tone="danger" role="alert">{error}</Alert> : <p role="status" className="py-10 text-center text-sm text-muted">Đang tải quiz…</p>;
  }
  const submitted = detail.attempts.filter((a) => a.status === "SUBMITTED");

  return (
    <>
      <PageHeader backHref="/quiz-practice" breadcrumb={[{ label: "Luyện tập Quiz", href: "/quiz-practice" }, { label: detail.classCode }]}
        title={<span className="inline-flex items-center gap-2">{detail.title} <Pill tone="info">Practice</Pill></span>}
        actions={detail.canStart && (
          <Button variant="primary" disabled={busy} onClick={() => void start()} data-testid="practice-detail-start">
            {detail.inProgressAttemptId ? "Tiếp tục" : detail.attemptsUsed > 0 ? "Làm lại" : "Làm quiz"}
          </Button>
        )} />
      {error && <Alert tone="danger" role="alert" className="mb-3">{error}</Alert>}
      {detail.blockedReason && <Alert tone="warning" className="mb-3" role="status">{detail.blockedReason}</Alert>}
      {detail.canStart && !detail.inProgressAttemptId && (
        <p className="mb-3 text-[13px] text-muted">
          {detail.attemptsLeft === null ? "Không giới hạn lượt" : `Còn ${detail.attemptsLeft} / ${detail.maxAttempts} lượt`} · không tính vào sổ điểm
        </p>
      )}
      {detail.activityNotice && <Alert tone="info" className="mb-3">Quiz này ghi nhận hoạt động khi làm bài: {detail.activityNotice}</Alert>}
      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_300px]">
        <Section title={`Các lần làm (${submitted.length})`} icon="history">
          {detail.attempts.length === 0
            ? <p className="text-[13px] text-muted">Bạn chưa làm quiz này.</p>
            : <TableFrame>
              <thead><tr><Th>Lần</Th><Th>Bắt đầu</Th><Th>Thời gian</Th>{detail.showScoreAfterSubmit && <Th>Điểm</Th>}<Th></Th></tr></thead>
              <tbody>
                {detail.attempts.map((a) => (
                  <tr key={a.attemptId} data-testid="practice-detail-attempt">
                    <Td>Lần {a.attemptNo}</Td>
                    <Td className="whitespace-nowrap">{formatDateTime(a.startedAt)}</Td>
                    <Td>{a.status === "SUBMITTED" ? formatDuration(a.startedAt, a.submittedAt) : "Đang làm"}</Td>
                    {detail.showScoreAfterSubmit && <Td>{a.score === null ? "—" : `${formatPoints(a.score)} / ${formatPoints(a.maxScore)}`}</Td>}
                    <Td>
                      {a.status === "SUBMITTED"
                        ? <Button size="sm" onClick={() => router.push(`/quiz-attempts/${a.attemptId}/result`)}>Xem kết quả</Button>
                        : <Button size="sm" onClick={() => router.push(`/quiz-attempts/${a.attemptId}`)}>Tiếp tục</Button>}
                    </Td>
                  </tr>
                ))}
              </tbody>
            </TableFrame>}
          {detail.referenceMaterialIds.length > 0 && (
            <div className="mt-4">
              <SlotOutlet slot={quizMaterialSections} props={{ classId, materialIds: detail.referenceMaterialIds }} />
            </div>
          )}
        </Section>
        <Section title="Thông tin" icon="info">
          <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1.5 text-[13px]">
            <dt className="text-muted">Lớp</dt><dd>{detail.classCode}</dd>
            <dt className="text-muted">Số câu</dt><dd>{detail.questionCount} câu · {formatPoints(detail.maxScore)} điểm</dd>
            <dt className="text-muted">Thời gian</dt><dd>{detail.timeLimitMinutes ? `${detail.timeLimitMinutes} phút` : "Không giới hạn"}</dd>
            <dt className="text-muted">Số lượt</dt><dd data-testid="practice-detail-attempts">{attemptsText(detail.attemptsUsed, detail.maxAttempts)}</dd>
            {detail.showScoreAfterSubmit && <><dt className="text-muted">Điểm cao nhất</dt><dd>{formatPoints(detail.bestScore)}</dd></>}
            <dt className="text-muted">Đáp án</dt><dd>{detail.showCorrectAnswers ? "Hiện sau khi nộp" : "Không hiện"}</dd>
          </dl>
          {detail.instructions && <p className="mt-3 whitespace-pre-wrap text-[13px] text-body">{detail.instructions}</p>}
        </Section>
      </div>
    </>
  );
}
