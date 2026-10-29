"use client";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useCallback, useEffect, useState, type ReactNode } from "react";
import { createPortal } from "react-dom";
import { Button, ClickableRow, Icon, Pill, Segmented, Select, TableFrame, Td, TextInput, Th, cx, type Tone } from "@/shared/ui";
import { Pagination } from "@/shared/ui/Pagination";
import { gradingApi } from "./api";
import { useResource } from "./useResource";
import { statusLabels, type Decimal, type GradebookPage, type GradeCell } from "./types";
import { GradebookExportDialog } from "./GradebookExportDialog";

type Assignment = GradebookPage["assignments"][number];
type Mode = "assignment" | "student";

/** Trạng thái lọc được trên bảng điểm, theo thứ tự công việc của giảng viên. */
const STATUS_FILTERS = ["NOT_SUBMITTED", "IN_PROGRESS", "PENDING", "DRAFT", "FINALIZED", "PUBLISHED"] as const;

function statusTone(status: string): Tone {
  if (status === "PUBLISHED") return "success";
  if (status === "FINALIZED") return "primary";
  if (status === "DRAFT") return "info";
  if (status === "PENDING") return "warning";
  return "neutral";
}

function actionLabel(status: string, evaluated: boolean) {
  if (!evaluated) return "Xem bài nộp";
  if (status === "PENDING") return "Chấm bài";
  if (status === "DRAFT") return "Tiếp tục chấm";
  return "Xem điểm";
}

const kindLabel = (assignment: Assignment) => (assignment.submissionMode === "GROUP" ? "Nhóm" : "Cá nhân");
/** Số điểm bỏ số 0 thừa ở cuối: 8.50 → 8.5, 10.00 → 10. */
const trim = (value: Decimal) => String(Number(Number(value).toFixed(2)));
const statusOf = (grade: GradeCell | undefined) => grade?.status ?? "NOT_SUBMITTED";

export function ClassGradebookTab({ classId, headerActions }: { classId: string; headerActions?: HTMLElement | null }) {
  return <GradebookContent key={classId} classId={classId} headerActions={headerActions} />;
}

/**
 * Sổ điểm (U09) theo kiểu danh sách – chi tiết, dùng được cả khi lớp có hàng trăm bài: chọn "Theo bài tập" hay "Theo sinh
 * viên"; cột trái là danh sách có ô tìm và cuộn riêng, cột phải là bảng điểm của mục đang chọn với ô tìm và lọc trạng thái.
 * Hai bảng cùng độ rộng cột. Mở từ Tiến độ nộp bài thì `?assignment=` hoặc `?student=` chọn sẵn mục tương ứng. Không có bảng
 * ma trận; cả sổ lấy bằng Xuất sổ điểm trên dòng tiêu đề trang lớp. Chỉ bài tính điểm và lượt được chấm, không cộng tổng hay
 * trọng số (BR-U09).
 */
