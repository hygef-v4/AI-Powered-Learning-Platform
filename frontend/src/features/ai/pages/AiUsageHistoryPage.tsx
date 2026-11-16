"use client";

import { useUrlState } from "@/shared/navigation/useUrlState";
import { useEffect, useRef, useState } from "react";
import { PROVIDER_LABEL, formatDateTime, formatMaybeTokens, formatTokens, type SourceOption } from "@/shared/ai";
import { Alert, cx, DateInput, Pagination, Pill, Segmented, SettingsCard, SettingsGroup, TableFrame, Td, Th } from "@/shared/ui";
import { getSources, getUsage } from "../api";
import { OUTCOME_LABEL, SOURCE_LABEL, TASK_LABEL } from "../model/labels";
import type { UsagePage, UsageQuery, UsageSource } from "../model/types";
import { CountCard, TokenCard } from "../components/UsageCards";
import { msg } from "@/i18n/msg";

export const PAGE_SIZE = 50;
const TABS: { key: UsageSource | ""; label: string }[] = [
  { key: "", label: msg("ai.pages.AiUsageHistoryPage.all") },
  { key: "SHARED", label: SOURCE_LABEL.SHARED },
  { key: "PERSONAL", label: msg("ai.pages.AiUsageHistoryPage.personalApi") },
  { key: "MCP", label: "Agent MCP" },
];

/** Ngày "yyyy-mm-dd" (giờ Việt Nam) → đầu ngày hoặc đầu ngày hôm sau, dạng ISO. */
function toDayBound(day: string, end: boolean): string | undefined {
  if (!day) return undefined;
  const start = new Date(`${day}T00:00:00+07:00`);
  if (end) start.setUTCDate(start.getUTCDate() + 1);
  return start.toISOString();
}

/**
 * Tab Usage của trang Kết nối AI (UC 33), cùng kiểu tab Usage của Cài đặt AI: thẻ số liệu ở trên, một thẻ lịch sử có
 * tab nguồn bên trái, khoảng ngày bên phải, bảng tiêu đề tô nền. Chỉ bản ghi của chính mình; token không có số hiện
 * "Không có số liệu".
 */
