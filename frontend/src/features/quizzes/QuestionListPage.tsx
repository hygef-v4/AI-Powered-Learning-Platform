"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { Alert, Button, ClickableRow, cx, Menu, MenuItem, PageHeader, Pagination, Pill, Select, TableFrame, Td, TextInput, Th } from "@/shared/ui";
import { getManagedSubjects, getQuestions } from "./api";
import { DIFFICULTY_LABELS, DIFFICULTY_TONES, errorMessage, formatDateTime, STATUS_LABELS, STATUS_TONES, TYPE_LABELS } from "./labels";
import { QuestionAiDraftDialog } from "./AiDraftTargets";
import { QuestionImportDialog } from "./QuestionImportDialog";
import type { Difficulty, ManagedSubject, QuestionPage, QuestionStatus, QuestionType } from "./types";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";

const PAGE_SIZE = 20;
const TABS: { key: QuestionType | null; label: string }[] = [
  { key: null, label: "Tất cả" },
  { key: "MCQ_SINGLE", label: TYPE_LABELS.MCQ_SINGLE },
  { key: "MCQ_MULTI", label: TYPE_LABELS.MCQ_MULTI },
];

type Filters = { type: QuestionType | null; difficulty: Difficulty | ""; status: QuestionStatus | ""; q: string; page: number };
const NO_FILTERS: Filters = { type: null, difficulty: "", status: "", q: "", page: 0 };

