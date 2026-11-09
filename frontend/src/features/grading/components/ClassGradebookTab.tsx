"use client";
import Link from "next/link";
import { useUrlState } from "@/shared/navigation/useUrlState";
import { useRouter, useSearchParams } from "next/navigation";
import { useCallback, useEffect, useState, type ReactNode } from "react";
import { createPortal } from "react-dom";
import { Button, ButtonLink, ClickableRow, Icon, Pill, Segmented, Select, TableFrame, Td, TextInput, Th, cx, type Tone } from "@/shared/ui";
import { Pagination } from "@/shared/ui/Pagination";
import { gradingApi } from "../api";
import { useResource } from "../hooks/useResource";
import { useSubmissionDirectory } from "../hooks/useSubmissionDirectory";
import { buildEvaluationHref } from "../model/navigation";
import { GRADE_STATUS_LABELS, type Decimal, type GradebookPage, type GradeCell } from "../model/types";
import { GradebookExportDialog } from "./GradebookExportDialog";

type Assignment = GradebookPage["assignments"][number];
type Mode = "assignment" | "student";

/** Trạng thái lọc được trên bảng điểm, theo thứ tự công việc của giảng viên. */
const STATUS_FILTERS = ["NOT_SUBMITTED", "PENDING", "DRAFT", "PUBLISHED"] as const;

function statusTone(status: string): Tone {
  if (status === "PUBLISHED") return "success";
  if (status === "DRAFT" || status === "FINALIZED") return "info";
  if (status === "PENDING") return "warning";
  return "neutral";
}

function actionLabel(status: string, evaluated: boolean) {
  if (!evaluated) return "Chưa nộp bài";
  if (status === "PENDING") return "Chấm bài";
  if (status === "DRAFT") return "Tiếp tục chấm";
  return "Xem điểm";
}

