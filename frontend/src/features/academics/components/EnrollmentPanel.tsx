"use client";

import { useCandidates, type CandidateLoader } from "../hooks/useCandidates";
import { useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Button, ClickableRow, Dialog, Icon, IconButton, PageNav, Pill, Section, Segmented, Select, TableFrame, Td, TextInput, Th, showToast } from "@/shared/ui";
import { formatDate, formatOperationError } from "../model/format";
import { parseEnrollmentList, type EnrollmentPreviewRow } from "../model/enrollmentList";
import { readEnrollmentWorkbook } from "../model/enrollmentWorkbook";
import { ENROLLMENT_RESULT_LABELS, type EnrollmentRowResult, type EnrollmentView, type PersonView } from "../model/types";

const PAGE_SIZE = 20;
const PREVIEW_SIZE = 10;
const STATUS_TABS = [{ value: "ACTIVE", label: "Đang học" }, { value: "REMOVED", label: "Đã gỡ" }] as const;
const OK_RESULTS = new Set(["ENROLLED", "RESTORED"]);
const NO_GROUP = "__none";

/** Kết quả từng dòng sau khi thêm theo danh sách, cùng kiểu bảng kiểm tra Excel ở trang Tài khoản. */
export function EnrollmentResultTable({ rows }: { rows: EnrollmentRowResult[] }) {
  return <TableFrame filledHead className="[&_table]:table-fixed">
    <caption className="sr-only">Kết quả từng dòng</caption>
    <colgroup><col className="w-[72px]" /><col /><col className="w-[200px]" /></colgroup><thead><tr><Th className="w-16">Dòng</Th><Th>Email</Th><Th>Kết quả</Th></tr></thead>
    <tbody>{rows.map((row) => <tr key={row.line}>
      <Td className="text-muted">{row.line}</Td>
      <Td>{row.email}</Td>
      <Td><Pill tone={OK_RESULTS.has(row.result) ? "success" : "danger"}>{ENROLLMENT_RESULT_LABELS[row.result]}</Pill></Td>
    </tr>)}</tbody>
  </TableFrame>;
}

/**
 * Sinh viên trong lớp, cùng kiểu trang Tài khoản: Thêm theo danh sách và Thêm sinh viên ở trên; thẻ danh sách có tab
 * trạng thái bên trái, ô tìm bên phải, bảng tiêu đề tô nền. Trang giảng viên chỉ xem (không tab, không thao tác).
 */