export function AiUsageHistoryPage({ mcpEnabled = true }: { mcpEnabled?: boolean }) {
  const [chosenSource, setSource] = useUrlState<UsageSource | "">("source", "", ["", "SHARED", "PERSONAL", "MCP"]);
  const source = chosenSource === "MCP" && !mcpEnabled ? "" : chosenSource;
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState<UsagePage | null>(null);
  const [shared, setShared] = useState<SourceOption | null>(null);
  const [error, setError] = useState<string | null>(null);
  const request = useRef(0);

  const rangeInvalid = from !== "" && to !== "" && from > to;
  const tabs = TABS.filter((tab) => (tab.key !== "MCP" || mcpEnabled) && (tab.key !== "SHARED" || shared !== null));

  useEffect(() => {
    if (rangeInvalid) return;
    const id = ++request.current;
    const query: UsageQuery = {
      source: source || undefined,
      from: toDayBound(from, false),
      to: toDayBound(to, true),
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
        if (id === request.current) setError(msg("ai.pages.AiUsageHistoryPage.couldNotLoadAiUsage"));
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
    <>
      <div className={cx("grid gap-3", shared?.quota ? "md:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]" : "md:grid-cols-2")}>
        {shared?.quota && (
          <TokenCard title={msg("ai.pages.AiUsageHistoryPage.systemApi")} className={cx("justify-between", mcpEnabled && "md:row-span-2")}>
            <p className="break-words">
              <span className="text-[100px] leading-none font-bold tracking-[-0.03em] tabular-nums md:text-[120px]">{formatTokens(shared.quota.used)}</span>
              <span className="text-[35px] font-semibold opacity-85"> / {formatTokens(shared.quota.limit)} token</span>
            </p>
          </TokenCard>
        )}
        {/* Không có thẻ API hệ thống (sinh viên): API cá nhân là thẻ chính, màu cam. */}
        <CountCard title={msg("ai.pages.AiUsageHistoryPage.personalApi")} accent={!shared?.quota} value={data ? formatTokens(data.personal.requests) : "—"} />
        {mcpEnabled && <CountCard title="Agent MCP" value={data ? formatTokens(data.mcp.requests) : "—"} />}
      </div>
      <SettingsCard>
        <SettingsGroup title={msg("ai.pages.AiUsageHistoryPage.usageHistory")}>
          <div className="flex flex-wrap items-center justify-between gap-x-3 gap-y-6">
            <Segmented
              as="tab"
              ariaLabel={msg("ai.pages.AiUsageHistoryPage.source")}
              value={source}
              onChange={(value) => {
                setSource(value);
                setPage(0);
              }}
              options={tabs.map((tab) => ({ value: tab.key, label: tab.label }))}
            />
            {/* Nhãn ngày nằm ngay trên ô, tràn lên dòng tiêu đề nên hàng tab không bị đẩy xuống. */}
            <div className="flex items-center gap-2">
              <DateBox id="usage-from" label={msg("ai.pages.AiUsageHistoryPage.from")} value={from} onChange={(value) => { setFrom(value); setPage(0); }} />
              <span aria-hidden="true" className="text-muted">–</span>
              <DateBox id="usage-to" label={msg("ai.pages.AiUsageHistoryPage.to")} value={to} invalid={rangeInvalid} onChange={(value) => { setTo(value); setPage(0); }} />
            </div>
          </div>
          {rangeInvalid && <p role="alert" className="mt-2 text-right text-xs text-danger">{msg("ai.pages.AiUsageHistoryPage.toMustBeAfterFrom")}</p>}
          {error && (
            <Alert tone="danger" role="alert" className="mt-3">
              {error}
            </Alert>
          )}
          {!data && !error && <p className="py-3 text-[13px] text-muted">{msg("ai.pages.AiUsageHistoryPage.loading")}</p>}
          {data && (
            <TableFrame filledHead className="mt-3 [&_table]:min-w-[820px] [&_table]:table-fixed">
              <colgroup><col className="w-[150px]" /><col /><col className="w-[130px]" /><col className="w-[170px]" /><col className="w-[140px]" /><col className="w-[120px]" /></colgroup><thead>
                <tr>
                  <Th>{msg("ai.pages.AiUsageHistoryPage.time")}</Th>
                  <Th>{msg("ai.pages.AiUsageHistoryPage.task")}</Th>
                  <Th>{msg("ai.pages.AiUsageHistoryPage.source")}</Th>
                  <Th>{msg("ai.pages.AiUsageHistoryPage.model")}</Th>
                  <Th className="text-right">{msg("ai.pages.AiUsageHistoryPage.tokensInOut")}</Th>
                  <Th>{msg("ai.pages.AiUsageHistoryPage.status")}</Th>
                </tr>
              </thead>
              <tbody>
                {data.items.length === 0 && (
                  <tr>
                    <Td colSpan={6} className="py-6 text-center! text-muted">
                      {msg("ai.pages.AiUsageHistoryPage.noUsageInThisPeriod")}</Td>
                  </tr>
                )}
                {data.items.map((item) => {
                  const outcome = OUTCOME_LABEL[item.outcome] ?? { text: item.outcome, tone: "neutral" as const };
                  const unknown = item.inputTokens === null && item.outputTokens === null;
                  return (
                    <tr key={item.id}>
                      <Td className="whitespace-nowrap">{formatDateTime(item.createdAt)}</Td>
                      <Td>{item.toolName ? `Tool: ${item.toolName}` : item.taskType ? (TASK_LABEL[item.taskType] ?? item.taskType) : "—"}</Td>
                      <Td className="whitespace-nowrap">{SOURCE_LABEL[item.source]}</Td>
                      <Td className="whitespace-nowrap">{item.model ?? (item.provider ? PROVIDER_LABEL[item.provider] : "—")}</Td>
                      <Td className="text-right whitespace-nowrap">
                        {unknown ? (
                          <span className="text-muted">{msg("ai.pages.AiUsageHistoryPage.noData")}</span>
                        ) : (
                          `${formatMaybeTokens(item.inputTokens)} / ${formatMaybeTokens(item.outputTokens)}`
                        )}
                      </Td>
                      <Td>
                        <Pill tone={outcome.tone}>{outcome.text}</Pill>
                      </Td>
                    </tr>
                  );
                })}
              </tbody>
            </TableFrame>
          )}
          {data && (
            <div className="pt-3">
              <Pagination page={page} pageSize={PAGE_SIZE} total={data.total} onPageChange={setPage} summary={false} />
            </div>
          )}
        </SettingsGroup>
      </SettingsCard>
    </>
  );
}

/** Ô ngày có nhãn đặt ngay trên ô, không chiếm chiều cao của hàng (tràn lên dòng tiêu đề khi chật). */
function DateBox({ id, label, value, invalid = false, onChange }: { id: string; label: string; value: string; invalid?: boolean; onChange: (value: string) => void }) {
  return (
    <div className="relative">
      <label htmlFor={id} className="absolute bottom-full left-0 mb-1 text-xs font-semibold whitespace-nowrap text-body">
        {label}
      </label>
      <DateInput id={id} value={value} invalid={invalid} onChange={onChange} />
    </div>
  );
}