const kindLabel = (assignment: Assignment) => (assignment.submissionMode === "GROUP" ? "Nhóm" : "Cá nhân");
/** Số điểm bỏ số 0 thừa ở cuối: 8.50 → 8.5, 10.00 → 10. */
const trim = (value: Decimal) => String(Number(Number(value).toFixed(2)));
const statusOf = (grade: GradeCell | undefined) => {
  const raw = grade?.status ?? "NOT_SUBMITTED";
  if (raw === "IN_PROGRESS" || raw === "NOT_SUBMITTED") return "NOT_SUBMITTED";
  if (raw === "FINALIZED") return "DRAFT";
  return raw;
};

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
  const [query, setQuery] = useUrlState<string>("gradebookQuery", "", undefined, "replace");
  const [pageValue, setPageValue] = useUrlState<string>("gradebookPage", "1", undefined, "replace");
  const page = /^[1-9]\d*$/.test(pageValue) && Number.isSafeInteger(Number(pageValue)) ? Number(pageValue) : 1;
  const setPage = (page: number) => setPageValue(String(page));
  const [mode, setMode] = useUrlState<Mode>("gradebookMode", params?.get("student") ? "student" : "assignment", ["assignment", "student"]);
  const [selectedAssignmentId, setSelectedAssignmentId] = useUrlState<string>("assignment", "");
  const [selectedStudentId, setSelectedStudentId] = useUrlState<string>("student", "");
  const [assignmentQuery, setAssignmentQuery] = useUrlState<string>("gradebookAssignmentQuery", "", undefined, "replace");
  const [status, setStatus] = useUrlState<string>("gradebookStatus", "", ["", ...STATUS_FILTERS]);
  const [exporting, setExporting] = useState(false);

  // Gõ tìm sinh viên: hỏi máy chủ sau khi ngừng gõ một nhịp, không theo từng phím.
  const [search, setSearch] = useState("");
  useEffect(() => {
    const timer = setTimeout(() => setSearch(query.trim()), 300);
    return () => clearTimeout(timer);
  }, [query]);
  const load = useCallback(() => gradingApi.loadGradebook(classId, search, page), [classId, search, page]);
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
      {STATUS_FILTERS.map((value) => <option key={value} value={value}>{GRADE_STATUS_LABELS[value]}</option>)}
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
                  {activeAssignment.submissionMode === "GROUP" && (
                    <GroupSubmissionLinks key={activeAssignment.id} classId={classId} assignmentId={activeAssignment.id} />
                  )}
                  <GradeTable first="Sinh viên">
                    {assignmentRows.map(({ row, grade }) => (
                      <GradeRow key={row.student.accountId} grade={grade} maxScore={activeAssignment.maxScore}
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

function GroupSubmissionLinks({ classId, assignmentId }: { classId: string; assignmentId: string }) {
  const { value, error, refresh } = useSubmissionDirectory(classId, assignmentId);
  if (error) {
    return (
      <div role="alert" className="flex flex-wrap items-center gap-2 text-sm text-danger">
        Không tải được bản nộp nhóm.
        <Button size="sm" onClick={() => void refresh().catch(() => {})}>Thử lại</Button>
      </div>
    );
  }
  if (!value) return <p role="status" className="text-xs text-muted">Đang tải bản nộp nhóm…</p>;
  const submissions = value.flatMap(({ student, evaluationId }) =>
    evaluationId ? [{ group: student, evaluationId }] : []);
  if (!submissions.length) return <p className="text-xs text-muted">Chưa có bản nộp chung để xem.</p>;

  return (
    <ul aria-label="Bản nộp chung theo nhóm"
      className="max-h-64 divide-y divide-line-soft overflow-y-auto rounded-lg border border-line">
      {submissions.map(({ group, evaluationId }) => (
        <li key={group.accountId} className="flex flex-wrap items-center justify-between gap-2 px-3 py-2">
          <span className="min-w-0 wrap-break-word text-sm font-semibold text-ink">{group.name}</span>
          <ButtonLink size="sm" href={buildEvaluationHref(evaluationId)}>
            Xem bản nộp chung{" "}
            <span className="sr-only">của {group.name}</span>
          </ButtonLink>
        </li>
      ))}
    </ul>
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

/**
 * Một dòng điểm: điểm (bỏ số 0 thừa), trạng thái (kèm Trễ), nút ›.
 * Chỉ bài đã nộp mới có link tới trang chấm điểm và hiện nút ›. Bài chưa nộp không có link hay nút ›.
 */
function GradeRow({ label, grade, maxScore }: { label: ReactNode; grade: GradeCell | undefined; maxScore: Assignment["maxScore"] }) {
  const router = useRouter();
  const status = statusOf(grade);
  const evaluationId = grade?.evaluationId;
  const hasEvaluation = Boolean(evaluationId && status !== "NOT_SUBMITTED");
  const href = hasEvaluation ? `/teaching/evaluations/${evaluationId}` : undefined;
  const action = actionLabel(status, hasEvaluation);
  return (
    <ClickableRow onClick={href ? () => router.push(href) : undefined}>
      <Td className="min-w-0">{label}</Td>
      <Td className="font-semibold whitespace-nowrap tabular-nums">
        {grade?.score == null ? "—" : `${trim(grade.score)} / ${trim(grade.maxScore ?? maxScore)}`}
      </Td>
      <Td>
        <div className="flex flex-wrap items-center gap-1.5">
          <Pill tone={statusTone(status)}>{GRADE_STATUS_LABELS[status] ?? status}</Pill>
          {grade?.late && <Pill tone="danger">Trễ</Pill>}
        </div>
      </Td>
      <Td className="text-right">
        {href ? (
          <Link href={href} aria-label={action} title={action} onClick={(event) => event.stopPropagation()}
            className="inline-flex size-8 items-center justify-center rounded-lg text-muted transition-colors hover:bg-primary-soft hover:text-primary-ink">
            <Icon name="chevron_right" size={20} />
          </Link>
        ) : null}
      </Td>
    </ClickableRow>
  );
}
