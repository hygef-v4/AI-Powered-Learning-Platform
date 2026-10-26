"use client";

import { ClickableRow, Pagination, Pill, Td } from "@/shared/ui";
import { cx } from "@/shared/ui/cx";
import { RESULT_LABELS, actionLabel, formatDateTime, objectTypeLabel, shortId } from "./labels";
import type { AuditLogItem, AuditLogPage } from "./types";

type Props = {
  data: AuditLogPage;
  onOpen: (item: AuditLogItem) => void;
  onPageChange: (page: number) => void;
};

export const SYSTEM_ACTOR = "Hệ thống";

const HEAD = "border-b border-line bg-sunken px-3 py-2.5 text-left text-[11px] font-semibold tracking-[.06em] whitespace-nowrap text-muted uppercase";

/** Bảng bản ghi audit (artboard a13): nền trắng, hàng tiêu đề tô màu nền phụ; bấm dòng mở popup chi tiết. */
export function AuditTable({ data, onOpen, onPageChange }: Props) {
  const first = data.page * data.size;
  return (
    <div className="flex flex-col gap-3">
      <div className="overflow-x-auto rounded-xl border border-line bg-surface">
        <table className="w-full border-collapse [&_tbody_tr:last-child>td]:border-b-0">
          <thead>
            <tr>
              <th scope="col" className={cx(HEAD, "w-14 text-right")}>STT</th>
              <th scope="col" className={cx(HEAD, "w-[14%]")}>Thời gian</th>
              <th scope="col" className={cx(HEAD, "w-[19%]")}>Người thực hiện</th>
              <th scope="col" className={cx(HEAD, "w-[15%]")}>Hành động</th>
              <th scope="col" className={cx(HEAD, "w-[19%]")}>Đối tượng</th>
              <th scope="col" className={cx(HEAD, "w-[11%]")}>Kết quả</th>
              <th scope="col" className={HEAD}>Lý do</th>
            </tr>
          </thead>
          <tbody>
            {data.items.length === 0 && (
              <tr>
                <Td colSpan={7} className="px-3 text-muted" data-testid="audit-table-empty">
                  Không có bản ghi nào khớp bộ lọc.
                </Td>
              </tr>
            )}
            {data.items.map((item, index) => (
              <ClickableRow
                key={item.id}
                tabIndex={0}
                onClick={() => onOpen(item)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    onOpen(item);
                  }
                }}
                data-testid={`audit-table-row-${item.id}`}
              >
                <Td className="px-3 text-right text-muted tabular-nums first:pl-3">{first + index + 1}</Td>
                <Td className="px-3 whitespace-nowrap tabular-nums">{formatDateTime(item.occurredAt)}</Td>
                <Td className="px-3">
                  {item.actorId ? (
                    <span className="font-mono text-xs">{item.actorEmail ?? shortId(item.actorId)}</span>
                  ) : (
                    <i className="text-muted">{SYSTEM_ACTOR}</i>
                  )}
                </Td>
                <Td className="px-3 font-bold">{actionLabel(item.action)}</Td>
                <Td className="px-3">{item.objectType ? objectTypeLabel(item.objectType) : "—"}</Td>
                <Td className="px-3">
                  <Pill tone={RESULT_LABELS[item.result].tone}>{RESULT_LABELS[item.result].label}</Pill>
                </Td>
                <Td className="px-3 whitespace-normal text-body last:pr-3">{item.reason ?? "—"}</Td>
              </ClickableRow>
            ))}
          </tbody>
        </table>
      </div>
      <Pagination
        page={data.page}
        pageSize={data.size}
        total={data.totalElements}
        onPageChange={onPageChange}
        summary={false}
        testId="audit-table-pagination"
      />
    </div>
  );
}
