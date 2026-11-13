"use client";

import { useUrlState } from "@/shared/navigation/useUrlState";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { Alert, Button, ClickableRow, Dialog, Field, Menu, MenuItem, PageHeader, Pagination, Pill, Segmented, Select, TableFrame, Td, TextInput, Th } from "@/shared/ui";
import { bulkQuestions, getDuplicateQuestions, getManagedSubjects, getQuestions } from "../api";
import { DIFFICULTY_LABELS, DIFFICULTY_TONES, errorMessage, formatDateTime, STATUS_LABELS, STATUS_TONES, TYPE_LABELS } from "../model/labels";
import { QuestionAiDraftDialog } from "../components/AiDraftTargets";
import { QuestionImportDialog } from "../components/QuestionImportDialog";
import type { BulkResult, Difficulty, DuplicateGroup, ManagedSubject, QuestionPage, QuestionRow, QuestionStatus, QuestionType } from "../model/types";
import { PageLoadError, PageLoading } from "@/shared/ui/PageStatus";

const PAGE_SIZE = 20;
const TABS: { key: QuestionType | null; label: string }[] = [
  { key: null, label: "Tất cả" },
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
      .catch((failure: unknown) => active && setError(errorMessage(failure)));
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
    if (failures.length > 0) setError(`${done} ${result.succeeded} câu; ${failures.length} câu chưa xử lý được: ${failures[0]?.message ?? ""}`);
    else setError(null);
    setNotice(result.succeeded > 0 ? `${done} ${result.succeeded} câu.` : null);
    setReloadKey((k) => k + 1);
  };
  const runBulk = async (action: "activate" | "approve" | "reject") => {
    if (!subject) return;
    setBusy(true);
    try {
      const items = selectedRows.map((row) => ({ id: row.id, version: row.version }));
      const result = await bulkQuestions(subject.id, action, items, action === "reject" ? reason.trim() : undefined);
      report(result, action === "activate" ? "Đã kích hoạt" : action === "approve" ? "Đã duyệt" : "Đã từ chối");
      setRejecting(false);
      setReason("");
    } catch (failure) {
      setError(errorMessage(failure));
    } finally {
      setBusy(false);
    }
  };
  const showDuplicates = async () => {
    if (!subject) return;
    try {
      setDuplicates(await getDuplicateQuestions(subject.id));
    } catch (failure) {
      setError(errorMessage(failure));
    }
  };

  if (subjectsError) return <PageLoadError>{subjectsError}</PageLoadError>;
  if (subjects === null) return <PageLoading label="Đang tải môn học…" />;
  if (!subject) return <Alert tone="info">Bạn chưa được giao quản lý môn nào.</Alert>;

  const archived = subject.status !== undefined && subject.status !== "ACTIVE";

  return (
    <>
      <PageHeader
        title="Ngân hàng câu hỏi"
        subtitle={data ? `${data.total} câu trong ngân hàng của môn` : undefined}
        actions={<div className="flex flex-wrap items-center gap-2">
          {subjects.length > 1 && (
            <Select aria-label="Chọn môn" value={subject.id} wrapperClassName="w-56" onChange={(event) => selectSubject(event.target.value)} data-testid="question-list-subject">
              {subjects.map((s) => <option key={s.id} value={s.id}>{s.code} · {s.name}</option>)}
            </Select>
          )}
          <Button icon="copy" onClick={() => void showDuplicates()} data-testid="question-list-duplicates">Câu trùng</Button>
          {!archived && (
            <Menu align="right" icon="add" label="Thêm câu hỏi" ariaLabel="Thêm câu hỏi"
              summaryClassName="!border-primary !bg-primary !text-on-primary hover:!bg-primary-strong">
              <MenuItem icon="edit" onClick={() => router.push(`/manager/questions/new?subjectId=${encodeURIComponent(subject.id)}`)}>Soạn câu mới</MenuItem>
              <MenuItem icon="upload_file" onClick={() => { setNotice(null); setImporting(true); }} data-testid="question-list-import">Nhập Excel</MenuItem>
              <MenuItem icon="sparkle" onClick={() => { setNotice(null); setDrafting(true); }} data-testid="question-list-ai">Soạn bằng AI</MenuItem>
            </Menu>
          )}
        </div>}
      />
      {archived && <Alert tone="warning">Môn đã lưu trữ, ngân hàng câu hỏi chỉ xem.</Alert>}
      {notice && <Alert tone="success" role="status">{notice}</Alert>}

      <div className="flex flex-wrap items-center gap-2">
        <Segmented<QuestionType | "">
          as="tab"
          ariaLabel="Loại câu"
          value={type}
          onChange={(value) => { setType(value); setFilters((current) => ({ ...current, page: 0 })); }}
          options={TABS.map((tab) => ({ value: tab.key ?? "", label: tab.label, testId: `question-list-tab-${tab.key ?? "ALL"}` }))}
        />
        <Select aria-label="Độ khó" value={filters.difficulty} data-testid="question-list-difficulty"
          onChange={(event) => setFilters((current) => ({ ...current, difficulty: event.target.value as Difficulty | "", page: 0 }))}>
          <option value="">Mọi độ khó</option>
          {(Object.keys(DIFFICULTY_LABELS) as Difficulty[]).map((d) => <option key={d} value={d}>{DIFFICULTY_LABELS[d]}</option>)}
        </Select>
        {!archived && bulkAction && selected.length > 0 && (
          <div className="flex items-center gap-2" data-testid="question-list-bulk">
            <span className="text-xs text-muted">Đã chọn {selected.length} câu</span>
            {bulkAction === "activate" && <Button size="sm" variant="primary" disabled={busy} onClick={() => void runBulk("activate")} data-testid="question-list-bulk-activate">Kích hoạt</Button>}
            {bulkAction === "review" && <>
              <Button size="sm" variant="primary" disabled={busy} onClick={() => void runBulk("approve")} data-testid="question-list-bulk-approve">Duyệt</Button>
              <Button size="sm" disabled={busy} onClick={() => setRejecting(true)} data-testid="question-list-bulk-reject">Từ chối</Button>
            </>}
          </div>
        )}
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
          <TableFrame className="[&_table]:min-w-[720px] [&_table]:table-fixed">
            <colgroup>{bulkAction && <col className="w-[44px]" />}<col /><col className="w-[130px]" /><col className="w-[100px]" /><col className="w-[130px]" /><col className="w-[150px]" /></colgroup><thead><tr>{bulkAction && <Th><span className="sr-only">Chọn</span></Th>}<Th>Câu hỏi</Th><Th>Loại</Th><Th>Độ khó</Th><Th>Trạng thái</Th><Th>Cập nhật</Th></tr></thead>
            <tbody>
              {data.items.length === 0 && <tr><Td colSpan={bulkAction ? 6 : 5} className="py-8 text-center text-muted">Chưa có câu hỏi phù hợp.</Td></tr>}
              {data.items.map((row) => (
                <ClickableRow key={row.id} onClick={() => router.push(`/manager/questions/${row.id}`)} data-testid="question-list-row">
                  {bulkAction && (
                    <Td onClick={(event) => event.stopPropagation()}>
                      <input type="checkbox" aria-label={`Chọn câu ${row.stem}`} checked={selected.includes(row.id)} onChange={() => toggleRow(row.id)} data-testid="question-list-select" />
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

      {rejecting && (
        <Dialog open onClose={() => setRejecting(false)} tone="warning" title={`Từ chối ${selected.length} câu đề xuất?`}
          footer={<Button variant="primary" disabled={busy || !reason.trim()} onClick={() => void runBulk("reject")} data-testid="question-list-reject-confirm">Từ chối</Button>}>
          <Field label="Lý do từ chối" required htmlFor="question-list-reason" hint="Giảng viên thấy lý do này để sửa rồi gửi lại.">
            <textarea id="question-list-reason" rows={3} maxLength={1000} value={reason} onChange={(e) => setReason(e.target.value)}
              className="w-full rounded-lg border border-line-strong px-3 py-2 text-[13px] text-ink focus-visible:outline-focus" data-testid="question-list-reason" />
          </Field>
        </Dialog>
      )}
      {duplicates && (
        <Dialog open size="lg" onClose={() => setDuplicates(null)} title="Câu giống hệt nhau trong ngân hàng">
          <div className="flex flex-col gap-3 text-[13px]" data-testid="question-duplicates">
            {duplicates.length === 0 && <p className="text-muted">Không có câu trùng.</p>}
            {duplicates.map((group, index) => (
              <section key={index} className="rounded-lg border border-line p-3">
                <p className="mb-1 text-xs text-muted">Nhóm {index + 1}: hệ thống không tự xóa, hãy mở câu để ngưng hoặc xóa bớt.</p>
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
