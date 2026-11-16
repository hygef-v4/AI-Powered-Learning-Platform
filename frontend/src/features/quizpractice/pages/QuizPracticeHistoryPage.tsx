"use client";

import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { Alert, Button, PageHeader, Pill, Select, TableFrame, TableSkeletonRows, Td, Th } from "@/shared/ui";
import { getPracticeHistory, startAttempt } from "../api";
import { formatAttempts, describeError, formatDateTime, formatPoints } from "../model/labels";
import type { PracticeQuizRow } from "../model/types";
import { msg } from "@/i18n/msg";

const buildDetailHref = (row: PracticeQuizRow) => `/quiz-practice/${row.quizId}?classId=${encodeURIComponent(row.classId)}`;

/** Màn #42 Quiz Practice History (UC 49, BR-U11-02): Quiz đang mở và Quiz đã làm của các lớp đang học. */
export function QuizPracticeHistoryPage() {
  const router = useRouter();
  const [rows, setRows] = useState<PracticeQuizRow[] | null>(null);
  const [classId, setClassId] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let active = true;
    getPracticeHistory()
      .then((list) => active && setRows(list))
      .catch((failure: unknown) => active && setError(describeError(failure)));
    return () => {
      active = false;
    };
  }, []);

  const classes = useMemo(() => {
    const seen = new Map<string, string>();
    rows?.forEach((row) => seen.set(row.classId, row.subjectCode ? `${row.subjectCode} · ${row.classCode}` : row.classCode));
    return [...seen.entries()];
  }, [rows]);
  const shown = rows?.filter((row) => !classId || row.classId === classId) ?? [];

  const start = async (row: PracticeQuizRow) => {
    setBusy(true);
    setError(null);
    try {
      router.push(`/quiz-attempts/${(await startAttempt(row.quizId, row.classId)).attemptId}`);
    } catch (failure) {
      setError(describeError(failure));
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader title={msg("quizpractice.pages.QuizPracticeHistoryPage.luyenTapQuiz")}
        actions={classes.length > 1 && (
          <Select aria-label={msg("quizpractice.pages.QuizPracticeHistoryPage.locTheoLop")} value={classId} onChange={(event) => setClassId(event.target.value)} data-testid="practice-history-class">
            <option value="">{msg("quizpractice.pages.QuizPracticeHistoryPage.moiLop")}</option>
            {classes.map(([id, label]) => <option key={id} value={id}>{label}</option>)}
          </Select>
        )} />
      {error && <Alert tone="danger" role="alert" className="mb-3">{error}</Alert>}
      {(rows || !error) && (
        <TableFrame stackOnMobile className="sm:[&_table]:min-w-[720px] [&_table]:table-fixed">
          <colgroup><col /><col className="w-[140px]" /><col className="w-[110px]" /><col className="w-[130px]" /><col className="w-[170px]" /><col className="w-[110px]" /></colgroup><thead><tr><Th>Quiz</Th><Th>{msg("quizpractice.pages.QuizPracticeHistoryPage.lop")}</Th><Th>{msg("quizpractice.pages.QuizPracticeHistoryPage.soLanLam")}</Th><Th>{msg("quizpractice.pages.QuizPracticeHistoryPage.diemCaoNhat")}</Th><Th>{msg("quizpractice.pages.QuizPracticeHistoryPage.lanGanNhat")}</Th><Th></Th></tr></thead>
          <tbody>
            {!rows && <TableSkeletonRows columns={6} label={msg("quizpractice.pages.QuizPracticeHistoryPage.dangTaiQuiz")} />}
            {rows && shown.length === 0 && <tr><Td colSpan={6} className="py-8 text-center text-muted">{msg("quizpractice.pages.QuizPracticeHistoryPage.chuaCoQuizLuyenTap")}</Td></tr>}
            {shown.map((row) => (
              <tr key={`${row.quizId}/${row.classId}`} data-testid="practice-history-row">
                <Td>
                  <span className="font-semibold">{row.title}</span>
                  <span className="ml-2 inline-flex gap-1">
                    <Pill tone="info">{msg("quizpractice.pages.QuizPracticeHistoryPage.luyenTap")}</Pill>
                    {row.attemptsUsed === 0 && <Pill tone="warning">{msg("quizpractice.pages.QuizPracticeHistoryPage.chuaLam")}</Pill>}
                  </span>
                </Td>
                <Td label={msg("quizpractice.pages.QuizPracticeHistoryPage.lop")}>{row.classCode}</Td>
                <Td label={msg("quizpractice.pages.QuizPracticeHistoryPage.soLanLam")}>{formatAttempts(row.attemptsUsed, row.maxAttempts)}</Td>
                <Td label={msg("quizpractice.pages.QuizPracticeHistoryPage.diemCaoNhat")}>{row.bestScore === null ? "—" : `${formatPoints(row.bestScore)} / ${formatPoints(row.maxScore)}`}</Td>
                <Td label={msg("quizpractice.pages.QuizPracticeHistoryPage.lanGanNhat")} className="whitespace-nowrap text-muted">{formatDateTime(row.lastAttemptAt)}</Td>
                <Td className="whitespace-nowrap">
                  {row.attemptsUsed === 0 && row.canStart
                    ? <Button size="sm" variant="primary" disabled={busy} onClick={() => void start(row)} data-testid="practice-history-start">{msg("quizpractice.pages.QuizPracticeHistoryPage.batDau")}</Button>
                    : <Button size="sm" onClick={() => router.push(buildDetailHref(row))} data-testid="practice-history-view">Xem</Button>}
                </Td>
              </tr>
            ))}
          </tbody>
        </TableFrame>
      )}
    </>
  );
}
