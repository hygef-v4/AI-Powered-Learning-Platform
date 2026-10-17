"use client";

import { ClickableRow, Pagination, Pill, TableFrame, Td, Th } from "@/shared/ui";
import { RESULT_LABELS, actionLabel, formatShort, objectTypeLabel, shortId } from "./labels";
import type { AuditLogItem, AuditLogPage } from "./types";

type Props = {
  data: AuditLogPage;
  onOpen: (item: AuditLogItem) => void;
  onPageChange: (page: number) => void;
};

export const SYSTEM_ACTOR = "Hệ thống";

/** Bảng bản ghi audit (artboard a13); bấm dòng mở popup chi tiết. */
export function AuditTable({ data, onOpen, onPageChange }: Props) {
  return (
    <TableFrame>
      <thead>
        <tr>
          <Th className="w-[11%]">Thời gian</Th>
          <Th className="w-[19%]">Người thực hiện</Th>
          <Th className="w-[15%]">Hành động</Th>
          <Th className="w-[23%]">Đối tượng</Th>
          <Th className="w-[11%]">Kết quả</Th>
          <Th className="w-[21%]">Lý do</Th>
        </tr>
      </thead>
      <tbody>
        {data.items.length === 0 && (
          <tr>
            <Td colSpan={6} className="text-muted" data-testid="audit-table-empty">
              Không có bản ghi nào khớp bộ lọc.
            </Td>
          </tr>
        )}
        {data.items.map((item) => (
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
            <Td className="whitespace-nowrap">{formatShort(item.occurredAt)}</Td>
            <Td>
              {item.actorId ? (
                <span className="font-mono text-xs">{item.actorEmail ?? shortId(item.actorId)}</span>
              ) : (
                <i className="text-muted">{SYSTEM_ACTOR}</i>
              )}
            </Td>
            <Td className="font-bold">{actionLabel(item.action)}</Td>
            <Td>
              {item.objectType ? objectTypeLabel(item.objectType) : "—"}
              {item.objectId && (
                <>
                  {" · "}
                  <span className="font-mono text-xs">{shortId(item.objectId)}</span>
                </>
              )}
            </Td>
            <Td>
              <Pill tone={RESULT_LABELS[item.result].tone}>{RESULT_LABELS[item.result].label}</Pill>
            </Td>
            <Td className="whitespace-normal text-body">{item.reason ?? "—"}</Td>
          </ClickableRow>
        ))}
      </tbody>
      <tfoot>
        <tr>
          <td colSpan={6} className="px-3.5 py-2.5">
            <Pagination
              page={data.page}
              pageSize={data.size}
              total={data.totalElements}
              onPageChange={onPageChange}
              note={`${data.size} dòng mỗi trang`}
              testId="audit-table-pagination"
            />
          </td>
        </tr>
      </tfoot>
    </TableFrame>
  );
}