export function EnrollmentPanel({ students, groupNames, groupLeaders, editable = false, includeRemoved = false, candidates = [], onStatusChange, onEnroll, onBatch, onView, loadCandidates, actionsHost }: {
  loadCandidates?: CandidateLoader; students: EnrollmentView[]; groupNames?: Record<string, string> | null; groupLeaders?: string[]; editable?: boolean; includeRemoved?: boolean; candidates?: PersonView[];
  /** Chỗ đặt nút Thêm theo danh sách / Thêm sinh viên (hàng tab của trang lớp); không có thì nút nằm trên hàng công cụ của thẻ. */
  actionsHost?: HTMLElement | null;
  onStatusChange?: (accountId: string, status: "ACTIVE" | "REMOVED") => Promise<void>;
  onEnroll?: (accountId: string) => Promise<void>; onBatch?: (text: string) => Promise<EnrollmentRowResult[]>; onView?: (student: EnrollmentView) => void;
}) {
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<"ACTIVE" | "REMOVED">("ACTIVE");
  // Lọc theo nhóm (trang giảng viên): "" là tất cả, NO_GROUP là chưa có nhóm, còn lại là tên nhóm.
  const [group, setGroup] = useState("");
  const [removing, setRemoving] = useState<EnrollmentView | null>(null);
  const [adding, setAdding] = useState(false);
  const [importing, setImporting] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const showStatus = editable || includeRemoved;
  const actions = editable && onStatusChange;
  const q = query.toLocaleLowerCase("vi").trim();
  const groupOf = (student: EnrollmentView) => groupNames?.[student.accountId] ?? NO_GROUP;
  const groupOptions = groupNames ? [...new Set(Object.values(groupNames))].sort((a, b) => a.localeCompare(b, "vi", { numeric: true })) : [];
  const visible = students.filter((student) => student.status === (showStatus ? status : "ACTIVE") && `${student.name} ${student.email}`.toLocaleLowerCase("vi").includes(q)
    && (!group || groupOf(student) === group));
  const totalPages = Math.max(1, Math.ceil(visible.length / PAGE_SIZE));
  const current = Math.min(page, totalPages - 1);
  const rows = visible.slice(current * PAGE_SIZE, (current + 1) * PAGE_SIZE);
  // Trang giảng viên (chỉ xem) không có cột STT.
  const numbered = editable || includeRemoved;
  const columns = 3 + (numbered ? 1 : 0) + (groupNames !== undefined ? 1 : 0) + (actions ? 1 : 0);
  async function handleEnrollmentChange(student: EnrollmentView, next: "ACTIVE" | "REMOVED") {
    if (!onStatusChange || busy) return;
    setBusy(true); setError("");
    try { await onStatusChange(student.accountId, next); setRemoving(null); if (next === "REMOVED") showToast("Đã gỡ sinh viên", "danger"); else showToast("Đã thêm lại sinh viên"); }
    catch (cause) { setError(formatOperationError(cause)); } finally { setBusy(false); }
  }
  const addButtons = editable && (onEnroll || onBatch) ? <div className="flex flex-wrap gap-2">
    {onBatch && <Button icon="upload_file" onClick={() => setImporting((open) => !open)} aria-expanded={importing}>Thêm theo danh sách</Button>}
    {onEnroll && <Button variant="primary" icon="add" onClick={() => setAdding(true)} data-testid="enrollment-add">Thêm sinh viên</Button>}
  </div> : null;
  return <div className="flex flex-col gap-4">
    {editable && importing && onBatch && <BatchEnrollPanel onBatch={onBatch} onClose={() => setImporting(false)} />}
    <section aria-label="Sinh viên trong lớp" className="flex flex-col gap-3.5 rounded-xl border border-line bg-surface p-5">
      {/* Một hàng công cụ: tab trạng thái, lọc nhóm, ô tìm. */}
      <div className="flex flex-wrap items-center gap-3">
        {showStatus && <Segmented
  as="tab"
  ariaLabel="Trạng thái sinh viên"
  value={status}
  onChange={(value) => { setStatus(value); setPage(0); }}
  options={[...STATUS_TABS]}
/>}
        {groupNames && (
          <Select aria-label="Lọc theo nhóm" value={group} wrapperClassName="w-48" onChange={(event) => { setGroup(event.target.value); setPage(0); }}>
            <option value="">Tất cả</option>
            <option value={NO_GROUP}>Chưa có nhóm</option>
            {groupOptions.map((name) => <option key={name} value={name}>{name}</option>)}
          </Select>
        )}
        <div role="search" className="ml-auto w-full sm:w-72">
          <TextInput type="search" icon="search" aria-label="Lọc theo tên hoặc email" placeholder="Lọc theo tên hoặc email"
            value={query} maxLength={200} onChange={(event) => { setQuery(event.target.value); setPage(0); }} />
        </div>
        {addButtons && !actionsHost && addButtons}
      </div>
      {addButtons && actionsHost && createPortal(addButtons, actionsHost)}
      {error && !removing && <p role="alert" className="text-sm text-danger">{error}</p>}
      <TableFrame filledHead className="[&_table]:min-w-[640px] [&_table]:table-fixed">
        <colgroup>{numbered && <col className="w-[60px]" />}<col /><col className="w-[30%]" />{groupNames !== undefined && <col className="w-[130px]" />}<col className="w-[120px]" />{actions && <col className="w-[56px]" />}</colgroup><thead><tr>
          {numbered && <Th className="w-12">STT</Th>}<Th>Họ tên</Th><Th>Email</Th>
          {groupNames !== undefined && <Th>Nhóm</Th>}
          <Th>Ngày vào lớp</Th>
          {actions && <Th className="w-12"><span className="sr-only">Thao tác</span></Th>}
        </tr></thead>
        <tbody>
          {rows.map((student, index) => {
            const cells = <>
              {numbered && <Td className="text-muted">{current * PAGE_SIZE + index + 1}</Td>}
              <Td><span className="flex flex-wrap items-center gap-2">{onView ? <button type="button" aria-label={`Xem ${student.name}`} onClick={(event) => { event.stopPropagation(); onView(student); }}
                className="text-left font-semibold text-ink hover:text-primary-ink">{student.name}</button> : <span className="font-semibold text-ink">{student.name}</span>}
                {groupLeaders?.includes(student.accountId) && <Pill tone="warning">Trưởng nhóm</Pill>}</span></Td>
              <Td className="text-muted">{student.email}</Td>
              {groupNames !== undefined && <Td>{groupNames === null ? "—" : groupNames[student.accountId] ?? <span className="text-muted">Chưa có nhóm</span>}</Td>}
              <Td className="text-muted">{formatDate(student.enrolledAt)}</Td>
              {actions && <Td className="text-right">{student.status === "ACTIVE"
                ? <IconButton icon="trash" label={`Gỡ ${student.name}`} disabled={busy} className="text-danger! hover:bg-danger-soft" onClick={() => { setError(""); setRemoving(student); }} />
                : <IconButton icon="person_add" label={`Thêm lại ${student.name}`} disabled={busy} className="text-primary-ink! hover:bg-primary-soft" onClick={() => void handleEnrollmentChange(student, "ACTIVE")} />}</Td>}
            </>;
            return onView ? <ClickableRow key={student.accountId} onClick={() => onView(student)}>{cells}</ClickableRow> : <tr key={student.accountId}>{cells}</tr>;
          })}
          {rows.length === 0 && <tr><Td colSpan={columns} className="py-10 text-center! text-muted">Không có sinh viên phù hợp.</Td></tr>}
        </tbody>
      </TableFrame>
      <PageNav page={current} size={PAGE_SIZE} total={visible.length} onPage={setPage} />
    </section>
    {adding && onEnroll && <AddStudentDialog candidates={candidates} students={students} loadCandidates={loadCandidates} onEnroll={onEnroll} onBatch={onBatch} onClose={() => setAdding(false)} />}
    <Dialog open={removing !== null} title="Gỡ sinh viên khỏi lớp?" onClose={() => setRemoving(null)} dismissible={!busy}
      footer={<Button variant="danger" disabled={busy} onClick={() => removing && void handleEnrollmentChange(removing, "REMOVED")} data-testid="enrollment-remove-confirm">Gỡ sinh viên</Button>}>
      <p>Bạn có chắc muốn gỡ {removing?.name} khỏi lớp không?</p>
      {error && <p role="alert" className="text-sm text-danger">{error}</p>}
    </Dialog>
  </div>;
}

