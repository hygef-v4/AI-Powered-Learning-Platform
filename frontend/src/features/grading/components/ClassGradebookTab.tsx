"use client";
import Link from "next/link";
import { useUrlState } from "@/shared/navigation/useUrlState";
import { useRouter, useSearchParams } from "next/navigation";
import { useCallback, useEffect, useState, type ReactNode } from "react";
import { createPortal } from "react-dom";
import { Button, ButtonLink, Card, ClickableRow, cx, Icon, Pill, Segmented, Select, TableFrame, Td, TextInput, Th, type Tone } from "@/shared/ui";
import { Pagination } from "@/shared/ui/Pagination";
import { gradingApi } from "../api";
import { useResource } from "../hooks/useResource";
import { GRADE_STATUS_LABELS, type Decimal, type GradebookPage, type GradeCell } from "../model/types";
import { GradebookBatchControls } from "./GradebookBatchControls";
import { GradebookMatrix } from "./GradebookMatrix";
import { PersonSubline } from "@/shared/ui/PersonSubline";
import { msg } from "@/i18n/msg";

type Assignment = GradebookPage["assignments"][number];
type Mode = "assignment" | "student";

/** Trạng thái lọc được trên bảng điểm, theo thứ tự công việc của giảng viên. */
const STATUS_FILTERS = ["NOT_SUBMITTED", "PENDING", "DRAFT", "PUBLISHED"] as const;

// Chọn màu theo trạng thái người dạy cần nhận diện trong bảng.
function resolveStatusTone(status: string): Tone {
  if (status === "PUBLISHED") return "success";
  if (status === "DRAFT" || status === "FINALIZED") return "info";
  if (status === "PENDING") return "warning";
  return "neutral";
}

// Chọn nhãn chấm hoặc xem dựa trên bài đã nộp và trạng thái bản chấm.
function describeAction(status: string, evaluated: boolean) {
  if (!evaluated) return msg("grading.components.ClassGradebookTab.notSubmitted");
  if (status === "PENDING") return msg("grading.components.ClassGradebookTab.grade");
  if (status === "DRAFT") return msg("grading.components.ClassGradebookTab.continueGrading");
  return msg("grading.components.ClassGradebookTab.viewGrade");
}

// Chuyển kiểu nộp nhóm hoặc cá nhân thành nhãn đã dịch.
const describeKind = (assignment: Assignment) => (assignment.submissionMode === "GROUP" ? msg("grading.components.ClassGradebookTab.group") : msg("grading.components.ClassGradebookTab.individual"));
/** Số điểm bỏ số 0 thừa ở cuối: 8.50 → 8.5, 10.00 → 10. */
const trim = (value: Decimal) => String(Number(Number(value).toFixed(2)));
// Gộp các trạng thái nội bộ vào nhóm trạng thái hiển thị trong sổ điểm.
const resolveCellStatus = (grade: GradeCell | undefined) => {
  const raw = grade?.status ?? "NOT_SUBMITTED";
  if (raw === "IN_PROGRESS" || raw === "NOT_SUBMITTED") return "NOT_SUBMITTED";
  if (raw === "FINALIZED") return "DRAFT";
  return raw;
};

// Chọn bảng ma trận hoặc chế độ sổ điểm mặc định theo URL.
export function ClassGradebookTab({ classId, headerActions }: { classId: string; headerActions?: HTMLElement | null }) {
  const params = useSearchParams();
  if (params?.get("gradebookView") === "matrix") return <GradebookMatrix key={classId} classId={classId} headerActions={headerActions} />;
  return <GradebookContent key={classId} classId={classId} headerActions={headerActions} />;
}

/**
 * Sổ điểm (U09) theo kiểu danh sách – chi tiết, dùng được cả khi lớp có hàng trăm bài: chọn "Theo bài tập" hay "Theo sinh
 * viên"; cột trái là danh sách có ô tìm và cuộn riêng, cột phải là bảng điểm của mục đang chọn với ô tìm và lọc trạng thái.
 * Hai bảng cùng độ rộng cột. Mở từ trang lớp thì `?assignment=` hoặc `?student=` chọn sẵn mục tương ứng. Nút Bảng điểm
 * mở ma trận sinh viên/bài tập và xuất tệp. Chỉ bài tính điểm và lượt được chấm, không cộng tổng hay
 * trọng số (BR-U09).
 */
