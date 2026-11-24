"use client";

import { Fragment, useState } from "react";
import { Card, Icon, Pagination, Pill, Td } from "@/shared/ui";
import { cx } from "@/shared/ui/cx";
import { RESULT_LABELS, describeAction, formatDateTime, formatFull, describeObjectType, shortenId, shortenReason } from "../model/labels";
import { describeOperation, describeReason, listDetailEntries } from "../model/details";
import type { AuditLogItem, AuditLogPage } from "../model/types";
import { msg } from "@/i18n/msg";

type Props = {
  data: AuditLogPage;
  onPageChange: (page: number) => void;
};

export const SYSTEM_ACTOR = msg("audit.components.AuditTable.system");

/** Ô chữ dài: tối đa 2 dòng, dài hơn thì cắt (đủ chữ khi rê chuột). */
const CLAMP = "line-clamp-2 leading-snug";

const HEAD = "border-b border-line bg-sunken px-3 py-2.5 text-left text-xs font-semibold tracking-[.06em] whitespace-nowrap text-muted uppercase";

/**
 * Bảng bản ghi audit (artboard a13): nền trắng, hàng tiêu đề tô màu nền phụ, chỉ xem. Mỗi cột rộng theo nội dung của nó:
 * - STT (kèm nút mở chi tiết), Kết quả (nhãn) hẹp cố định; Lý do là câu đầy đủ, tối đa 2 dòng;
 * - Thời gian hai dòng ngắn: ngày rồi giờ (giây và ngày đầy đủ khi rê chuột);
 * - Người thực hiện (email), Hành động, Đối tượng (tên do người dùng nhập, có thể rất dài) chia phần còn lại, xuống tối đa
 *   2 dòng, dài hơn thì cắt và hiện đủ khi rê chuột. Lần bị từ chối hiện thêm thao tác bị chặn dưới Hành động.
 * - Bấm STT mở dòng chi tiết: mã đối tượng, thao tác, vai trò lúc đó, mã lý do và các giá trị khác đã ghi.
 * Bảng dài theo nội dung, chỉ có một thanh cuộn dọc là của trang (yêu cầu 2026-11-12: bỏ thanh cuộn riêng của bảng); màn
 * hẹp thì bảng kéo ngang được.
 */
