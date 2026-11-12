"use client";

import { Pagination, Pill, Td } from "@/shared/ui";
import { cx } from "@/shared/ui/cx";
import { RESULT_LABELS, actionLabel, formatDateTime, formatFull, objectTypeLabel, shortId, shortReason } from "../model/labels";
import type { AuditLogPage } from "../model/types";

type Props = {
  data: AuditLogPage;
  onPageChange: (page: number) => void;
};

export const SYSTEM_ACTOR = "Hệ thống";

/** Ô chữ dài: tối đa 2 dòng, dài hơn thì cắt (đủ chữ khi rê chuột). */
const CLAMP = "line-clamp-2 leading-snug";

const HEAD = "border-b border-line bg-sunken px-3 py-2.5 text-left text-xs font-semibold tracking-[.06em] whitespace-nowrap text-muted uppercase";

/**
 * Bảng bản ghi audit (artboard a13): nền trắng, hàng tiêu đề tô màu nền phụ, chỉ xem. Mỗi cột rộng theo nội dung của nó:
 * - STT, Kết quả (nhãn), Lý do (2–3 chữ, đủ câu khi rê chuột) hẹp cố định;
 * - Thời gian hai dòng ngắn: ngày rồi giờ (giây và ngày đầy đủ khi rê chuột);
 * - Người thực hiện (email), Hành động, Đối tượng (tên do người dùng nhập, có thể rất dài) chia phần còn lại, xuống tối đa
 *   2 dòng, dài hơn thì cắt và hiện đủ khi rê chuột.
 * Bảng dài theo nội dung, chỉ có một thanh cuộn dọc là của trang (yêu cầu 2026-11-12: bỏ thanh cuộn riêng của bảng); màn
 * hẹp thì bảng kéo ngang được.
 */
export function AuditTable({ data, onPageChange }: Props) {
  const first = data.page * data.size;
  return (
    <div className="flex flex-col gap-3">
      <div className="overflow-x-auto overflow-y-hidden overscroll-x-contain rounded-xl border border-line bg-surface"
        data-testid="audit-table-scroll">
        <table className="w-full min-w-[980px] table-fixed border-collapse [&_tbody_tr:last-child>td]:border-b-0">
          <colgroup>
            <col className="w-12" /><col className="w-[104px]" /><col className="w-[22%]" /><col className="w-[25%]" />
            <col /><col className="w-[112px]" /><col className="w-[124px]" />
          </colgroup>
          <thead>
            <tr>
              <th scope="col" className={cx(HEAD, "text-right")}>STT</th>
              <th scope="col" className={HEAD}>Thời gian</th>
              <th scope="col" className={HEAD}>Người thực hiện</th>
              <th scope="col" className={HEAD}>Hành động</th>
              <th scope="col" className={HEAD}>Đối tượng</th>
              <th scope="col" className={HEAD}>Kết quả</th>
              <th scope="col" className={HEAD}>Lý do</th>
            </tr>
          </thead>
          <tbody>
            {data.items.length === 0 && (
              <tr>
                <Td colSpan={7} className="px-3 text-muted" data-testid="audit-table-empty">
                  Không có bản ghi nào phù hợp.
                </Td>
              </tr>
            )}
            {data.items.map((item, index) => {
              const when = formatDateTime(item.occurredAt);
              const [time, date] = [when.slice(0, 5), when.slice(6)];
              const action = actionLabel(item.action);
              const object = item.objectLabel ?? (item.objectType ? objectTypeLabel(item.objectType) : "—");
              return (
                <tr key={item.id} data-testid={`audit-table-row-${item.id}`}>
                  <Td className="px-3 text-right text-muted tabular-nums first:pl-3">{first + index + 1}</Td>
                  <Td className="px-3 leading-tight tabular-nums" title={formatFull(item.occurredAt)}>
                    <span className="block">{date}</span>
                    <span className="block text-xs text-muted">{time}</span>
                  </Td>
                  <Td className="px-3">
                    {item.actorId ? (
                      <span className={CLAMP + " font-mono text-xs break-all"} title={item.actorEmail ?? item.actorId}>{item.actorEmail ?? shortId(item.actorId)}</span>
                    ) : (
                      <i className="text-muted">{SYSTEM_ACTOR}</i>
                    )}
                  </Td>
                  <Td className="px-3">
                    <span className={CLAMP + " font-semibold [overflow-wrap:anywhere]"} title={action === item.action ? item.action : `${action} (${item.action})`}>{action}</span>
                  </Td>
                  <Td className="px-3">
                    <span className={CLAMP + " [overflow-wrap:anywhere]"} title={item.objectType ? `${objectTypeLabel(item.objectType)}: ${object}` : object}>{object}</span>
                  </Td>
                  <Td className="px-3">
                    <Pill tone={RESULT_LABELS[item.result].tone}>{RESULT_LABELS[item.result].label}</Pill>
                  </Td>
                  <Td className="px-3 last:pr-3">
                    <span className="block truncate text-body" title={item.reason ?? undefined}>{shortReason(item.reason)}</span>
                  </Td>
                </tr>
              );
            })}
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