/** Chọn nhiều người, dùng cùng batch enrollment và giữ các dòng thất bại để thử lại. */
function AddStudentDialog({ candidates, students, loadCandidates, onEnroll, onBatch, onClose }: {
  candidates: PersonView[]; students: EnrollmentView[]; loadCandidates?: CandidateLoader;
  onEnroll: (id: string) => Promise<void>; onBatch?: (text: string) => Promise<EnrollmentRowResult[]>; onClose: () => void;
}) {
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState<PersonView[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [results, setResults] = useState<EnrollmentRowResult[]>([]);
  const options = useCandidates(candidates, query, loadCandidates, 200);
  const enrolled = new Set(students.filter(student => student.status === "ACTIVE").map(student => student.accountId));
  const q = query.trim().toLocaleLowerCase("vi");
  const rows = options.rows.filter(person => !enrolled.has(person.id) && `${person.name} ${person.email}`.toLocaleLowerCase("vi").includes(q));
  const allSelected = rows.length > 0 && rows.every(person => selected.some(item => item.id === person.id));
  const someSelected = rows.some(person => selected.some(item => item.id === person.id));
  function toggleAll() {
    setSelected(current => allSelected ? current.filter(person => !rows.some(row => row.id === person.id))
      : [...current, ...rows.filter(person => !current.some(item => item.id === person.id))]);
  }
  async function add() {
    if (!selected.length || busy) return;
    setBusy(true); setError(""); setResults([]);
    try {
      if (onBatch) {
        const next = await onBatch(selected.map(person => person.email).join("\n"));
        const failed = next.filter(row => !OK_RESULTS.has(row.result));
        const added = next.length - failed.length;
        if (added) showToast(`Đã thêm ${added} sinh viên`);
        if (!failed.length) { onClose(); return; }
        setSelected(current => current.filter(person => failed.some(row => row.email.toLowerCase() === person.email.toLowerCase())));
        setResults(failed);
      } else {
        for (const person of selected) {
          await onEnroll(person.id);
          setSelected(current => current.filter(item => item.id !== person.id));
        }
        showToast("Đã thêm sinh viên"); onClose();
      }
    } catch (cause) { setError(formatOperationError(cause)); } finally { setBusy(false); }
  }
  return <Dialog open title="Thêm sinh viên" size="lg" onClose={onClose} dismissible={!busy}
    footer={<Button variant="primary" disabled={busy || !selected.length || selected.length > 200} onClick={() => void add()} data-testid="enrollment-add-submit">Thêm {selected.length} sinh viên</Button>}>
    <TextInput type="search" icon="search" aria-label="Tìm sinh viên" placeholder="Tìm tên hoặc email" value={query} disabled={busy} onChange={event => setQuery(event.target.value)} />
    <label className="flex items-center gap-3 text-sm">
      <input type="checkbox" className="size-4 accent-primary" checked={allSelected} disabled={busy || options.loading || !rows.length}
        ref={node => { if (node) node.indeterminate = someSelected && !allSelected; }} onChange={toggleAll} />
      Chọn tất cả kết quả lọc ({rows.length})
      <span className="ml-auto text-xs text-muted">Đã chọn {selected.length}</span>
    </label>
    {options.loading && <p role="status">Đang tải sinh viên…</p>}
    {options.error && <p role="alert" className="text-sm text-danger">{options.error}</p>}
    <ul aria-label="Chọn sinh viên" className="max-h-72 overflow-y-auto rounded-lg border border-line">
      {!options.loading && rows.map(person => <li key={person.id} className="border-b border-line last:border-0">
        <label className="flex cursor-pointer items-center gap-3 px-3 py-2 hover:bg-shell">
          <input type="checkbox" className="size-4 accent-primary" checked={selected.some(item => item.id === person.id)} disabled={busy}
            onChange={event => setSelected(current => event.target.checked ? [...current, person] : current.filter(item => item.id !== person.id))} />
          <span><span className="block text-sm font-semibold">{person.name}</span><span className="block text-xs text-muted">{person.email}</span></span>
        </label>
      </li>)}
      {!options.loading && !options.error && !rows.length && <li className="p-4 text-sm text-muted">Không có sinh viên phù hợp.</li>}
    </ul>
    {options.rows.length >= 200 && <p className="text-xs text-muted">Hiển thị tối đa 200 kết quả. Tìm tên hoặc email để thu hẹp danh sách.</p>}
    {selected.length > 200 && <p role="alert">Chọn tối đa 200 sinh viên mỗi lượt.</p>}
    {results.length > 0 && <EnrollmentResultTable rows={results} />}
    {error && <p role="alert" className="text-sm text-danger">{error}</p>}
  </Dialog>;
}

/**
 * Thêm theo danh sách: dán email hoặc chọn tệp Excel một cột, Xem trước kiểm từng
 * dòng, rồi Thêm N sinh viên. Thêm hết thì đóng thẻ; còn dòng không thêm được thì hiện bảng kết quả.
 */
function BatchEnrollPanel({ onBatch, onClose }: { onBatch: (text: string) => Promise<EnrollmentRowResult[]>; onClose: () => void }) {
  const id = useId();
  const file = useRef<HTMLInputElement>(null);
  const [input, setInput] = useState("");
  const [fileName, setFileName] = useState("");
  const [preview, setPreview] = useState<EnrollmentPreviewRow[] | null>(null);
  const [results, setResults] = useState<EnrollmentRowResult[] | null>(null);
  const [page, setPage] = useState(0);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const valid = preview?.filter((row) => !row.error).length ?? 0;
  function edit(text: string) { setInput(text); setPreview(null); setResults(null); setError(""); }
  function review(text = input) {
    try { setPreview(parseEnrollmentList(text)); setPage(0); setError(""); }
    catch (cause) { setPreview(null); setError(formatOperationError(cause)); }
  }
  async function choose(next: File | undefined) {
    if (file.current) file.current.value = "";
    if (!next) return;
    setBusy(true); setError(""); setPreview(null); setResults(null); setInput(""); setFileName("");
    try {
      const text = await readEnrollmentWorkbook(next);
      setFileName(next.name);
      edit(text);
      review(text);
    } catch (cause) { setError(formatOperationError(cause)); }
    finally { setBusy(false); }
  }
  async function handleSubmit() {
    if (busy || !valid) return;
    setBusy(true); setError("");
    try {
      const rows = await onBatch(input);
      const added = rows.filter((row) => OK_RESULTS.has(row.result)).length;
      if (added) showToast(`Đã thêm ${added} sinh viên`);
      if (rows.every((row) => OK_RESULTS.has(row.result))) { onClose(); return; }
      setResults(rows); setPreview(null);
    } catch (cause) { setError(formatOperationError(cause)); } finally { setBusy(false); }
  }
  const shown = preview?.slice(page * PREVIEW_SIZE, (page + 1) * PREVIEW_SIZE) ?? [];
  return <Section title="Thêm sinh viên theo danh sách" icon="upload_file"
    actions={<button type="button" onClick={onClose} aria-label="Đóng" className="text-muted hover:text-ink"><Icon name="close" /></button>}>
    <input ref={file} type="file" accept=".xlsx" aria-label="Chọn tệp Excel" className="sr-only" disabled={busy}
      data-testid="enrollment-excel-input" onChange={(event) => void choose(event.target.files?.[0])} />
    <div className="flex flex-col gap-1.5">
      <label htmlFor={`${id}-list`} className="text-xs font-semibold text-body">Danh sách email</label>
      <textarea id={`${id}-list`} value={input} disabled={busy} onChange={(event) => edit(event.target.value)} data-testid="enrollment-list-input"
        className="min-h-28 w-full rounded-lg border border-field px-3 py-2 font-mono text-[13px] text-ink outline-none focus:border-focus disabled:bg-sunken" />
    </div>
    <div className="flex flex-wrap items-center gap-3">
      {fileName && <div className="flex min-w-0 items-center gap-2.5 rounded-xl border border-line bg-shell px-3 py-2">
        <Icon name="description" size={20} className="text-primary-ink" />
        <span className="min-w-0 truncate text-[13px] font-semibold text-ink">{fileName}</span>
      </div>}
      <Button icon="folder_open" disabled={busy} onClick={() => file.current?.click()}>{fileName ? "Chọn tệp khác" : "Chọn tệp"}</Button>
      <a href="/templates/enrollment-import.xlsx" download="mau-sinh-vien.xlsx" className="text-xs font-semibold text-primary-ink hover:underline">Tải tệp mẫu Excel</a>
      <Button icon="check" disabled={busy || !input.trim()} onClick={() => review()} data-testid="enrollment-list-preview">Xem trước</Button>
    </div>
    {error && <p role="alert" className="text-sm text-danger">{error}</p>}
    {preview && <>
      <div className="flex flex-wrap items-center gap-2" role="status">
        <Pill>{preview.length} dòng</Pill>
        <Pill tone="success">{valid} hợp lệ</Pill>
        {preview.length > valid && <Pill tone="danger">{preview.length - valid} lỗi</Pill>}
      </div>
      <TableFrame filledHead className="[&_table]:table-fixed">
        <colgroup><col className="w-[72px]" /><col /><col className="w-[220px]" /></colgroup><thead><tr><Th className="w-16">Dòng</Th><Th>Email</Th><Th>Kết quả kiểm tra</Th></tr></thead>
        <tbody>{shown.map((row) => <tr key={row.line}>
          <Td className="text-muted">{row.line}</Td>
          <Td>{row.email}</Td>
          <Td>{row.error ? <Pill tone="danger">{ENROLLMENT_RESULT_LABELS[row.error]}</Pill> : <Pill tone="success">Hợp lệ</Pill>}</Td>
        </tr>)}</tbody>
      </TableFrame>
      <PageNav page={page} size={PREVIEW_SIZE} total={preview.length} onPage={setPage} />
      <div className="flex justify-end gap-2 border-t border-line pt-3.5">
        <Button variant="primary" icon="check" disabled={busy || !valid} onClick={() => void handleSubmit()} data-testid="enrollment-list-submit">{`Thêm ${valid} sinh viên`}</Button>
      </div>
    </>}
    {results && <EnrollmentResultTable rows={results} />}
  </Section>;
}