export function AuditTable({ data, onPageChange }: Props) {
  const first = data.page * data.size;
  const [openId, setOpenId] = useState<string | null>(null);
  return (
    <div className="flex flex-col gap-3">
      <Card padding="none" className="overflow-x-auto overflow-y-hidden overscroll-x-contain" data-testid="audit-table-scroll">
        <table className="w-full min-w-[980px] table-fixed border-collapse [&_tbody_tr:last-child>td]:border-b-0">
          <colgroup>
            <col className="w-16" /><col className="w-[104px]" /><col className="w-[20%]" /><col className="w-[22%]" />
            <col /><col className="w-[112px]" /><col className="w-[200px]" />
          </colgroup>
          <thead>
            <tr>
              <th scope="col" className={cx(HEAD, "text-right")}>STT</th>
              <th scope="col" className={HEAD}>{msg("audit.components.AuditTable.time")}</th>
              <th scope="col" className={HEAD}>{msg("audit.components.AuditTable.actor")}</th>
              <th scope="col" className={HEAD}>{msg("audit.components.AuditTable.action")}</th>
              <th scope="col" className={HEAD}>{msg("audit.components.AuditTable.object")}</th>
              <th scope="col" className={HEAD}>{msg("audit.components.AuditTable.result")}</th>
              <th scope="col" className={HEAD}>{msg("audit.components.AuditTable.reason")}</th>
            </tr>
          </thead>
          <tbody>
            {data.items.length === 0 && (
              <tr>
                <Td colSpan={7} className="px-3 text-muted" data-testid="audit-table-empty">
                  {msg("audit.components.AuditTable.noMatchingRecords")}</Td>
              </tr>
            )}
            {data.items.map((item, index) => {
              const when = formatDateTime(item.occurredAt);
              const [time, date] = [when.slice(0, 5), when.slice(6)];
              const action = describeAction(item.action);
              const object = item.objectLabel ?? (item.objectType ? describeObjectType(item.objectType) : "—");
              const operation = describeOperation(item);
              const reason = describeReason(item);
              const open = openId === item.id;
              return (
                <Fragment key={item.id}>
                <tr data-testid={`audit-table-row-${item.id}`}>
                  <Td className="px-2 text-right text-muted tabular-nums first:pl-2">
                    <button
                      type="button"
                      className="inline-flex items-center gap-0.5 rounded px-1 py-0.5 hover:bg-sunken hover:text-body"
                      aria-expanded={open}
                      aria-controls={`audit-detail-${item.id}`}
                      title={open ? msg("audit.model.details.ui.hideDetails") : msg("audit.model.details.ui.showDetails")}
                      onClick={() => setOpenId(open ? null : item.id)}
                      data-testid={`audit-table-toggle-${item.id}`}
                    >
                      <Icon name="chevron_right" size={14} className={cx("transition-transform", open && "rotate-90")} />
                      {first + index + 1}
                    </button>
                  </Td>
                  <Td className="px-3 leading-tight tabular-nums" title={formatFull(item.occurredAt)}>
                    <span className="block">{date}</span>
                    <span className="block text-xs text-muted">{time}</span>
                  </Td>
                  <Td className="px-3">
                    {item.actorId ? (
                      <span className={CLAMP + " font-mono text-xs break-all"} title={item.actorEmail ?? item.actorId}>{item.actorEmail ?? shortenId(item.actorId)}</span>
                    ) : (
                      <i className="text-muted">{SYSTEM_ACTOR}</i>
                    )}
                  </Td>
                  <Td className="px-3">
                    <span className={CLAMP + " font-semibold [overflow-wrap:anywhere]"} title={action === item.action ? item.action : `${action} (${item.action})`}>{action}</span>
                    {operation && (
                      <span className="block truncate text-xs text-muted" title={operation} data-testid="audit-table-operation">{operation}</span>
                    )}
                  </Td>
                  <Td className="px-3">
                    <span className={CLAMP + " [overflow-wrap:anywhere]"} title={item.objectType ? `${describeObjectType(item.objectType)}: ${object}` : object}>{object}</span>
                  </Td>
                  <Td className="px-3">
                    <Pill tone={RESULT_LABELS[item.result].tone}>{RESULT_LABELS[item.result].label}</Pill>
                  </Td>
                  <Td className="px-3 last:pr-3">
                    <span className={CLAMP + " text-body [overflow-wrap:anywhere]"} title={item.reason ?? undefined}>{reason ?? shortenReason(item.reason)}</span>
                  </Td>
                </tr>
                {open && <DetailRow item={item} />}
                </Fragment>
              );
            })}
          </tbody>
        </table>
      </Card>
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

/** Dòng chi tiết dưới một bản ghi: từng giá trị đã ghi, nhãn bên trái. */
function DetailRow({ item }: { item: AuditLogItem }) {
  const entries = listDetailEntries(item);
  return (
    <tr id={`audit-detail-${item.id}`} data-testid={`audit-table-detail-${item.id}`}>
      <Td colSpan={7} className="bg-sunken px-3">
        {entries.length === 0 ? (
          <span className="text-muted">—</span>
        ) : (
          <dl className="grid grid-cols-[minmax(120px,max-content)_1fr] gap-x-4 gap-y-1 text-[13px]">
            {entries.map((entry) => (
              <Fragment key={entry.label}>
                <dt className="text-muted">{entry.label}</dt>
                <dd className="font-mono text-xs break-all text-body">{entry.value}</dd>
              </Fragment>
            ))}
          </dl>
        )}
      </Td>
    </tr>
  );
}