function GradebookContent({ classId, headerActions }: { classId: string; headerActions?: HTMLElement | null }) {
  const params = useSearchParams();
  const [query, setQuery] = useUrlState<string>("gradebookQuery", "", undefined, "replace");
  // Bài nhóm: ô tìm lọc bảng bài nộp theo tên nhóm, không tìm sinh viên trên máy chủ.
  const [groupQuery, setGroupQuery] = useState("");
  const [pageValue, setPageValue] = useUrlState<string>("gradebookPage", "1", undefined, "replace");
  const page = /^[1-9]\d*$/.test(pageValue) && Number.isSafeInteger(Number(pageValue)) ? Number(pageValue) : 1;
  // Lưu số trang vào trạng thái URL để giữ bộ lọc hiện hành.
  const setPage = (page: number) => setPageValue(String(page));
  const [mode, setMode] = useUrlState<Mode>("gradebookMode", params?.get("student") ? "student" : "assignment", ["assignment", "student"]);
  const [selectedAssignmentId, setSelectedAssignmentId] = useUrlState<string>("assignment", "");
  const [selectedStudentId, setSelectedStudentId] = useUrlState<string>("student", "");
  const [assignmentQuery, setAssignmentQuery] = useUrlState<string>("gradebookAssignmentQuery", "", undefined, "replace");
  const [status, setStatus] = useUrlState<string>("gradebookStatus", "", ["", ...STATUS_FILTERS]);

  // Gõ tìm sinh viên: hỏi máy chủ sau khi ngừng gõ một nhịp, không theo từng phím.
  const [search, setSearch] = useState("");
  useEffect(() => {
    const timer = setTimeout(() => setSearch(query.trim()), 300);
    return () => clearTimeout(timer);
  }, [query]);
  // Tải trang sổ điểm theo lớp, tìm kiếm và trang đang chọn.
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
  // Tra ô điểm theo ID bài tập trong hàng của sinh viên.
  const gradeOf = (cells: GradeCell[], assignmentId: string) => cells.find((cell) => cell.assignmentId === assignmentId);
  // Giữ ô điểm khi không lọc hoặc trạng thái hiển thị khớp bộ lọc.
  const keep = (grade: GradeCell | undefined) => !status || resolveCellStatus(grade) === status;

  const matrixParams = new URLSearchParams(params?.toString());
  matrixParams.set("tab", "gradebook");
  matrixParams.set("gradebookView", "matrix");
  const gradebookButton = <ButtonLink href={`/teaching/classes/${encodeURIComponent(classId)}?${matrixParams}`} icon="assignment">{msg("grading.components.ClassGradebookTab.gradebook")}</ButtonLink>;
  const studentSearch = (
    <TextInput type="search" icon="search" aria-label={msg("grading.components.ClassGradebookTab.findStudents")} placeholder={msg("grading.components.ClassGradebookTab.searchByNameEmailOr")} value={query}
      onChange={(e) => { setQuery(e.target.value); setPage(1); }} />
  );
  const groupSearch = (
    <TextInput type="search" icon="search" aria-label={msg("grading.components.ClassGradebookTab.findGroups")} placeholder={msg("grading.components.ClassGradebookTab.findGroups")} value={groupQuery}
      onChange={(e) => setGroupQuery(e.target.value)} />
  );
  const assignmentSearch = (
    <TextInput type="search" icon="search" aria-label={msg("grading.components.ClassGradebookTab.findAssignments")} placeholder={msg("grading.components.ClassGradebookTab.findAssignments")} value={assignmentQuery}
      onChange={(e) => setAssignmentQuery(e.target.value)} />
  );
  const statusFilter = (
    <Select aria-label={msg("grading.components.ClassGradebookTab.filterByStatus")} value={status} wrapperClassName="w-44 shrink-0" onChange={(e) => setStatus(e.target.value)}>
      <option value="">{msg("grading.components.ClassGradebookTab.allStatuses")}</option>
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
      {headerActions ? createPortal(gradebookButton, headerActions) : <div className="flex justify-end">{gradebookButton}</div>}
      <Card as="section" padding="lg" className="flex min-w-0 flex-col gap-3.5" aria-label={msg("grading.components.ClassGradebookTab.grading")}>
        <Segmented as="tab" ariaLabel={msg("grading.components.ClassGradebookTab.gradingView")} value={mode} onChange={setMode} className="self-start"
          options={[{ value: "assignment", label: msg("grading.components.ClassGradebookTab.byAssignment") }, { value: "student", label: msg("grading.components.ClassGradebookTab.byStudent") }]} />

        {resource.error && (
          <div role="alert" className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-danger-line bg-danger-soft px-3.5 py-2.5 text-sm text-danger">
            <span>{resource.error}</span>
            <Button size="sm" onClick={() => void resource.refresh()}>{msg("grading.components.ClassGradebookTab.tryAgain")}</Button>
          </div>
        )}
        {!data && !resource.error && <p role="status" className="py-8 text-center text-sm text-muted">{msg("grading.components.ClassGradebookTab.loadingGradingTable")}</p>}
        {data && assignments.length === 0 && <p className="py-8 text-center text-sm text-muted">{msg("grading.components.ClassGradebookTab.noGradedAssignmentsInThis")}</p>}

        {data && assignments.length > 0 && (
          <div className="grid min-w-0 gap-4 lg:grid-cols-[300px_minmax(0,1fr)]">
            {/* Cột trái: danh sách để chọn. */}
            <div className="flex min-w-0 flex-col gap-2.5 lg:border-r lg:border-line lg:pr-4">
              {mode === "assignment" ? assignmentSearch : studentSearch}
              <ul aria-label={mode === "assignment" ? msg("grading.components.ClassGradebookTab.assignments") : msg("grading.components.ClassGradebookTab.students")}
                className="-mx-1 flex max-h-[62vh] min-h-40 flex-col overflow-y-auto px-1 [scrollbar-color:var(--c-muted)_transparent]">
                {mode === "assignment" ? shownAssignments.map((assignment) => (
                  <PickItem key={assignment.id} active={assignment.id === activeAssignment?.id} onPick={() => setSelectedAssignmentId(assignment.id)}
                    title={assignment.title} meta={describeKind(assignment)} />
                )) : data.rows.map((row) => (
                  <PickItem key={row.student.accountId} active={row.student.accountId === activeStudent?.student.accountId}
                    onPick={() => setSelectedStudentId(row.student.accountId)} title={row.student.name} meta={<PersonSubline studentCode={row.student.studentCode} email={row.student.email} />} />
                ))}
                {mode === "assignment" && shownAssignments.length === 0 && <li className="py-6 text-center text-[13px] text-muted">{msg("grading.components.ClassGradebookTab.noMatchingAssignments")}</li>}
                {mode === "student" && data.rows.length === 0 && <li className="py-6 text-center text-[13px] text-muted">{msg("grading.components.ClassGradebookTab.noMatchingStudents")}</li>}
              </ul>
              {mode === "student" && pager}
            </div>

            {/* Cột phải: bảng điểm của mục đang chọn. */}
            <div className="flex min-w-0 flex-col gap-3">
              {mode === "assignment" && activeAssignment && (
                <>
                  <DetailHead title={activeAssignment.title} meta={describeKind(activeAssignment)} filter={statusFilter}
                    search={activeAssignment.submissionMode === "GROUP" ? groupSearch : studentSearch} />
                  <GradebookBatchControls key={`batch-${activeAssignment.id}`} classId={classId}
                    assignmentId={activeAssignment.id} query={activeAssignment.submissionMode === "GROUP" ? groupQuery : query}
                    status={status} group={activeAssignment.submissionMode === "GROUP"}
                    onDone={() => void resource.refresh().catch(() => {})} />
                  {activeAssignment.submissionMode === "GROUP" && (
                    <details className="border-t border-line pt-3">
                      <summary className="cursor-pointer text-sm font-semibold text-body">{msg("grading.components.ClassGradebookTab.memberScores")}</summary>
                      <GradeTable first={msg("grading.components.ClassGradebookTab.students")}>
                        {assignmentRows.map(({ row, grade }) => (
                          <GradeRow key={row.student.accountId} grade={grade} maxScore={activeAssignment.maxScore}
                            label={<><strong className="block truncate text-ink">{row.student.name}</strong><PersonSubline className="truncate" studentCode={row.student.studentCode} email={row.student.email} /></>} />
                        ))}
                        {!assignmentRows.length && <EmptyRow text={msg("grading.components.ClassGradebookTab.noMatchingStudents")} />}
                      </GradeTable>
                      {pager}
                    </details>
                  )}
                </>
              )}
              {mode === "student" && activeStudent && (
                <>
                  <DetailHead title={activeStudent.student.name} meta={<PersonSubline studentCode={activeStudent.student.studentCode} email={activeStudent.student.email} />} search={assignmentSearch} filter={statusFilter} />
                  <GradeTable first={msg("grading.components.ClassGradebookTab.assignments")}>
                    {studentRows.map(({ assignment, grade }) => (
                      <GradeRow key={assignment.id} grade={grade} maxScore={assignment.maxScore}
                        label={<><strong className="block truncate text-ink" title={assignment.title}>{assignment.title}</strong><p className="text-xs text-muted">{describeKind(assignment)}</p></>} />
                    ))}
                    {!studentRows.length && <EmptyRow text={msg("grading.components.ClassGradebookTab.noMatchingAssignments")} />}
                  </GradeTable>
                </>
              )}
              {mode === "student" && !activeStudent && <p className="py-8 text-center text-sm text-muted">{msg("grading.components.ClassGradebookTab.noMatchingStudents")}</p>}
            </div>
          </div>
        )}
      </Card>
    </div>
  );
}

