"use client";

import {useCandidates, type CandidateLoader} from "./useCandidates";
import { useId, useState } from "react";
import { Avatar, EmptyState, formatDate, Icon, Modal, operationError } from "./ui";
import { parseEnrollmentList, type EnrollmentPreviewRow } from "./enrollmentList";
import { resultLabels, type EnrollmentRowResult, type EnrollmentView, type PersonView } from "./types";
import styles from "./academics.module.css";

export function EnrollmentResultTable({ rows }: { rows: EnrollmentRowResult[] }) {
  return <div className={styles.tableWrap}><table className={styles.table}><caption className="sr-only">Kết quả từng dòng</caption><thead><tr><th scope="col">Dòng</th><th scope="col">Email</th><th scope="col">Kết quả</th></tr></thead><tbody>{rows.map((row) => <tr key={row.line}><td>{row.line}</td><td>{row.email}</td><td><span className={`${styles.badge} ${row.result === "ENROLLED" || row.result === "RESTORED" ? styles.open : styles.draft}`}>{resultLabels[row.result]}</span></td></tr>)}</tbody></table></div>;
}
export function EnrollmentPanel({ students, groupNames, editable = false, includeRemoved = false, candidates = [], onStatusChange, onEnroll, onBatch, onView, loadCandidates }: {
  loadCandidates?: CandidateLoader; students: EnrollmentView[]; groupNames?: Record<string, string> | null; editable?: boolean; includeRemoved?: boolean; candidates?: PersonView[];
  onStatusChange?: (accountId: string, status: "ACTIVE" | "REMOVED") => Promise<void>;
  onEnroll?: (accountId: string) => Promise<void>; onBatch?: (text: string) => Promise<EnrollmentRowResult[]>; onView?: (student: EnrollmentView) => void;
}) {
  const [query, setQuery] = useState("");
  const [page,setPage]=useState(1);
  const [status, setStatus] = useState<"ACTIVE" | "REMOVED">("ACTIVE");
  const [removing, setRemoving] = useState<EnrollmentView | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const showStatus = editable || includeRemoved;
  const visible = students.filter((student) => student.status === (showStatus ? status : "ACTIVE") && `${student.name} ${student.email}`.toLocaleLowerCase("vi").includes(query.toLocaleLowerCase("vi").trim()));
  const pageSize = 20;
  const totalPages = Math.ceil(visible.length / pageSize);
  const displayedPage = Math.min(page, Math.max(1, totalPages));
  const offset = (displayedPage - 1) * pageSize;
  const pagedStudents = visible.slice(offset, offset + pageSize);
  async function change(student: EnrollmentView, next: "ACTIVE" | "REMOVED") {
    if (!onStatusChange || busy) return;
    setBusy(true); setError("");
    try { await onStatusChange(student.accountId, next); setRemoving(null); } catch (cause) { setError(operationError(cause)); } finally { setBusy(false); }
  }
  return <section className={styles.panel} aria-label="Sinh viên trong lớp">
    {editable && (onEnroll || onBatch) && <AddStudents loadCandidates={loadCandidates} candidates={candidates} onEnroll={onEnroll} onBatch={onBatch}/>}
    <div className={styles.toolbar}><label className={styles.search}><Icon name="search"/><input type="search" aria-label="Tìm sinh viên" placeholder="Tìm theo tên hoặc email" value={query} onChange={(event) => {setQuery(event.target.value); setPage(1);}}/></label>
      {showStatus && <div className={styles.segmented}>{(["ACTIVE", "REMOVED"] as const).map((key) => <button key={key} aria-pressed={status === key} className={status === key ? styles.selected : ""} onClick={() => setStatus(key)}>{key === "ACTIVE" ? "Đang học" : "Đã gỡ"} · {students.filter((student) => student.status === key).length}</button>)}</div>}
    </div>
    {error && !removing && <p role="alert" className={styles.error}>{error}</p>}
    <div className={styles.tableWrap}><table className={styles.table}>
      <thead><tr><th scope="col">#</th><th scope="col">Sinh viên</th>{groupNames !== undefined && <th scope="col">Nhóm</th>}{showStatus && <th scope="col">Trạng thái</th>}<th scope="col">Ngày vào lớp</th>{editable && onStatusChange && <th scope="col"><span className="sr-only">Thao tác</span></th>}</tr></thead>
      <tbody>{pagedStudents.map((student, index) => <tr key={student.accountId}>
        <td>{offset + index + 1}</td>
        <td><div className={styles.person}><Avatar name={student.name}/><span>{onView ? <button className={styles.rowLink} aria-label={`Xem ${student.name}`} onClick={() => onView(student)}>{student.name}</button> : <strong>{student.name}</strong>}<small>{student.email}</small></span></div></td>
        {groupNames !== undefined && <td>{groupNames === null ? "—" : groupNames[student.accountId] ?? "Chưa có nhóm"}</td>}
        {showStatus && <td><span className={`${styles.badge} ${student.status === "ACTIVE" ? styles.open : styles.draft}`}>{student.status === "ACTIVE" ? "Đang học" : "Đã gỡ"}</span></td>}
        <td className={styles.muted}>{formatDate(student.enrolledAt)}</td>
        {editable && onStatusChange && <td><button disabled={busy} className={styles.textButton} aria-label={`${student.status === "ACTIVE" ? "Gỡ" : "Thêm lại"} ${student.name}`} onClick={() => { setError(""); if (student.status === "ACTIVE") setRemoving(student); else void change(student, "ACTIVE"); }}>{student.status === "ACTIVE" ? "Gỡ" : "Thêm lại"}</button></td>}
      </tr>)}</tbody>
    </table></div>
    {!visible.length && <EmptyState title="Không có sinh viên phù hợp"/>}
{visible.length>pageSize && <nav aria-label="Phân trang" className={styles.pager}><button className={styles.button} disabled={page<=1} onClick={()=>setPage(v=>Math.max(1,v-1))}>Trang trước</button><span>Trang {displayedPage} / {totalPages}</span><button className={styles.button} disabled={page>=totalPages} onClick={()=>setPage(v=>v+1)}>Trang sau</button></nav>}
    {removing && <Modal title="Gỡ sinh viên khỏi lớp?" onClose={() => setRemoving(null)} busy={busy} footer={<><button className={styles.dangerButton} disabled={busy} onClick={() => void change(removing, "REMOVED")} data-testid="enrollment-remove-confirm">{busy ? "Đang gỡ…" : "Gỡ sinh viên"}</button></>}><div className={styles.person}><Avatar name={removing.name}/><div><strong>{removing.name}</strong><small>{removing.email}</small></div></div>{error && <p role="alert" className={styles.error}>{error}</p>}</Modal>}
  </section>;
}