function GradebookContent({ classId, headerActions }: { classId: string; headerActions?: HTMLElement | null }) {
  const params = useSearchParams();
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(1);
  const [mode, setMode] = useState<Mode>(() => (params?.get("student") ? "student" : "assignment"));
  const [selectedAssignmentId, setSelectedAssignmentId] = useState(() => params?.get("assignment") ?? "");
  const [selectedStudentId, setSelectedStudentId] = useState(() => params?.get("student") ?? "");
  const [assignmentQuery, setAssignmentQuery] = useState("");
  const [status, setStatus] = useState("");
  const [exporting, setExporting] = useState(false);

  // Gõ tìm sinh viên: hỏi máy chủ sau khi ngừng gõ một nhịp, không theo từng phím.
  const [search, setSearch] = useState("");
  useEffect(() => {
    const timer = setTimeout(() => setSearch(query.trim()), 300);
    return () => clearTimeout(timer);
  }, [query]);
  const load = useCallback(() => gradingApi.gradebook(classId, search, page), [classId, search, page]);
  const resource = useResource(load);
  // Giữ bảng đang hiện trong lúc tải kết quả mới, để ô tìm không bị dựng lại giữa chừng.
  const [kept, setKept] = useState<GradebookPage | null>(null);
  if (resource.value && resource.value !== kept) setKept(resource.value);
  const data = resource.value ?? kept;
  const assignments = data?.assignments ?? [];
  const activeAssignment = assignments.find((item) => item.id === selectedAssignmentId) ?? assignments[0] ?? null;
  const activeStudent = data?.rows.find((row) => row.student.accountId === selectedStudentId) ?? data?.rows[0] ?? null;
  const aq = assignmentQuery.toLocaleLowerCase("vi").trim();
  const shownAssignments = assignments.filter((item) => item.title.toLocaleLowerCase("vi").includes(aq));
  const gradeOf = (cells: GradeCell[], assignmentId: string) => cells.find((cell) => cell.assignmentId === assignmentId);
  const keep = (grade: GradeCell | undefined) => !status || statusOf(grade) === status;

  const exportButton = (
    <Button icon="download" disabled={!data || assignments.length === 0} onClick={() => setExporting(true)}>Xuất bảng điểm</Button>
  );
  const studentSearch = (
    <TextInput type="search" icon="search" aria-label="Tìm sinh viên" placeholder="Tìm sinh viên" value={query}
      onChange={(e) => { setQuery(e.target.value); setPage(1); }} />
  );
  const assignmentSearch = (
    <TextInput type="search" icon="search" aria-label="Tìm bài tập" placeholder="Tìm bài tập" value={assignmentQuery}
      onChange={(e) => setAssignmentQuery(e.target.value)} />
  );
  const statusFilter = (
    <Select aria-label="Lọc theo trạng thái" value={status} wrapperClassName="w-44 shrink-0" onChange={(e) => setStatus(e.target.value)}>
      <option value="">Mọi trạng thái</option>
      {STATUS_FILTERS.map((value) => <option key={value} value={value}>{statusLabels[value]}</option>)}
    </Select>
  );
  const pager = data && <Pagination page={page - 1} pageSize={20} total={data.total} summary={false} onPageChange={(p) => setPage(p + 1)} />;

  const assignmentRows = mode === "assignment" && activeAssignment && data
    ? data.rows.map((row) => ({ row, grade: gradeOf(row.grades, activeAssignment.id) })).filter(({ grade }) => keep(grade))
    : [];
  const studentRows = mode === "student" && activeStudent
    ? shownAssignments.map((assignment) => ({ assignment, grade: gradeOf(activeStudent.grades, assignment.id) })).filter(({ grade }) => keep(grade))
    : [];

  return (
    <div className="flex min-w-0 flex-col gap-3">
      {headerActions ? createPortal(exportButton, headerActions) : <div className="flex justify-end">{exportButton}</div>}
      <section aria-label="Chấm điểm" className="flex min-w-0 flex-col gap-3.5 rounded-xl border border-line bg-surface p-5">
        <Segmented as="tab" ariaLabel="Chế độ chấm điểm" value={mode} onChange={setMode} className="self-start"
          options={[{ value: "assignment", label: "Theo bài tập" }, { value: "student", label: "Theo sinh viên" }]} />

        {resource.error && (
          <div role="alert" className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-danger-line bg-danger-soft px-3.5 py-2.5 text-sm text-danger">
            <span>{resource.error}</span>
            <Button size="sm" onClick={() => void resource.refresh()}>Thử tải lại</Button>
          </div>
        )}
        {!data && !resource.error && <p role="status" className="py-8 text-center text-sm text-muted">Đang tải bảng chấm điểm…</p>}
        {data && assignments.length === 0 && <p className="py-8 text-center text-sm text-muted">Chưa có bài tính điểm trong lớp.</p>}

        {data && assignments.length > 0 && (
          <div className="grid min-w-0 gap-4 lg:grid-cols-[300px_minmax(0,1fr)]">
            {/* Cột trái: danh sách để chọn. */}
            <div className="flex min-w-0 flex-col gap-2.5 lg:border-r lg:border-line lg:pr-4">
              {mode === "assignment" ? assignmentSearch : studentSearch}
              <ul aria-label={mode === "assignment" ? "Bài tập" : "Sinh viên"}
                className="-mx-1 flex max-h-[62vh] min-h-40 flex-col overflow-y-auto px-1 [scrollbar-color:var(--c-muted)_transparent]">
                {mode === "assignment" ? shownAssignments.map((assignment) => (
                  <PickItem key={assignment.id} active={assignment.id === activeAssignment?.id} onPick={() => setSelectedAssignmentId(assignment.id)}
                    title={assignment.title} meta={kindLabel(assignment)} />
                )) : data.rows.map((row) => (
                  <PickItem key={row.student.accountId} active={row.student.accountId === activeStudent?.student.accountId}
                    onPick={() => setSelectedStudentId(row.student.accountId)} title={row.student.name} meta={row.student.email} />
                ))}
                {mode === "assignment" && shownAssignments.length === 0 && <li className="py-6 text-center text-[13px] text-muted">Không có bài khớp.</li>}
                {mode === "student" && data.rows.length === 0 && <li className="py-6 text-center text-[13px] text-muted">Không có sinh viên phù hợp.</li>}
              </ul>
              {mode === "student" && pager}
            </div>

            {/* Cột phải: bảng điểm của mục đang chọn. */}
            <div className="flex min-w-0 flex-col gap-3">
              {mode === "assignment" && activeAssignment && (
                <>
                  <DetailHead title={activeAssignment.title} meta={kindLabel(activeAssignment)} search={studentSearch} filter={statusFilter} />
                  <GradeTable first="Sinh viên">
                    {assignmentRows.map(({ row, grade }) => (
                      <GradeRow key={row.student.accountId} grade={grade} maxScore={activeAssignment.maxScore}
                        fallback={submissionsHref(classId, activeAssignment.id)}
                        label={<><strong className="block truncate text-ink">{row.student.name}</strong><p className="truncate text-xs text-muted">{row.student.email}</p></>} />
                    ))}
                    {!assignmentRows.length && <EmptyRow text="Không có sinh viên phù hợp." />}
                  </GradeTable>
                  {pager}
                </>
              )}
              {mode === "student" && activeStudent && (
                <>
                  <DetailHead title={activeStudent.student.name} meta={activeStudent.student.email} search={assignmentSearch} filter={statusFilter} />
                  <GradeTable first="Bài tập">
                    {studentRows.map(({ assignment, grade }) => (
                      <GradeRow key={assignment.id} grade={grade} maxScore={assignment.maxScore}
                        fallback={submissionsHref(classId, assignment.id)}
                        label={<><strong className="block truncate text-ink" title={assignment.title}>{assignment.title}</strong><p className="text-xs text-muted">{kindLabel(assignment)}</p></>} />
                    ))}
                    {!studentRows.length && <EmptyRow text="Không có bài phù hợp." />}
                  </GradeTable>
                </>
              )}
              {mode === "student" && !activeStudent && <p className="py-8 text-center text-sm text-muted">Không có sinh viên phù hợp.</p>}
            </div>
          </div>
        )}
      </section>
      {exporting && data && <GradebookExportDialog classId={classId} assignments={data.assignments} onClose={() => setExporting(false)} />}
    </div>
  );
}

