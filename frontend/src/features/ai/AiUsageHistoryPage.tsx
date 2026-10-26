"use client";

import { useEffect, useRef, useState, type ReactNode } from "react";
import { PROVIDER_LABEL, formatMaybeTokens, formatShort, formatTokens, type SourceOption } from "@/shared/ai";
import { Alert, Field, IconTile, PageHeader, Pagination, Pill, Section, TableFrame, Td, TextInput, Th, cx } from "@/shared/ui";
import { getSources, getUsage } from "./api";
import { OUTCOME_LABEL, PROVENANCE_LABEL, SOURCE_LABEL, TASK_LABEL } from "./labels";
import type { UsagePage, UsageQuery, UsageSource } from "./types";

export const PAGE_SIZE = 50;
const TABS: { key: UsageSource | ""; label: string }[] = [
  { key: "", label: "Tất cả" },
  { key: "SHARED", label: "Hạn mức trường" },
  { key: "PERSONAL", label: "API cá nhân" },
  { key: "MCP", label: "Agent MCP" },
];

/** Ngày "yyyy-mm-dd" (giờ Việt Nam) → đầu ngày hoặc đầu ngày hôm sau, dạng ISO. */
function dayBound(day: string, end: boolean): string | undefined {
  if (!day) return undefined;
  const start = new Date(`${day}T00:00:00+07:00`);
  if (end) start.setUTCDate(start.getUTCDate() + 1);
  return start.toISOString();
}