function AddStudents({ candidates, onEnroll, onBatch, loadCandidates }: { loadCandidates?: CandidateLoader; candidates: PersonView[]; onEnroll?: (id: string) => Promise<void>; onBatch?: (text: string) => Promise<EnrollmentRowResult[]> }) {
  const id = useId();
  const [mode, setMode] = useState<"search" | "batch">(onEnroll ? "search" : "batch");
  const [query, setQuery] = useState("");
  const [input, setInput] = useState("");
  const [preview, setPreview] = useState<EnrollmentPreviewRow[] | null>(null);
  const [results, setResults] = useState<EnrollmentRowResult[] | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const options=useCandidates(candidates,query,loadCandidates);
  async function run(action: () => Promise<void>) { if (busy) return; setBusy(true); setError(""); try { await action(); } catch (cause) { setError(operationError(cause)); } finally { setBusy(false); } }
  function edit(text: string) { setInput(text); setPreview(null); setResults(null); setError(""); }
  function review() { try { setPreview(parseEnrollmentList(input)); setError(""); } catch (cause) { setPreview(null); setError(operationError(cause)); } }
  return <div className={styles.enrollmentTools}><h2>Thêm sinh viên</h2><div className={styles.subTabs}>
    {onEnroll && <button disabled={busy} aria-pressed={mode === "search"} className={mode === "search" ? styles.activeTab : ""} onClick={() => setMode("search")}>Thêm từng người</button>}
    {onBatch && <button disabled={busy} aria-pressed={mode === "batch"} className={mode === "batch" ? styles.activeTab : ""} onClick={() => setMode("batch")}>Thêm theo danh sách</button>}
    </div>
    {mode === "search" && onEnroll ? <><label className={styles.search}><Icon name="search"/><input aria-label="Tìm sinh viên để thêm" placeholder="Nhập tên hoặc email sinh viên" value={query} disabled={busy} onChange={(event) => setQuery(event.target.value)}/></label>{query.trim() && <div className={styles.choiceList}>{options.rows.filter((person) => `${person.name} ${person.email}`.toLocaleLowerCase("vi").includes(query.toLocaleLowerCase("vi").trim())).slice(0, 20).map((person) => <div key={person.id} className={styles.choice}><div className={styles.person}><Avatar name={person.name}/><div><strong>{person.name}</strong><small>{person.email}</small></div></div><button disabled={busy} className={styles.textButton} onClick={() => void run(async () => { await onEnroll(person.id); setQuery(""); })}>Thêm</button></div>)}</div>}</> : <div className={styles.stack}>
      <div className={styles.formField}><label htmlFor={`${id}-list`}>Dán email, mỗi dòng một email</label><textarea id={`${id}-list`} className={styles.textarea} value={input} disabled={busy} onChange={(event) => edit(event.target.value)} data-testid="enrollment-list-input"/></div>
      <label className={styles.textButton}>Chọn tệp CSV một cột<input className="sr-only" type="file" accept=".csv,text/csv" disabled={busy} data-testid="enrollment-csv-input" onChange={(event) => { const file = event.target.files?.[0]; event.target.value = ""; if (!file) return; if (file.size > 102400) { setError("Danh sách tối đa 100 KB."); setPreview(null); return; } void run(async () => edit(await file.text())); }}/></label>
      {preview && <><p className={styles.muted}>{preview.filter((row) => !row.error).length}/{preview.length} dòng hợp lệ</p><div className={styles.tableWrap}><table className={styles.table}><thead><tr><th>Dòng</th><th>Email</th><th>Kiểm tra</th></tr></thead><tbody>{preview.map((row) => <tr key={row.line}><td>{row.line}</td><td>{row.email}</td><td>{row.error ? resultLabels[row.error] : "Chờ ghi danh"}</td></tr>)}</tbody></table></div></>}
      {results && <EnrollmentResultTable rows={results}/>}
      <div className={styles.actions}><button className={styles.button} disabled={busy} onClick={review} data-testid="enrollment-list-preview">Xem trước</button>{preview && onBatch && <button className={styles.primaryButton} disabled={busy || !preview.some((row) => !row.error)} onClick={() => void run(async () => { setResults(await onBatch(input)); setPreview(null); })} data-testid="enrollment-list-submit"><Icon name="users"/>{busy ? "Đang thêm…" : "Thêm vào lớp"}</button>}</div>
    </div>}
    {options.error && <p role="alert">{options.error}</p>}{options.loading && <p role="status">Đang tìm…</p>}
    {error && <p role="alert" className={styles.error}>{error}</p>}
  </div>;
}