/** Đầu cột phải: tên mục đang chọn (dài thì cắt bằng dấu ...), dòng phụ, lọc trạng thái rồi ô tìm, cùng một hàng. */
function DetailHead({ title, meta, search, filter }: { title: string; meta: string; search: ReactNode; filter: ReactNode }) {
  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
      <div className="min-w-0 flex-1">
        <h3 className="truncate text-[15px] font-bold text-ink" title={title}>{title}</h3>
        <p className="truncate text-xs text-muted">{meta}</p>
      </div>
      <div className="flex shrink-0 items-center gap-3">
        {filter}
        <div className="w-56">{search}</div>
      </div>
    </div>
  );
}

/** Bảng điểm dùng chung cho hai chế độ, cùng độ rộng cột. */
function GradeTable({ first, children }: { first: string; children: ReactNode }) {
  return (
    <TableFrame filledHead className="[&_table]:table-fixed">
      <colgroup><col /><col className="w-[120px]" /><col className="w-[210px]" /><col className="w-[56px]" /></colgroup>
      <thead><tr><Th>{first}</Th><Th>Điểm</Th><Th>Trạng thái</Th><Th><span className="sr-only">Thao tác</span></Th></tr></thead>
      <tbody>{children}</tbody>
    </TableFrame>
  );
}

function EmptyRow({ text }: { text: string }) {
  return <tr><Td colSpan={4} className="py-10 text-center! text-muted">{text}</Td></tr>;
}

/** Một mục trong danh sách chọn bên trái. */
function PickItem({ title, meta, active, onPick }: { title: string; meta: string; active: boolean; onPick: () => void }) {
  return (
    <li>
      <button type="button" aria-pressed={active} onClick={onPick}
        className={cx("row-interactive flex w-full items-center gap-2 rounded-lg px-2.5 py-2 text-left",
          active ? "bg-primary-soft" : "hover:bg-shell")}>
        <span className="min-w-0 flex-1">
          <span className={cx("block truncate text-[13.5px] font-semibold", active ? "text-primary-ink" : "text-ink")} title={title}>{title}</span>
          <span className="block truncate text-xs text-muted">{meta}</span>
        </span>
      </button>
    </li>
  );
}

const submissionsHref = (classId: string, assignmentId: string) =>
  `/teaching/classes/${encodeURIComponent(classId)}/assignments/${encodeURIComponent(assignmentId)}/submissions`;

/**
 * Một dòng điểm: điểm (bỏ số 0 thừa), trạng thái (kèm Trễ), nút ›. Bấm dòng nào cũng sang trang kế: có lượt chấm thì trang
 * chấm hoặc xem điểm, chưa có thì danh sách bài nộp của bài đó.
 */
function GradeRow({ label, grade, maxScore, fallback }: { label: ReactNode; grade: GradeCell | undefined; maxScore: Assignment["maxScore"]; fallback: string }) {
  const router = useRouter();
  const status = statusOf(grade);
  const href = grade?.evaluationId ? `/teaching/evaluations/${grade.evaluationId}` : fallback;
  const action = actionLabel(status, !!grade?.evaluationId);
  return (
    <ClickableRow onClick={() => router.push(href)}>
      <Td className="min-w-0">{label}</Td>
      <Td className="font-semibold whitespace-nowrap tabular-nums">
        {grade?.score == null ? "—" : `${trim(grade.score)} / ${trim(grade.maxScore ?? maxScore)}`}
      </Td>
      <Td>
        <div className="flex flex-wrap items-center gap-1.5">
          <Pill tone={statusTone(status)}>{statusLabels[status] ?? status}</Pill>
          {grade?.late && <Pill tone="danger">Trễ</Pill>}
        </div>
      </Td>
      <Td className="text-right">
        <Link href={href} aria-label={action} title={action} onClick={(event) => event.stopPropagation()}
          className="inline-flex size-8 items-center justify-center rounded-lg text-muted transition-colors hover:bg-primary-soft hover:text-primary-ink">
          <Icon name="chevron_right" size={20} />
        </Link>
      </Td>
    </ClickableRow>
  );
}