/** Màn #7 AI Usage History (UC 33): chỉ bản ghi của chính mình; token không có số hiện "Không có số liệu". */
export function AiUsageHistoryPage() {
  const [source, setSource] = useState<UsageSource | "">("");
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<UsagePage | null>(null);
  const [shared, setShared] = useState<SourceOption | null>(null);
  const [error, setError] = useState<string | null>(null);
  const request = useRef(0);

  const rangeInvalid = from !== "" && to !== "" && from > to;

  useEffect(() => {
    if (rangeInvalid) return;
    const id = ++request.current;
    const query: UsageQuery = {
      source: source || undefined,
      from: dayBound(from, false),
      to: dayBound(to, true),
      page,
      size: PAGE_SIZE,
    };
    getUsage(query).then(
      (loaded) => {
        if (id !== request.current) return;
        setData(loaded);
        setError(null);
      },
      () => {
        if (id === request.current) setError("Không tải được lịch sử sử dụng AI, thử lại sau.");
      },
    );
  }, [source, from, to, page, rangeInvalid]);

  useEffect(() => {
    let active = true;
    getSources().then(
      (sources) => {
        const option = sources.find((s) => s.source === "SHARED");
        if (active && option?.quota) setShared(option);
      },
      () => undefined,
    );
    return () => {
      active = false;
    };
  }, []);

  return (
    <div className="flex flex-col gap-4">
      <PageHeader
        title="Lịch sử sử dụng AI"
        backHref="/ai/connection"
        breadcrumb={[{ label: "Kết nối AI", href: "/ai/connection" }, { label: "Lịch sử sử dụng AI" }]}
      />
      <div className="grid gap-3 md:grid-cols-3">
        {shared?.quota && (
          <SummaryCard icon="speed" title="Token hôm nay · hạn mức trường">
            {formatTokens(shared.quota.used)} / {formatTokens(shared.quota.limit)}
            <span className="block text-xs font-normal text-muted">Còn {formatTokens(shared.quota.left)} token</span>
          </SummaryCard>
        )}
        <SummaryCard icon="key" title="API cá nhân trong kỳ">
          {data ? `${formatTokens(data.personal.requests)} yêu cầu` : "—"}
        </SummaryCard>
        <SummaryCard icon="smart_toy" title="Agent MCP trong kỳ">
          {data ? `${formatTokens(data.mcp.requests)} lượt gọi tool` : "—"}
          <span className="block text-xs font-normal text-muted">
            {data && data.mcp.tokens > 0 ? `${formatTokens(data.mcp.tokens)} token do agent tự báo` : "Token chỉ có khi agent tự báo"}
          </span>
        </SummaryCard>
      </div>
      <Section>
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div role="tablist" aria-label="Nguồn" className="flex flex-wrap gap-1.5">
            {TABS.map((tab) => (
              <button
                key={tab.key}
                type="button"
                role="tab"
                aria-selected={source === tab.key}
                onClick={() => {
                  setSource(tab.key);
                  setPage(0);
                }}
                className={cx(
                  "rounded-full border px-3 py-1 text-xs font-semibold",
                  source === tab.key ? "border-primary/30 bg-primary-soft text-primary-ink" : "border-line-strong bg-surface text-body hover:bg-shell",
                )}
              >
                {tab.label}
              </button>
            ))}
          </div>
          <div className="flex gap-2">
            <Field label="Từ ngày" htmlFor="usage-from">
              <TextInput id="usage-from" type="date" value={from} onChange={(e) => { setFrom(e.target.value); setPage(0); }} />
            </Field>
            <Field label="Đến ngày" htmlFor="usage-to" error={rangeInvalid ? "Đến ngày phải sau Từ ngày" : null}>
              <TextInput id="usage-to" type="date" value={to} invalid={rangeInvalid} onChange={(e) => { setTo(e.target.value); setPage(0); }} />
            </Field>
          </div>
        </div>
        {error && (
          <Alert tone="danger" role="alert">
            {error}
          </Alert>
        )}
        {data && data.items.length === 0 && <p className="text-[13px] text-muted">Chưa có lượt sử dụng nào trong khoảng này.</p>}
        {data && data.items.length > 0 && (
          <TableFrame>
            <thead>
              <tr>
                <Th>Thời gian</Th>
                <Th>Tác vụ</Th>
                <Th>Nguồn · mô hình</Th>
                <Th className="text-right">Token vào / ra</Th>
                <Th>Nguồn số liệu</Th>
                <Th>Trạng thái</Th>
              </tr>
            </thead>
            <tbody>
              {data.items.map((item) => {
                const outcome = OUTCOME_LABEL[item.outcome] ?? { text: item.outcome, tone: "neutral" as const };
                const unknown = item.inputTokens === null && item.outputTokens === null;
                return (
                  <tr key={item.id}>
                    <Td className="whitespace-nowrap">{formatShort(item.createdAt)}</Td>
                    <Td>{item.toolName ? `Tool: ${item.toolName}` : item.taskType ? (TASK_LABEL[item.taskType] ?? item.taskType) : "—"}</Td>
                    <Td>
                      {SOURCE_LABEL[item.source]}
                      {item.provider && ` · ${PROVIDER_LABEL[item.provider]}`}
                      {item.model && <span className="block text-[11px] text-muted">{item.model}</span>}
                    </Td>
                    <Td className="text-right whitespace-nowrap">
                      {unknown ? (
                        <span className="text-muted">Không có số liệu</span>
                      ) : (
                        `${formatMaybeTokens(item.inputTokens)} / ${formatMaybeTokens(item.outputTokens)}`
                      )}
                    </Td>
                    <Td>{PROVENANCE_LABEL[item.provenance]}</Td>
                    <Td>
                      <Pill tone={outcome.tone}>{outcome.text}</Pill>
                    </Td>
                  </tr>
                );
              })}
            </tbody>
          </TableFrame>
        )}
        {data && data.total > 0 && (
          <Pagination page={page} pageSize={PAGE_SIZE} total={data.total} onPageChange={setPage} itemLabel="lượt" />
        )}
        <p className="text-[11px] text-muted">Lịch sử không lưu tên bài, tên sinh viên hay nội dung gửi cho AI.</p>
      </Section>
    </div>
  );
}

function SummaryCard({ icon, title, children }: { icon: "speed" | "key" | "smart_toy"; title: string; children: ReactNode }) {
  return (
    <div className="flex items-start gap-3 rounded-xl border border-line bg-surface p-4">
      <IconTile icon={icon} tone={icon === "smart_toy" ? "accent" : "primary"} />
      <div className="min-w-0">
        <p className="text-xs font-semibold text-muted">{title}</p>
        <p className="font-display text-[28px] leading-none text-ink">{children}</p>
      </div>
    </div>
  );
}
