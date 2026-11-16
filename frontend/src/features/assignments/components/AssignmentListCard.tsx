"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useUrlState } from "@/shared/navigation/useUrlState";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Alert, Button, Card, ClickableRow, Icon, Pagination, Pill, Segmented, Select, TableFrame, Td, TextInput, Th } from "@/shared/ui";
import type { ListFilter } from "../api";
import { describeError, formatDateTime } from "../model/format";
import { assignmentRowActions } from "../slots";
import { rememberAssignmentsPage } from "../model/outline";
import { statusLabels, statusTones, type AssignmentRow, type AssignmentStatus } from "../model/types";
import type { useAssignmentPage } from "../hooks/useAssignmentPage";
import { msg } from "@/i18n/msg";

type Tab = "open" | "draft" | "finalized" | "closed";
type Kind = "INDIVIDUAL" | "GROUP" | "PRACTICE";

const TABS: { value: Tab; label: string; statuses: AssignmentStatus[]; empty: string }[] = [
  { value: "open", label: msg("assignments.components.AssignmentListCard.open"), statuses: ["OPEN", "SCHEDULED"], empty: msg("assignments.components.AssignmentListCard.noOpenAssignments") },
  { value: "draft", label: msg("assignments.components.AssignmentListCard.draft"), statuses: ["DRAFT"], empty: msg("assignments.components.AssignmentListCard.noDraftAssignments") },
  // Bài đã hoàn tất, chờ xếp lịch giao (yêu cầu 2026-11-11).
  { value: "finalized", label: msg("assignments.components.AssignmentListCard.finalized"), statuses: ["REVIEWED"], empty: msg("assignments.components.AssignmentListCard.noFinalizedAssignments") },
  { value: "closed", label: msg("assignments.components.AssignmentListCard.closed"), statuses: ["CLOSED", "RETIRED"], empty: msg("assignments.components.AssignmentListCard.noClosedAssignmentsYet") },
];
const KIND_LABELS: Record<Kind, string> = { INDIVIDUAL: msg("assignments.components.AssignmentListCard.individual"), GROUP: msg("assignments.components.AssignmentListCard.group"), PRACTICE: msg("assignments.components.AssignmentListCard.practice") };
const PAGE_SIZE = 10;
/** Thẻ lọc tại chỗ nên tải mọi bài một lần (kể cả bài đã đóng). */
export const ALL_ASSIGNMENTS: ListFilter = { includeClosed: true };

/** Loại bài như trên bảng Bài tập của sinh viên: bài luyện tập là loại riêng, còn lại theo cá nhân hay nhóm. */
function classifyKind(row: AssignmentRow): Kind {
  return row.gradingMode === "PRACTICE" ? "PRACTICE" : row.submissionMode;
}

/**
 * Danh sách bài tập của môn hoặc của lớp, cùng kiểu trang Bài tập của sinh viên: tab trạng thái bên trái, lọc loại và tìm
 * theo tên bên phải, bảng cột cố định, phân trang. Có `classId` thì thêm cột Phạm vi (Lớp / Môn) và nút Bài nộp sang trang Chấm điểm (U09 gắn). Nút Tạo bài tập
 * nằm ngoài thẻ, do trang chứa đặt.
 */
