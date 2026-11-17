"use client";

import { useUrlState } from "@/shared/navigation/useUrlState";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { Alert, Button, ClickableRow, Dialog, Field, Menu, MenuItem, PageHeader, Pagination, Pill, Segmented, Select, TableFrame, Td, Textarea, TextInput, Th } from "@/shared/ui";
import { bulkQuestions, getDuplicateQuestions, getManagedSubjects, getQuestions } from "../api";
import { DIFFICULTY_LABELS, DIFFICULTY_TONES, describeError, formatDateTime, STATUS_LABELS, STATUS_TONES, TYPE_LABELS } from "../model/labels";
import { QuestionAiDraftDialog } from "../components/AiDraftTargets";
import { QuestionImportDialog } from "../components/QuestionImportDialog";
import type { BulkResult, Difficulty, DuplicateGroup, ManagedSubject, QuestionPage, QuestionRow, QuestionStatus, QuestionType } from "../model/types";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";
import { msg } from "@/i18n/msg";

const PAGE_SIZE = 20;
const TABS: { key: QuestionType | null; label: string }[] = [
  { key: null, label: msg("quizzes.pages.QuestionListPage.all") },
  { key: "MCQ_SINGLE", label: TYPE_LABELS.MCQ_SINGLE },
  { key: "MCQ_MULTI", label: TYPE_LABELS.MCQ_MULTI },
];

type Filters = { difficulty: Difficulty | ""; status: QuestionStatus | ""; q: string; page: number };
const NO_FILTERS: Filters = { difficulty: "", status: "", q: "", page: 0 };
const STATUSES = Object.keys(STATUS_LABELS) as QuestionStatus[];

/**
 * Màn #19 Question List (UC 46): ngân hàng câu hỏi của môn Chủ nhiệm môn đang quản lý (BR-U10-01, 06). Lọc "Chờ duyệt" để
 * duyệt hoặc từ chối hàng loạt câu giảng viên đề xuất (BR-U10-09), lọc "Nháp" để kích hoạt hàng loạt, và xem các nhóm câu
 * giống hệt nhau (BR-U10-08).
 */
