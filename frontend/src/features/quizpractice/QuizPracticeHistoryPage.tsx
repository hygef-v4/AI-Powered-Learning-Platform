"use client";

import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { Alert, Button, PageHeader, Pill, Select, TableFrame, Td, Th } from "@/shared/ui";
import { getPracticeHistory, startAttempt } from "./api";
import { attemptsText, errorMessage, formatDateTime, formatPoints } from "./labels";
import type { PracticeQuizRow } from "./types";

const detailHref = (row: PracticeQuizRow) => `/quiz-practice/${row.quizId}?classId=${encodeURIComponent(row.classId)}`;

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
      .catch((failure: unknown) => active && setError(errorMessage(failure)));
    return () => {
      active = false;
    };
  }, []);

  const classes = useMemo(() => {
    const seen = new Map<string, string>();
    rows?.forEach((row) => seen.set(row.classId, `${row.classCode} · ${row.className}`));
    return [...seen.entries()];
  }, [rows]);
  const shown = rows?.filter((row) => !classId || row.classId === classId) ?? [];

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
    <>
      <PageHeader title="Luyện tập Quiz"
        actions={classes.length > 1 && (
          <Select aria-label="Lọc theo lớp" value={classId} onChange={(event) => setClassId(event.target.value)} data-testid="practice-history-class">
            <option value="">Mọi lớp</option>
            {classes.map(([id, label]) => <option key={id} value={id}>{label}</option>)}
          </Select>
        )} />
      {error && <Alert tone="danger" role="alert" className="mb-3">{error}</Alert>}
      {!rows && !error && <p role="status" className="py-8 text-center text-sm text-muted">Đang tải quiz…</p>}
      {rows && (
        <TableFrame className="[&_table]:min-w-[720px] [&_table]:table-fixed">
          <colgroup><col /><col className="w-[140px]" /><col className="w-[110px]" /><col className="w-[130px]" /><col className="w-[170px]" /><col className="w-[110px]" /></colgroup><thead><tr><Th>Quiz</Th><Th>Lớp</Th><Th>Số lần làm</Th><Th>Điểm cao nhất</Th><Th>Lần gần nhất</Th><Th></Th></tr></thead>
          <tbody>
            {shown.length === 0 && <tr><Td colSpan={6} className="py-8 text-center text-muted">Chưa có quiz luyện tập.</Td></tr>}
            {shown.map((row) => (
              <tr key={`${row.quizId}/${row.classId}`} data-testid="practice-history-row">
                <Td>
                  <span className="font-semibold">{row.title}</span>
                  <span className="ml-2 inline-flex gap-1">
                    <Pill tone="info">Practice</Pill>
                    {row.status !== "OPEN" && <Pill>Đã ngưng</Pill>}
                    {row.attemptsUsed === 0 && <Pill tone="warning">Chưa làm</Pill>}
                  </span>
                </Td>
                <Td>{row.classCode}</Td>
                <Td>{attemptsText(row.attemptsUsed, row.maxAttempts)}</Td>
                <Td>{row.bestScore === null ? "—" : `${formatPoints(row.bestScore)} / ${formatPoints(row.maxScore)}`}</Td>
                <Td className="whitespace-nowrap text-muted">{formatDateTime(row.lastAttemptAt)}</Td>
                <Td className="whitespace-nowrap">
                  {row.attemptsUsed === 0 && row.canStart
                    ? <Button size="sm" variant="primary" disabled={busy} onClick={() => void start(row)} data-testid="practice-history-start">Bắt đầu</Button>
                    : <Button size="sm" onClick={() => router.push(detailHref(row))} data-testid="practice-history-view">Xem</Button>}
                </Td>
              </tr>
            ))}
          </tbody>
        </TableFrame>
      )}
    </>
  );
}