export function AssignmentListCard({ page, classId }: { page: ReturnType<typeof useAssignmentPage>; classId?: string }) {
  const router = useRouter();
  const [tab, setTab] = useUrlState<Tab>("assignmentState", "open", ["open", "draft", "finalized", "closed"]);
  const [kind, setKind] = useUrlState<Kind | "">("assignmentKind", "", ["", "INDIVIDUAL", "GROUP", "PRACTICE"]);
  const [query, setQuery] = useUrlState<string>("assignmentQuery", "", undefined, "replace");
  const [pageValue, setPageValue] = useUrlState<string>("assignmentPage", "0", undefined, "replace");
  const pageNo = /^\d+$/.test(pageValue) && Number.isSafeInteger(Number(pageValue)) ? Number(pageValue) : 0;
  const setPageNo = (page: number) => setPageValue(String(page));
  const items = page.data?.items ?? [];
  const q = query.toLocaleLowerCase("vi").trim();
  const isInTab = (row: AssignmentRow, value: Tab) => TABS.find((t) => t.value === value)!.statuses.includes(row.status);
  const visible = items.filter((row) => isInTab(row, tab) && (!kind || classifyKind(row) === kind) && row.title.toLocaleLowerCase("vi").includes(q));
  const totalPages = Math.max(1, Math.ceil(visible.length / PAGE_SIZE));
  const current = Math.min(pageNo, totalPages - 1);
  const shown = visible.slice(current * PAGE_SIZE, (current + 1) * PAGE_SIZE);
  const buildHref = (row: AssignmentRow) => `/assignments/${encodeURIComponent(row.id)}${classId ? `?classId=${encodeURIComponent(classId)}` : ""}`;
  const columns = classId ? 7 : 5;
  const filtered = !!kind || !!q;

  return (
    <Card as="section" padding="lg" className="flex flex-col gap-3.5" aria-label={msg("assignments.components.AssignmentListCard.assignmentList")}>
      <div className="flex flex-wrap items-center gap-3">
        <Segmented as="tab" ariaLabel={msg("assignments.components.AssignmentListCard.assignmentStatus")} value={tab} onChange={(value) => { setTab(value); setPageNo(0); }}
          options={TABS.map((t) => ({ value: t.value, label: <>{t.label}{page.data && <span className="ml-1 tabular-nums text-muted">{items.filter((row) => isInTab(row, t.value)).length}</span>}</> }))} />
        <div className="ml-auto flex flex-wrap items-center gap-2">
          <Select aria-label={msg("assignments.components.AssignmentListCard.assignmentType")} value={kind} wrapperClassName="w-36" onChange={(e) => { setKind(e.target.value as Kind | ""); setPageNo(0); }}>
            <option value="">{msg("assignments.components.AssignmentListCard.allTypes")}</option>
            {(Object.keys(KIND_LABELS) as Kind[]).map((value) => <option key={value} value={value}>{KIND_LABELS[value]}</option>)}
          </Select>
          <TextInput type="search" icon="search" aria-label={msg("assignments.components.AssignmentListCard.findAssignments")} placeholder={msg("assignments.components.AssignmentListCard.searchByName")} value={query} wrapperClassName="w-56"
            onChange={(e) => { setQuery(e.target.value); setPageNo(0); }} />
        </div>
      </div>
      {page.error !== null && <Alert tone="danger" role="alert" action={<Button size="sm" onClick={page.reload}>{msg("assignments.components.AssignmentListCard.retry")}</Button>}>{describeError(page.error)}</Alert>}
      <TableFrame filledHead className="[&_table]:min-w-[720px] [&_table]:table-fixed">
        <colgroup>
          <col /><col className="w-[110px]" />{classId && <col className="w-[80px]" />}<col className="w-[150px]" /><col className="w-[120px]" />{classId && <col className="w-[100px]" />}<col className="w-[48px]" />
        </colgroup>
        <thead>
          <tr>
            <Th>{msg("assignments.components.AssignmentListCard.assignment")}</Th><Th>{msg("assignments.components.AssignmentListCard.type")}</Th>{classId && <Th>{msg("assignments.components.AssignmentListCard.scope")}</Th>}<Th>{msg("assignments.components.AssignmentListCard.deadline")}</Th><Th>{msg("assignments.components.AssignmentListCard.status")}</Th>{classId && <Th><span className="sr-only">{msg("assignments.components.AssignmentListCard.submissions")}</span></Th>}
            <Th><span className="sr-only">{msg("assignments.components.AssignmentListCard.openAssignment")}</span></Th>
          </tr>
        </thead>
        <tbody>
          {shown.map((row) => (
            <ClickableRow key={row.id} data-testid={`assignment-row-${row.id}`} onClick={() => { rememberAssignmentsPage(); router.push(buildHref(row)); }}>
              <Td><Link href={buildHref(row)} onClick={(e) => { e.stopPropagation(); rememberAssignmentsPage(); }} className="block truncate font-semibold text-ink hover:text-primary-ink">{row.title}</Link></Td>
              <Td className="whitespace-nowrap">{KIND_LABELS[classifyKind(row)]}</Td>
              {classId && <Td className="whitespace-nowrap">{row.scope === "SUBJECT" ? msg("assignments.components.AssignmentListCard.subject") : msg("assignments.components.AssignmentListCard.class")}</Td>}
              <Td className="whitespace-nowrap"><span className="font-mono text-xs text-muted">{formatDateTime(row.closesAt)}</span></Td>
              <Td><Pill tone={statusTones[row.status]}>{statusLabels[row.status]}</Pill></Td>
              {classId && (
                <Td>
                  {/* Bài nháp, bài đã hoàn tất chưa giao nên chưa có bài nộp: không có nút sang trang Chấm điểm (yêu cầu 2026-11-12). */}
                  {row.status !== "DRAFT" && row.status !== "REVIEWED" && (
                    <SlotOutlet slot={assignmentRowActions} props={{ assignmentId: row.id, classId, practice: row.gradingMode === "PRACTICE" }} />
                  )}
                </Td>
              )}
              <Td className="text-faint"><Icon name="chevron_right" size={18} /></Td>
            </ClickableRow>
          ))}
          {page.loading && !page.data && <tr><Td colSpan={columns} className="py-10 text-center! text-muted"><span role="status">{msg("assignments.components.AssignmentListCard.loadingAssignments")}</span></Td></tr>}
          {page.data && shown.length === 0 && (
            <tr><Td colSpan={columns} className="py-10 text-center! text-muted">{filtered ? msg("assignments.components.AssignmentListCard.noMatchingAssignments") : TABS.find((t) => t.value === tab)!.empty}</Td></tr>
          )}
        </tbody>
      </TableFrame>
      <Pagination page={current} pageSize={PAGE_SIZE} total={visible.length} onPageChange={setPageNo} summary={false} />
    </Card>
  );
}
