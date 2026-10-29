"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { Alert, Button, ClickableRow, cx, PageHeader, Pagination, Pill, Select, TableFrame, Td, TextInput, Th } from "@/shared/ui";
import { createSubjectQuiz, getManagedSubjects, getSubjectQuizzes } from "./api";
import { CreateQuizDialog } from "./CreateQuizDialog";
import { errorMessage, formatDateTime, formatPoints, QUIZ_STATUS_LABELS, QUIZ_STATUS_TONES } from "./labels";
import type { ManagedSubject, QuizPage, QuizStatus } from "./types";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";

const PAGE_SIZE = 20;
const TABS: { key: QuizStatus | null; label: string }[] = [
  { key: null, label: "Tất cả" },
  { key: "DRAFT", label: QUIZ_STATUS_LABELS.DRAFT },
  { key: "REVIEWED", label: QUIZ_STATUS_LABELS.REVIEWED },
  { key: "OPEN", label: QUIZ_STATUS_LABELS.OPEN },
  { key: "RETIRED", label: QUIZ_STATUS_LABELS.RETIRED },
];

/** Màn #21 Quiz List (UC 48, F5): Quiz của môn Chủ nhiệm môn đang quản lý. */
export function SubjectQuizListPage() {
  const router = useRouter();
  const params = useSearchParams();
  const [subjects, setSubjects] = useState<ManagedSubject[] | null>(null);
  const [subjectsError, setSubjectsError] = useState<string | null>(null);
  const [filters, setFilters] = useState<{ status: QuizStatus | null; q: string; page: number }>({ status: null, q: "", page: 0 });
  const [search, setSearch] = useState("");
  const [data, setData] = useState<QuizPage | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    let active = true;
    getManagedSubjects()
      .then((list) => active && setSubjects(list))
      .catch((failure: unknown) => active && setSubjectsError(errorMessage(failure)));
    return () => {
      active = false;
    };
  }, []);

  const requested = params?.get("subjectId");
  const subject = subjects?.find((s) => s.id === requested) ?? subjects?.[0] ?? null;

  useEffect(() => {
    const timer = setTimeout(() => setFilters((current) => (current.q === search.trim() ? current : { ...current, q: search.trim(), page: 0 })), 300);
    return () => clearTimeout(timer);
  }, [search]);

  useEffect(() => {
    if (!subject) return;
    let active = true;
    getSubjectQuizzes(subject.id, { status: filters.status ?? undefined, q: filters.q, page: filters.page, size: PAGE_SIZE })
      .then((page) => {
        if (!active) return;
        setError(null);
        setData(page);
      })
      .catch((failure: unknown) => active && setError(errorMessage(failure)));
    return () => {
      active = false;
    };
  }, [subject, filters, reloadKey]);

  if (subjectsError) return <PageLoadError>{subjectsError}</PageLoadError>;
  if (subjects === null) return <PageLoading label="Đang tải môn học…" />;
  if (!subject) return <Alert tone="info">Bạn chưa được giao quản lý môn nào.</Alert>;
  const archived = subject.status !== undefined && subject.status !== "ACTIVE";

  return (
    <>
      <PageHeader
        title="Quiz"
        breadcrumb={[{ label: "Quiz của môn" }, { label: subject.code }]}
        actions={<div className="flex flex-wrap items-center gap-2">
          <Select aria-label="Chọn môn" value={subject.id} data-testid="quiz-list-subject"
            onChange={(event) => {
              setData(null);
              setFilters({ status: null, q: "", page: 0 });
              setSearch("");
              router.replace(`/manager/quizzes?subjectId=${encodeURIComponent(event.target.value)}`);
            }}>
            {subjects.map((s) => <option key={s.id} value={s.id}>{s.code} · {s.name}</option>)}
          </Select>
          {!archived && <Button variant="primary" icon="add" onClick={() => setCreating(true)} data-testid="quiz-list-create">Tạo quiz</Button>}
        </div>}
      />
      {archived && <Alert tone="warning" className="mb-3">Môn đã lưu trữ, quiz của môn chỉ xem.</Alert>}
      <div className="mb-3 flex flex-wrap items-center gap-2">
        <div role="tablist" aria-label="Trạng thái quiz" className="flex gap-1">
          {TABS.map((tab) => (
            <button key={tab.label} type="button" role="tab" aria-selected={filters.status === tab.key}
              onClick={() => setFilters((current) => ({ ...current, status: tab.key, page: 0 }))}
              data-testid={`quiz-list-tab-${tab.key ?? "ALL"}`}
              className={cx("rounded-lg px-3 py-1.5 text-[13px] font-semibold",
                filters.status === tab.key ? "bg-primary-soft text-primary-ink" : "text-body hover:bg-shell")}>
              {tab.label}
            </button>
          ))}
        </div>
        <TextInput icon="search" placeholder="Tìm theo tên quiz" aria-label="Tìm theo tên quiz" value={search} maxLength={200}
          onChange={(event) => setSearch(event.target.value)} wrapperClassName="min-w-60 flex-1" data-testid="quiz-list-search" />
      </div>
      {error && <Alert tone="danger" role="alert" action={<Button size="sm" onClick={() => setReloadKey((k) => k + 1)}>Thử lại</Button>}>{error}</Alert>}
      {!error && !data && <p role="status" className="py-8 text-center text-sm text-muted">Đang tải quiz…</p>}
      {!error && data && (
        <div className="flex flex-col gap-3">
          <TableFrame className="[&_table]:min-w-[680px] [&_table]:table-fixed">
            <colgroup><col /><col className="w-[90px]" /><col className="w-[110px]" /><col className="w-[130px]" /><col className="w-[150px]" /></colgroup><thead><tr><Th>Quiz</Th><Th>Số câu</Th><Th>Thời gian</Th><Th>Trạng thái</Th><Th>Cập nhật</Th></tr></thead>
            <tbody>
              {data.items.length === 0 && <tr><Td colSpan={5} className="py-8 text-center text-muted">Chưa có quiz phù hợp.</Td></tr>}
              {data.items.map((row) => (
                <ClickableRow key={row.id} onClick={() => router.push(`/quizzes/${row.id}`)} data-testid="quiz-list-row">
                  <Td><span className="font-semibold">{row.title}</span></Td>
                  <Td>{row.itemCount} câu · {formatPoints(row.maxScore)} điểm</Td>
                  <Td>{row.timeLimitMinutes ? `${row.timeLimitMinutes} phút` : "Không giới hạn"}</Td>
                  <Td><Pill tone={QUIZ_STATUS_TONES[row.status]}>{QUIZ_STATUS_LABELS[row.status]}</Pill></Td>
                  <Td className="whitespace-nowrap text-muted">{formatDateTime(row.updatedAt)}</Td>
                </ClickableRow>
              ))}
            </tbody>
          </TableFrame>
          <Pagination page={data.page} pageSize={data.size} total={data.total} itemLabel="quiz" testId="quiz-list-pagination"
            onPageChange={(page) => setFilters((current) => ({ ...current, page }))} />
        </div>
      )}
      {creating && (
        <CreateQuizDialog onClose={() => setCreating(false)} onCreate={(title) => createSubjectQuiz(subject.id, title)}
          onCreated={(quiz) => router.push(`/quizzes/${quiz.quiz.id}`)} />
      )}
    </>
  );
}