export function QuestionListPage() {
  const router = useRouter();
  const params = useSearchParams();
  const [subjects, setSubjects] = useState<ManagedSubject[] | null>(null);
  const [subjectsError, setSubjectsError] = useState<string | null>(null);
  const [type, setType] = useUrlState<QuestionType | "">("type", "", ["", "MCQ_SINGLE", "MCQ_MULTI"]);
  const initialStatus = params?.get("status");
  const [filters, setFilters] = useState<Filters>(() => ({ ...NO_FILTERS,
    status: STATUSES.includes(initialStatus as QuestionStatus) ? initialStatus as QuestionStatus : "" }));
  const [search, setSearch] = useState("");
  const [data, setData] = useState<QuestionPage | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [importing, setImporting] = useState(false);
  const [drafting, setDrafting] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [selected, setSelected] = useState<string[]>([]);
  const [rejecting, setRejecting] = useState(false);
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);
  const [duplicates, setDuplicates] = useState<DuplicateGroup[] | null>(null);

  useEffect(() => {
    let active = true;
    getManagedSubjects()
      .then((list) => active && setSubjects(list))
      .catch((failure: unknown) => active && setSubjectsError(describeError(failure)));
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
      type: type || undefined,
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
        setSelected([]);
      })
      .catch((failure: unknown) => active && setError(describeError(failure)));
    return () => {
      active = false;
    };
  }, [subject, filters, type, reloadKey]);

  const selectSubject = (id: string) => {
    setFilters(NO_FILTERS);
    setSearch("");
    setData(null);
    router.replace(`/manager/questions?subjectId=${encodeURIComponent(id)}`);
  };

  const bulkAction = filters.status === "PENDING" ? "review" : filters.status === "DRAFT" ? "activate" : null;
  const selectedRows = (data?.items ?? []).filter((row: QuestionRow) => selected.includes(row.id));
  const toggleRow = (id: string) => setSelected((list) => (list.includes(id) ? list.filter((x) => x !== id) : [...list, id]));
  const report = (result: BulkResult, done: string) => {
    const failures = result.items.filter((item) => !item.ok);
    if (failures.length > 0) setError(msg("quizzes.pages.QuestionListPage.questionsQuestionsCouldNotBe", { done: String(done), succeeded: String(result.succeeded), length: String(failures.length), value: String(failures[0]?.message ?? "") }));
    else setError(null);
    setNotice(result.succeeded > 0 ? msg("quizzes.pages.QuestionListPage.questions", { done: String(done), succeeded: String(result.succeeded) }) : null);
    setReloadKey((k) => k + 1);
  };
  const runBulk = async (action: "activate" | "approve" | "reject") => {
    if (!subject) return;
    setBusy(true);
    try {
      const items = selectedRows.map((row) => ({ id: row.id, version: row.version }));
      const result = await bulkQuestions(subject.id, action, items, action === "reject" ? reason.trim() : undefined);
      report(result, action === "activate" ? msg("quizzes.pages.QuestionListPage.activated") : action === "approve" ? msg("quizzes.pages.QuestionListPage.approved") : msg("quizzes.pages.QuestionListPage.rejected"));
      setRejecting(false);
      setReason("");
    } catch (failure) {
      setError(describeError(failure));
    } finally {
      setBusy(false);
    }
  };
  const showDuplicates = async () => {
    if (!subject) return;
    try {
      setDuplicates(await getDuplicateQuestions(subject.id));
    } catch (failure) {
      setError(describeError(failure));
    }
  };

  if (subjectsError) return <PageLoadError>{subjectsError}</PageLoadError>;
  if (subjects === null) return <PageLoading label={msg("quizzes.pages.QuestionListPage.loadingSubjects")} />;
  if (!subject) return <Alert tone="info">{msg("quizzes.pages.QuestionListPage.youHaveNotBeenAssigned")}</Alert>;

  const archived = subject.status !== undefined && subject.status !== "ACTIVE";

  return (
    <>
      <PageHeader
        title={msg("quizzes.pages.QuestionListPage.questionBank")}
        subtitle={data ? msg("quizzes.pages.QuestionListPage.questionsInTheSubjectBank", { total: String(data.total) }) : undefined}
        actions={<div className="flex flex-wrap items-center gap-2">
          {subjects.length > 1 && (
            <Select aria-label={msg("quizzes.pages.QuestionListPage.chooseSubject")} value={subject.id} wrapperClassName="w-56" onChange={(event) => selectSubject(event.target.value)} data-testid="question-list-subject">
              {subjects.map((s) => <option key={s.id} value={s.id}>{s.code} · {s.name}</option>)}
            </Select>
          )}
          <Button icon="copy" onClick={() => void showDuplicates()} data-testid="question-list-duplicates">{msg("quizzes.pages.QuestionListPage.duplicates")}</Button>
          {!archived && (
            <Menu align="right" icon="add" label={msg("quizzes.pages.QuestionListPage.addQuestion")} ariaLabel={msg("quizzes.pages.QuestionListPage.addQuestion")}
              summaryClassName="!border-primary !bg-primary !text-on-primary hover:!bg-primary-strong">
              <MenuItem icon="edit" onClick={() => router.push(`/manager/questions/new?subjectId=${encodeURIComponent(subject.id)}`)}>{msg("quizzes.pages.QuestionListPage.writeANewQuestion")}</MenuItem>
              <MenuItem icon="upload_file" onClick={() => { setNotice(null); setImporting(true); }} data-testid="question-list-import">{msg("quizzes.pages.QuestionListPage.importExcel")}</MenuItem>
              <MenuItem icon="sparkle" onClick={() => { setNotice(null); setDrafting(true); }} data-testid="question-list-ai">{msg("quizzes.pages.QuestionListPage.writeWithAi")}</MenuItem>
            </Menu>
          )}
        </div>}
      />
      {archived && <Alert tone="warning">{msg("quizzes.pages.QuestionListPage.theSubjectIsArchivedThe")}</Alert>}
      {notice && <Alert tone="success" role="status">{notice}</Alert>}

      <div className="flex flex-wrap items-center gap-2">
        <Segmented<QuestionType | "">
          as="tab"
          ariaLabel={msg("quizzes.pages.QuestionListPage.questionType")}
          value={type}
          onChange={(value) => { setType(value); setFilters((current) => ({ ...current, page: 0 })); }}
          options={TABS.map((tab) => ({ value: tab.key ?? "", label: tab.label, testId: `question-list-tab-${tab.key ?? "ALL"}` }))}
        />
        <Select aria-label={msg("quizzes.pages.QuestionListPage.difficulty")} value={filters.difficulty} data-testid="question-list-difficulty"
          onChange={(event) => setFilters((current) => ({ ...current, difficulty: event.target.value as Difficulty | "", page: 0 }))}>
          <option value="">{msg("quizzes.pages.QuestionListPage.allDifficulties")}</option>
          {(Object.keys(DIFFICULTY_LABELS) as Difficulty[]).map((d) => <option key={d} value={d}>{DIFFICULTY_LABELS[d]}</option>)}
        </Select>
        {!archived && bulkAction && selected.length > 0 && (
          <div className="flex items-center gap-2" data-testid="question-list-bulk">
            <span className="text-xs text-muted">{msg("quizzes.pages.QuestionListPage.selected")}{selected.length} {msg("quizzes.pages.QuestionListPage.questions2")}</span>
            {bulkAction === "activate" && <Button size="sm" variant="primary" disabled={busy} onClick={() => void runBulk("activate")} data-testid="question-list-bulk-activate">{msg("quizzes.pages.QuestionListPage.activate")}</Button>}
            {bulkAction === "review" && <>
              <Button size="sm" variant="primary" disabled={busy} onClick={() => void runBulk("approve")} data-testid="question-list-bulk-approve">{msg("quizzes.pages.QuestionListPage.approve")}</Button>
              <Button size="sm" disabled={busy} onClick={() => setRejecting(true)} data-testid="question-list-bulk-reject">{msg("quizzes.pages.QuestionListPage.reject")}</Button>
            </>}
          </div>
        )}
        <Select aria-label={msg("quizzes.pages.QuestionListPage.status")} value={filters.status} data-testid="question-list-status"
          onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value as QuestionStatus | "", page: 0 }))}>
          <option value="">{msg("quizzes.pages.QuestionListPage.allStatuses")}</option>
          {(Object.keys(STATUS_LABELS) as QuestionStatus[]).map((s) => <option key={s} value={s}>{STATUS_LABELS[s]}</option>)}
        </Select>
        <TextInput icon="search" placeholder={msg("quizzes.pages.QuestionListPage.filterByContentOrTag")} aria-label={msg("quizzes.pages.QuestionListPage.filterByContentOrTag")} value={search}
          maxLength={200} onChange={(event) => setSearch(event.target.value)} wrapperClassName="min-w-60 flex-1" data-testid="question-list-search" />
      </div>

      {error && <Alert tone="danger" role="alert" action={<Button size="sm" onClick={() => setReloadKey((k) => k + 1)}>{msg("quizzes.pages.QuestionListPage.retry")}</Button>}>{error}</Alert>}
      {!error && !data && <p role="status" className="py-8 text-center text-sm text-muted">{msg("quizzes.pages.QuestionListPage.loadingQuestions")}</p>}
      {!error && data && (
        <div className="flex flex-col gap-3">
          <TableFrame className="[&_table]:min-w-[720px] [&_table]:table-fixed">
            <colgroup>{bulkAction && <col className="w-[44px]" />}<col /><col className="w-[130px]" /><col className="w-[100px]" /><col className="w-[130px]" /><col className="w-[150px]" /></colgroup><thead><tr>{bulkAction && <Th><span className="sr-only">{msg("quizzes.pages.QuestionListPage.select")}</span></Th>}<Th>{msg("quizzes.pages.QuestionListPage.question")}</Th><Th>{msg("quizzes.pages.QuestionListPage.type")}</Th><Th>{msg("quizzes.pages.QuestionListPage.difficulty")}</Th><Th>{msg("quizzes.pages.QuestionListPage.status")}</Th><Th>{msg("quizzes.pages.QuestionListPage.updated")}</Th></tr></thead>
            <tbody>
              {data.items.length === 0 && <tr><Td colSpan={bulkAction ? 6 : 5} className="py-8 text-center text-muted">{msg("quizzes.pages.QuestionListPage.noMatchingQuestionsYet")}</Td></tr>}
              {data.items.map((row) => (
                <ClickableRow key={row.id} onClick={() => router.push(`/manager/questions/${row.id}`)} data-testid="question-list-row">
                  {bulkAction && (
                    <Td onClick={(event) => event.stopPropagation()}>
                      <input type="checkbox" aria-label={msg("quizzes.pages.QuestionListPage.selectQuestion", { stem: String(row.stem) })} checked={selected.includes(row.id)} onChange={() => toggleRow(row.id)} data-testid="question-list-select" />
                    </Td>
                  )}
                  <Td className="max-w-[28rem]">
                    <p className="line-clamp-2 whitespace-pre-wrap">{row.stem}</p>
                    {row.tags.length > 0 && <p className="mt-1 text-xs text-muted">{row.tags.join(" · ")}</p>}
                  </Td>
                  <Td>{TYPE_LABELS[row.type]}</Td>
                  <Td><Pill tone={DIFFICULTY_TONES[row.difficulty]}>{DIFFICULTY_LABELS[row.difficulty]}</Pill></Td>
                  <Td>
                    <span className="inline-flex flex-wrap items-center gap-1">
                      <Pill tone={STATUS_TONES[row.status]}>{STATUS_LABELS[row.status]}</Pill>
                      {row.hasDraft && <Pill tone="warning">{msg("quizzes.pages.QuestionListPage.newDraftAvailable")}</Pill>}
                    </span>
                  </Td>
                  <Td className="font-mono text-xs whitespace-nowrap text-muted">{formatDateTime(row.updatedAt)}</Td>
                </ClickableRow>
              ))}
            </tbody>
          </TableFrame>
          <Pagination page={data.page} pageSize={data.size} total={data.total} itemLabel={msg("quizzes.pages.QuestionListPage.questions3")} testId="question-list-pagination"
            onPageChange={(page) => setFilters((current) => ({ ...current, page }))} />
        </div>
      )}

      {rejecting && (
        <Dialog open onClose={() => setRejecting(false)} tone="warning" title={msg("quizzes.pages.QuestionListPage.rejectProposedQuestions", { length: String(selected.length) })}
          footer={<Button variant="primary" disabled={busy || !reason.trim()} onClick={() => void runBulk("reject")} data-testid="question-list-reject-confirm">{msg("quizzes.pages.QuestionListPage.reject")}</Button>}>
          <Field label={msg("quizzes.pages.QuestionListPage.rejectionReason")} required htmlFor="question-list-reason" hint={msg("quizzes.pages.QuestionListPage.theLecturerSeesThisReason")}>
            <Textarea id="question-list-reason" rows={3} maxLength={1000} value={reason} onChange={(e) => setReason(e.target.value)} data-testid="question-list-reason" />
          </Field>
        </Dialog>
      )}
      {duplicates && (
        <Dialog open size="lg" onClose={() => setDuplicates(null)} title={msg("quizzes.pages.QuestionListPage.identicalQuestionsInTheBank")}>
          <div className="flex flex-col gap-3 text-[13px]" data-testid="question-duplicates">
            {duplicates.length === 0 && <p className="text-muted">{msg("quizzes.pages.QuestionListPage.noDuplicates")}</p>}
            {duplicates.map((group, index) => (
              <section key={index} className="rounded-lg border border-line p-3">
                <p className="mb-1 text-xs text-muted">{msg("quizzes.pages.QuestionListPage.group")}{index + 1}{msg("quizzes.pages.QuestionListPage.theSystemDoesNotDelete")}</p>
                <ul className="flex flex-col gap-1">
                  {group.questions.map((q) => (
                    <li key={q.id} className="flex items-center gap-2">
                      <Pill tone={STATUS_TONES[q.status]}>{STATUS_LABELS[q.status]}</Pill>
                      <button type="button" className="line-clamp-1 text-left text-primary-ink underline-offset-2 hover:underline" onClick={() => router.push(`/manager/questions/${q.id}`)}>{q.stem}</button>
                    </li>
                  ))}
                </ul>
              </section>
            ))}
          </div>
        </Dialog>
      )}
      {drafting && (
        <QuestionAiDraftDialog subjectId={subject.id} onClose={() => setDrafting(false)}
          onAccepted={(count) => {
            setDrafting(false);
            setNotice(msg("quizzes.pages.QuestionListPage.savedQuestionsAsDraft", { count: String(count) }));
            setReloadKey((k) => k + 1);
          }} />
      )}
      {importing && (
        <QuestionImportDialog subjectId={subject.id} onClose={() => setImporting(false)}
          onImported={(created) => {
            setImporting(false);
            setNotice(msg("quizzes.pages.QuestionListPage.importedQuestionsAsDraft", { created: String(created) }));
            setReloadKey((k) => k + 1);
          }} />
      )}
    </>
  );
}
