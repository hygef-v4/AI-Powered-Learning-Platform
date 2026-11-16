"use client";

import { useEffect, useRef, useState } from "react";
import { PROVIDER_LABEL, formatTokens, type AiProvider } from "@/shared/ai";
import { Alert, SettingsCard, SettingsGroup, TableFrame, Td, Th, cx } from "@/shared/ui";
import { getUsageSummary, type UsageSummary } from "../api";
import { Big, CountCard, TokenCard } from "../../components/UsageCards";
import { msg } from "@/i18n/msg";

const COMPACT = new Intl.NumberFormat("vi-VN", { notation: "compact", maximumFractionDigits: 1 });

function formatDayLabel(day: string) {
  const [, month, date] = day.split("-");
  return `${date}/${month}`;
}

/**
 * Usage API của trường (BR-U06-41): hàng trên là hôm nay, hàng dưới là 7 hoặc 30 ngày; mỗi hàng một thẻ cam số token
 * và một thẻ số giảng viên đã dùng. Sau đó là bảng theo giảng viên và theo nhà cung cấp. Không có nội dung, khóa,
 * usage API cá nhân hay MCP.
 */
export function SharedUsageTab() {
  const [days, setDays] = useState<7 | 30>(7);
  const [data, setData] = useState<UsageSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [values, setValues] = useState(false);
  const chartCard = useRef<HTMLDivElement>(null);
  // Đang hiện số thì bấm ra ngoài thẻ biểu đồ để ẩn.
  useEffect(() => {
    if (!values) return;
    const hide = (event: PointerEvent) => {
      if (!chartCard.current?.contains(event.target as Node)) setValues(false);
    };
    document.addEventListener("pointerdown", hide);
    return () => document.removeEventListener("pointerdown", hide);
  }, [values]);

  useEffect(() => {
    let active = true;
    getUsageSummary(days).then(
      (loaded) => {
        if (!active) return;
        setData(loaded);
        setError(null);
      },
      () => {
        if (active) setError(msg("ai.admin.components.SharedUsageTab.khongTaiDuocUsageApiCua"));
      },
    );
    return () => {
      active = false;
    };
  }, [days]);

  if (error) {
    return (
      <Alert tone="danger" role="alert">
        {error}
      </Alert>
    );
  }
  if (!data) return <p className="text-[13px] text-muted">{msg("ai.admin.components.SharedUsageTab.dangTai")}</p>;

  const max = Math.max(1, ...data.byDay.map((d) => d.tokens));
  const teachersToday = data.byTeacher.filter((t) => t.todayTokens > 0).length;

  return (
    <>
      <div className="grid gap-3 md:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
        <TokenCard title={msg("ai.admin.components.SharedUsageTab.homNay")}>
          <Big>{formatTokens(data.todayTokens)}</Big>
          {data.globalDayTokens !== null && <span className="text-sm opacity-85"> / {formatTokens(data.globalDayTokens)} token</span>}
        </TokenCard>
        <CountCard title={msg("ai.admin.components.SharedUsageTab.giangVienDaDungHomNay")} value={teachersToday} />
        {/* Bấm vào thẻ là hiện số trên từng cột. */}
        <div ref={chartCard} className="contents" onClick={() => setValues(true)}>
        <TokenCard
          className="cursor-pointer"
          title={msg("ai.admin.components.SharedUsageTab.ngay", { days: String(data.days) })}
          action={
            <div role="radiogroup" aria-label={msg("ai.admin.components.SharedUsageTab.khoangThoiGian")} className="flex gap-1 rounded-full bg-on-primary/10 p-0.5">
              {([7, 30] as const).map((d) => (
                <button
                  key={d}
                  type="button"
                  role="radio"
                  aria-checked={days === d}
                  onClick={() => setDays(d)}
                  className={cx(
                    "rounded-full px-3 py-1 text-xs font-semibold transition-colors",
                    days === d ? "bg-surface text-ink" : "text-on-primary hover:bg-on-primary/10",
                  )}
                >
                  {d} {msg("ai.admin.components.SharedUsageTab.ngay2")}</button>
              ))}
            </div>
          }
        >
          <div className="flex flex-wrap items-end justify-between gap-4">
            <p>
              <Big>{formatTokens(data.periodTokens)}</Big>
              <span className="text-sm opacity-85"> token</span>
            </p>
            <div
              role="img"
              aria-label={msg("ai.admin.components.SharedUsageTab.tokenTheoNgayTrongNgay", { days: String(data.days) })}
              className="flex h-16 w-full max-w-[320px] items-end gap-[3px]"
            >
              {data.byDay.map((d) => (
                <span key={d.day} title={`${formatDayLabel(d.day)}: ${formatTokens(d.tokens)} token`} className="flex h-full min-w-0 flex-1 flex-col items-center justify-end gap-0.5">
                  {values && d.tokens > 0 && (
                    <span className="text-xs leading-none font-semibold whitespace-nowrap tabular-nums">{COMPACT.format(d.tokens)}</span>
                  )}
                  <span
                    className="w-full rounded-t-[3px] bg-on-primary/60"
                    style={{ height: `${Math.max(d.tokens > 0 ? 4 : 2, (d.tokens / max) * 40)}px` }}
                  />
                </span>
              ))}
            </div>
          </div>
        </TokenCard>
        </div>
        <CountCard title={msg("ai.admin.components.SharedUsageTab.giangVienDaDungTrongNgay", { days: String(data.days) })} value={data.teachersUsed} />
      </div>

      <SettingsCard>
        <SettingsGroup title={msg("ai.admin.components.SharedUsageTab.theoGiangVien")}>
          <TableFrame filledHead className="mt-2 [&_table]:min-w-[640px] [&_table]:table-fixed">
            <colgroup><col /><col className="w-[110px]" /><col className="w-[190px]" /><col className="w-[160px]" /></colgroup><thead>
              <tr>
                <Th>{msg("ai.admin.components.SharedUsageTab.giangVien")}</Th>
                <Th className="text-right">{msg("ai.admin.components.SharedUsageTab.yeuCau")}</Th>
                <Th className="text-right">{msg("ai.admin.components.SharedUsageTab.homNayHanMucNgay")}</Th>
                <Th className="text-right">{msg("ai.admin.components.SharedUsageTab.tokenTrongNgay", { days: String(data.days) })}</Th>
              </tr>
            </thead>
            <tbody>
              {data.byTeacher.length === 0 && (
                <tr>
                  <Td colSpan={4} className="py-6 text-center! text-muted">
                    {msg("ai.admin.components.SharedUsageTab.chuaGiangVienNaoDungTrong")}</Td>
                </tr>
              )}
              {data.byTeacher.map((t) => (
                <tr key={t.accountId}>
                  <Td>
                    <p className="font-semibold">{t.name}</p>
                    <p className="text-xs text-muted">{t.email}</p>
                  </Td>
                  <Td className="text-right tabular-nums">{formatTokens(t.requests)}</Td>
                  <Td className="text-right whitespace-nowrap tabular-nums">
                    {formatTokens(t.todayTokens)} / {t.dayLimit === null ? "—" : t.dayLimit === 0 ? msg("ai.admin.components.SharedUsageTab.chan") : formatTokens(t.dayLimit)}
                  </Td>
                  <Td className="text-right tabular-nums">{formatTokens(t.periodTokens)}</Td>
                </tr>
              ))}
            </tbody>
          </TableFrame>
        </SettingsGroup>
        <SettingsGroup title={msg("ai.admin.components.SharedUsageTab.theoNhaCungCap")}>
          <TableFrame filledHead className="mt-2 [&_table]:min-w-[600px] [&_table]:table-fixed">
            <colgroup><col className="w-[160px]" /><col /><col className="w-[130px]" /><col className="w-[160px]" /></colgroup><thead>
              <tr>
                <Th>{msg("ai.admin.components.SharedUsageTab.nhaCungCap")}</Th>
                <Th>{msg("ai.admin.components.SharedUsageTab.moHinh")}</Th>
                <Th className="text-right">{msg("ai.admin.components.SharedUsageTab.homNay")}</Th>
                <Th className="text-right">{msg("ai.admin.components.SharedUsageTab.tokenTrongNgay", { days: String(data.days) })}</Th>
              </tr>
            </thead>
            <tbody>
              {data.byModel.length === 0 && (
                <tr>
                  <Td colSpan={4} className="py-6 text-center! text-muted">
                    {msg("ai.admin.components.SharedUsageTab.chuaCoYeuCauTrongKy")}</Td>
                </tr>
              )}
              {data.byModel.map((m) => (
                <tr key={`${m.provider}-${m.model}`}>
                  <Td>{PROVIDER_LABEL[m.provider as AiProvider] ?? m.provider}</Td>
                  <Td>{m.model}</Td>
                  <Td className="text-right tabular-nums">{formatTokens(m.todayTokens ?? 0)}</Td>
                  <Td className="text-right tabular-nums">{formatTokens(m.tokens)}</Td>
                </tr>
              ))}
            </tbody>
          </TableFrame>
        </SettingsGroup>
      </SettingsCard>
    </>
  );
}
