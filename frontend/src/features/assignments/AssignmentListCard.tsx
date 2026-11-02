"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { SlotOutlet } from "@/shared/slots/SlotOutlet";
import { Alert, Button, ClickableRow, Icon, PageNav, Pill, Segmented, Select, TableFrame, Td, TextInput, Th } from "@/shared/ui";
import type { ListFilter } from "./api";
import { errorMessage, formatDateTime } from "./format";
import { assignmentRowActions } from "./slots";
import { statusLabels, statusTones, type AssignmentRow, type AssignmentStatus } from "./types";
import type { useAssignmentPage } from "./useAssignmentPage";

type Tab = "open" | "draft" | "closed";
type Kind = "INDIVIDUAL" | "GROUP" | "PRACTICE";

const TABS: { value: Tab; label: string; statuses: AssignmentStatus[]; empty: string }[] = [
  { value: "open", label: "Đang mở", statuses: ["OPEN", "SCHEDULED"], empty: "Không có bài đang mở." },
  { value: "draft", label: "Nháp", statuses: ["DRAFT", "REVIEWED"], empty: "Không có bài nháp." },
  { value: "closed", label: "Đã đóng", statuses: ["CLOSED", "RETIRED"], empty: "Chưa có bài đã đóng." },
];
const KIND_LABELS: Record<Kind, string> = { INDIVIDUAL: "Cá nhân", GROUP: "Nhóm", PRACTICE: "Luyện tập" };
const PAGE_SIZE = 10;
/** Thẻ lọc tại chỗ nên tải mọi bài một lần (kể cả bài đã đóng). */
export const ALL_ASSIGNMENTS: ListFilter = { includeClosed: true };

/** Loại bài như trên bảng Bài tập của sinh viên: bài luyện tập là loại riêng, còn lại theo cá nhân hay nhóm. */
function kindOf(row: AssignmentRow): Kind {
  return row.gradingMode === "PRACTICE" ? "PRACTICE" : row.submissionMode;
}

/**
 * Danh sách bài tập của môn hoặc của lớp, cùng kiểu trang Bài tập của sinh viên: tab trạng thái bên trái, lọc loại và tìm
 * theo tên bên phải, bảng cột cố định, phân trang. Có `classId` thì thêm cột Phạm vi (Lớp / Môn) và nút Bài nộp sang trang Chấm điểm (U09 gắn). Nút Tạo bài tập
 * nằm ngoài thẻ, do trang chứa đặt.
 */
export function AssignmentListCard({ page, classId }: { page: ReturnType<typeof useAssignmentPage>; classId?: string }) {
  const router = useRouter();
  const [tab, setTab] = useState<Tab>("open");
  const [kind, setKind] = useState<Kind | "">("");
  const [query, setQuery] = useState("");
  const [pageNo, setPageNo] = useState(0);
  const items = page.data?.items ?? [];
  const q = query.toLocaleLowerCase("vi").trim();
  const inTab = (row: AssignmentRow, value: Tab) => TABS.find((t) => t.value === value)!.statuses.includes(row.status);
  const visible = items.filter((row) => inTab(row, tab) && (!kind || kindOf(row) === kind) && row.title.toLocaleLowerCase("vi").includes(q));
  const totalPages = Math.max(1, Math.ceil(visible.length / PAGE_SIZE));
  const current = Math.min(pageNo, totalPages - 1);
  const shown = visible.slice(current * PAGE_SIZE, (current + 1) * PAGE_SIZE);
  const href = (row: AssignmentRow) => `/assignments/${encodeURIComponent(row.id)}${classId ? `?classId=${encodeURIComponent(classId)}` : ""}`;
  const columns = classId ? 7 : 5;
  const filtered = !!kind || !!q;

  return (
    <section aria-label="Danh sách bài tập" className="flex flex-col gap-3.5 rounded-xl border border-line bg-surface p-5">
      <div className="flex flex-wrap items-center gap-3">
        <Segmented as="tab" ariaLabel="Trạng thái bài" value={tab} onChange={(value) => { setTab(value); setPageNo(0); }}
          options={TABS.map((t) => ({ value: t.value, label: <>{t.label}{page.data && <span className="ml-1 tabular-nums text-muted">{items.filter((row) => inTab(row, t.value)).length}</span>}</> }))} />
        <div className="ml-auto flex flex-wrap items-center gap-2">
          <Select aria-label="Loại bài" value={kind} wrapperClassName="w-36" onChange={(e) => { setKind(e.target.value as Kind | ""); setPageNo(0); }}>
            <option value="">Mọi loại</option>
            {(Object.keys(KIND_LABELS) as Kind[]).map((value) => <option key={value} value={value}>{KIND_LABELS[value]}</option>)}
          </Select>
          <TextInput type="search" icon="search" aria-label="Tìm bài tập" placeholder="Tìm theo tên" value={query} wrapperClassName="w-56"
            onChange={(e) => { setQuery(e.target.value); setPageNo(0); }} />
        </div>
      </div>
      {page.error !== null && <Alert tone="danger" role="alert" action={<Button size="sm" onClick={page.reload}>Thử lại</Button>}>{errorMessage(page.error)}</Alert>}
      <TableFrame filledHead className="[&_table]:min-w-[720px] [&_table]:table-fixed">
        <colgroup>
          <col /><col className="w-[110px]" />{classId && <col className="w-[80px]" />}<col className="w-[150px]" /><col className="w-[120px]" />{classId && <col className="w-[100px]" />}<col className="w-[48px]" />
        </colgroup>
        <thead>
          <tr>
            <Th>Bài tập</Th><Th>Loại</Th>{classId && <Th>Phạm vi</Th>}<Th>Hạn nộp</Th><Th>Trạng thái</Th>{classId && <Th><span className="sr-only">Bài nộp</span></Th>}
            <Th><span className="sr-only">Mở bài</span></Th>
          </tr>
        </thead>
        <tbody>
          {shown.map((row) => (
            <ClickableRow key={row.id} data-testid={`assignment-row-${row.id}`} onClick={() => router.push(href(row))}>
              <Td><Link href={href(row)} onClick={(e) => e.stopPropagation()} className="block truncate font-semibold text-ink hover:text-primary-ink">{row.title}</Link></Td>
              <Td className="whitespace-nowrap">{KIND_LABELS[kindOf(row)]}</Td>
              {classId && <Td className="whitespace-nowrap">{row.scope === "SUBJECT" ? "Môn" : "Lớp"}</Td>}
              <Td className="whitespace-nowrap"><span className="font-mono text-xs text-muted">{formatDateTime(row.closesAt)}</span></Td>
              <Td><Pill tone={statusTones[row.status]}>{statusLabels[row.status]}</Pill></Td>
              {classId && <Td><SlotOutlet slot={assignmentRowActions} props={{ assignmentId: row.id, classId, practice: row.gradingMode === "PRACTICE" }} /></Td>}
              <Td className="text-faint"><Icon name="chevron_right" size={18} /></Td>
            </ClickableRow>
          ))}
          {page.loading && !page.data && <tr><Td colSpan={columns} className="py-10 text-center! text-muted"><span role="status">Đang tải bài tập…</span></Td></tr>}
          {page.data && shown.length === 0 && (
            <tr><Td colSpan={columns} className="py-10 text-center! text-muted">{filtered ? "Không có bài tập khớp bộ lọc." : TABS.find((t) => t.value === tab)!.empty}</Td></tr>
          )}
        </tbody>
      </TableFrame>
      <PageNav page={current} size={PAGE_SIZE} total={visible.length} onPage={setPageNo} />
    </section>
  );
}