/** Màn #19 Question List (UC 46): ngân hàng câu hỏi của môn Chủ nhiệm môn đang quản lý (BR-U10-01, 06). */
export function QuestionListPage() {
  const router = useRouter();
  const params = useSearchParams();
  const [subjects, setSubjects] = useState<ManagedSubject[] | null>(null);
  const [subjectsError, setSubjectsError] = useState<string | null>(null);
  const [filters, setFilters] = useState<Filters>(NO_FILTERS);
  const [search, setSearch] = useState("");
  const [data, setData] = useState<QuestionPage | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [importing, setImporting] = useState(false);
  const [drafting, setDrafting] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

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
    getQuestions(subject.id, {
      type: filters.type ?? undefined,
      difficulty: filters.difficulty || undefined,
      status: filters.status || undefined,
      q: filters.q || undefined,
      page: filters.page,
      size: PAGE_SIZE,
    })
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

  const selectSubject = (id: string) => {
    setFilters(NO_FILTERS);
    setSearch("");
    setData(null);
    router.replace(`/manager/questions?subjectId=${encodeURIComponent(id)}`);
  };

  if (subjectsError) return <PageLoadError>{subjectsError}</PageLoadError>;
  if (subjects === null) return <PageLoading label="Đang tải môn học…" />;
  if (!subject) return <Alert tone="info">Bạn chưa được giao quản lý môn nào.</Alert>;

  const archived = subject.status !== undefined && subject.status !== "ACTIVE";

  return (
    <>
      <PageHeader
        title="Câu hỏi"
        subtitle={data ? `${data.total} câu trong ngân hàng của môn` : undefined}
        actions={<div className="flex flex-wrap items-center gap-2">
          {subjects.length > 1 && (
            <Select aria-label="Chọn môn" value={subject.id} wrapperClassName="w-56" onChange={(event) => selectSubject(event.target.value)} data-testid="question-list-subject">
              {subjects.map((s) => <option key={s.id} value={s.id}>{s.code} · {s.name}</option>)}
            </Select>
          )}
          {!archived && (
            <Menu align="right" icon="add" label="Thêm câu hỏi" ariaLabel="Thêm câu hỏi"
              summaryClassName="!border-primary !bg-primary !text-on-primary hover:!bg-primary-strong">
              <MenuItem icon="edit" onClick={() => router.push(`/manager/questions/new?subjectId=${encodeURIComponent(subject.id)}`)}>Soạn câu mới</MenuItem>
              <MenuItem icon="upload_file" onClick={() => { setNotice(null); setImporting(true); }} data-testid="question-list-import">Nhập từ tệp</MenuItem>
              <MenuItem icon="sparkle" onClick={() => { setNotice(null); setDrafting(true); }} data-testid="question-list-ai">Soạn bằng AI</MenuItem>
            </Menu>
          )}
        </div>}
      />
      {archived && <Alert tone="warning">Môn đã lưu trữ, ngân hàng câu hỏi chỉ xem.</Alert>}
      {notice && <Alert tone="success" role="status">{notice}</Alert>}

      <div className="flex flex-wrap items-center gap-2">
        <div role="tablist" aria-label="Loại câu" className="inline-flex gap-0.5 rounded-[10px] bg-sunken p-[3px]">
          {TABS.map((tab) => (
            <button key={tab.label} type="button" role="tab" aria-selected={filters.type === tab.key}
              onClick={() => setFilters((current) => ({ ...current, type: tab.key, page: 0 }))}
              data-testid={`question-list-tab-${tab.key ?? "ALL"}`}
              className={cx("rounded-[7px] px-3 py-1.5 text-[12.5px] font-semibold transition-[background-color,color] duration-200",
                filters.type === tab.key ? "bg-surface text-ink shadow-[0_1px_2px_rgb(0_0_0/0.06)]" : "text-muted hover:text-ink")}>
              {tab.label}
            </button>
          ))}
        </div>
        <Select aria-label="Độ khó" value={filters.difficulty} data-testid="question-list-difficulty"
          onChange={(event) => setFilters((current) => ({ ...current, difficulty: event.target.value as Difficulty | "", page: 0 }))}>
          <option value="">Mọi độ khó</option>
          {(Object.keys(DIFFICULTY_LABELS) as Difficulty[]).map((d) => <option key={d} value={d}>{DIFFICULTY_LABELS[d]}</option>)}
        </Select>
        <Select aria-label="Trạng thái" value={filters.status} data-testid="question-list-status"
          onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value as QuestionStatus | "", page: 0 }))}>
          <option value="">Mọi trạng thái</option>
          {(Object.keys(STATUS_LABELS) as QuestionStatus[]).map((s) => <option key={s} value={s}>{STATUS_LABELS[s]}</option>)}
        </Select>
        <TextInput icon="search" placeholder="Lọc theo nội dung hoặc thẻ" aria-label="Lọc theo nội dung hoặc thẻ" value={search}
          maxLength={200} onChange={(event) => setSearch(event.target.value)} wrapperClassName="min-w-60 flex-1" data-testid="question-list-search" />
      </div>

      {error && <Alert tone="danger" role="alert" action={<Button size="sm" onClick={() => setReloadKey((k) => k + 1)}>Thử lại</Button>}>{error}</Alert>}
      {!error && !data && <p role="status" className="py-8 text-center text-sm text-muted">Đang tải câu hỏi…</p>}
      {!error && data && (
        <div className="flex flex-col gap-3">
          <TableFrame>
            <thead><tr><Th>Câu hỏi</Th><Th>Loại</Th><Th>Độ khó</Th><Th>Trạng thái</Th><Th>Cập nhật</Th></tr></thead>
            <tbody>
              {data.items.length === 0 && <tr><Td colSpan={5} className="py-8 text-center text-muted">Chưa có câu hỏi phù hợp.</Td></tr>}
              {data.items.map((row) => (
                <ClickableRow key={row.id} onClick={() => router.push(`/manager/questions/${row.id}`)} data-testid="question-list-row">
                  <Td className="max-w-[28rem]">
                    <p className="line-clamp-2 whitespace-pre-wrap">{row.stem}</p>
                    {row.tags.length > 0 && <p className="mt-1 text-[11px] text-muted">{row.tags.join(" · ")}</p>}
                  </Td>
                  <Td>{TYPE_LABELS[row.type]}</Td>
                  <Td><Pill tone={DIFFICULTY_TONES[row.difficulty]}>{DIFFICULTY_LABELS[row.difficulty]}</Pill></Td>
                  <Td>
                    <span className="inline-flex flex-wrap items-center gap-1">
                      <Pill tone={STATUS_TONES[row.status]}>{STATUS_LABELS[row.status]}</Pill>
                      {row.hasDraft && <Pill tone="warning">Có bản nháp mới</Pill>}
                    </span>
                  </Td>
                  <Td className="font-mono text-xs whitespace-nowrap text-muted">{formatDateTime(row.updatedAt)}</Td>
                </ClickableRow>
              ))}
            </tbody>
          </TableFrame>
          <Pagination page={data.page} pageSize={data.size} total={data.total} itemLabel="câu hỏi" testId="question-list-pagination"
            onPageChange={(page) => setFilters((current) => ({ ...current, page }))} />
        </div>
      )}

      {drafting && (
        <QuestionAiDraftDialog subjectId={subject.id} onClose={() => setDrafting(false)}
          onAccepted={(count) => {
            setDrafting(false);
            setNotice(`Đã lưu ${count} câu ở trạng thái Nháp.`);
            setReloadKey((k) => k + 1);
          }} />
      )}
      {importing && (
        <QuestionImportDialog subjectId={subject.id} onClose={() => setImporting(false)}
          onImported={(created) => {
            setImporting(false);
            setNotice(`Đã nhập ${created} câu ở trạng thái Nháp.`);
            setReloadKey((k) => k + 1);
          }} />
      )}
    </>
  );
}