/** Đầu cột phải: tên mục đang chọn (dài thì cắt bằng dấu ...), dòng phụ, lọc trạng thái rồi ô tìm, cùng một hàng. */
function DetailHead({ title, meta, search, filter }: { title: string; meta: ReactNode; search: ReactNode; filter: ReactNode }) {
  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
      <div className="min-w-0 flex-1">
        <h3 className="truncate text-[15px] font-bold text-ink" title={title}>{title}</h3>
        <p className="truncate text-xs text-muted">{meta}</p>
      </div>
      <div className="flex flex-wrap items-center gap-3">
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
      <thead><tr><Th>{first}</Th><Th>{msg("grading.components.ClassGradebookTab.score")}</Th><Th>{msg("grading.components.ClassGradebookTab.status")}</Th><Th><span className="sr-only">{msg("grading.components.ClassGradebookTab.actions")}</span></Th></tr></thead>
      <tbody>{children}</tbody>
    </TableFrame>
  );
}

// Hiển thị thông báo trống trong một hàng phủ toàn bảng.
function EmptyRow({ text }: { text: string }) {
  return <tr><Td colSpan={4} className="py-10 text-center! text-muted">{text}</Td></tr>;
}

/** Một mục trong danh sách chọn bên trái. */
function PickItem({ title, meta, active, onPick }: { title: string; meta: ReactNode; active: boolean; onPick: () => void }) {
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
  const status = resolveCellStatus(grade);
  const evaluationId = grade?.evaluationId;
  const hasEvaluation = Boolean(evaluationId && status !== "NOT_SUBMITTED");
  const href = hasEvaluation ? `/teaching/evaluations/${evaluationId}` : undefined;
  const action = describeAction(status, hasEvaluation);
  return (
    <ClickableRow onClick={href ? () => router.push(href) : undefined}>
      <Td className="min-w-0">{label}</Td>
      <Td className="font-semibold whitespace-nowrap tabular-nums">
        {grade?.score == null ? "—" : `${trim(grade.score)} / ${trim(grade.maxScore ?? maxScore)}`}
      </Td>
      <Td>
        <div className="flex flex-wrap items-center gap-1.5">
          <Pill tone={resolveStatusTone(status)}>{GRADE_STATUS_LABELS[status] ?? status}</Pill>
          {grade?.late && <Pill tone="danger">{msg("grading.components.ClassGradebookTab.late")}</Pill>}
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
