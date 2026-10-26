"use client";

import { useEffect, useState, type ReactNode } from "react";
import { PROVIDER_LABEL, formatTokens, type AiProvider } from "@/shared/ai";
import { Alert, IconTile, Section, TableFrame, Td, Th, cx } from "@/shared/ui";
import { getUsageSummary, type UsageSummary } from "./api";

function dayLabel(day: string) {
  const [, month, date] = day.split("-");
  return `${date}/${month}`;
}

/**
 * Usage API của trường (BR-U06-41): hôm nay trên trần, tổng kỳ, số giảng viên đã dùng, theo ngày, giảng viên, mô hình.
 * Không có nội dung, khóa, usage API cá nhân hay MCP.
 */
export function SharedUsageTab() {
  const [days, setDays] = useState<7 | 30>(7);
  const [data, setData] = useState<UsageSummary | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    getUsageSummary(days).then(
      (loaded) => {
        if (!active) return;
        setData(loaded);
        setError(null);
      },
      () => {
        if (active) setError("Không tải được usage API của trường, thử lại sau.");
      },
    );
    return () => {
      active = false;
    };
  }, [days]);

  const max = Math.max(1, ...(data?.byDay.map((d) => d.tokens) ?? [0]));

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-xs text-muted">Chỉ tính yêu cầu dùng API của trường.</p>
        <div role="radiogroup" aria-label="Khoảng thời gian" className="flex gap-1.5">
          {([7, 30] as const).map((d) => (
            <button
              key={d}
              type="button"
              role="radio"
              aria-checked={days === d}
              onClick={() => setDays(d)}
              className={cx(
                "rounded-full border px-3 py-1 text-xs font-semibold",
                days === d ? "border-primary/30 bg-primary-soft text-primary-ink" : "border-line-strong bg-surface text-body hover:bg-shell",
              )}
            >
              {d} ngày
            </button>
          ))}
        </div>
      </div>
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
      {data && (
        <>
          <div className="grid gap-3 md:grid-cols-3">
            <Stat icon="speed" title="Hôm nay">
              {formatTokens(data.todayTokens)}
              {data.globalDayTokens !== null && <span className="text-sm font-normal text-muted"> / {formatTokens(data.globalDayTokens)}</span>}
            </Stat>
            <Stat icon="monitoring" title={`Tổng ${data.days} ngày`}>
              {formatTokens(data.periodTokens)} <span className="text-sm font-normal text-muted">token</span>
            </Stat>
            <Stat icon="groups" title="Giảng viên đã dùng">
              {data.teachersUsed}
            </Stat>
          </div>
          <Section title="Token theo ngày">
            <div className="flex h-40 items-end gap-1" role="img" aria-label={`Token theo ngày trong ${data.days} ngày`}>
              {data.byDay.map((d) => (
                <div key={d.day} className="flex min-w-0 flex-1 flex-col items-center gap-1" title={`${dayLabel(d.day)}: ${formatTokens(d.tokens)} token`}>
                  <div className="w-full rounded-t-[4px] bg-primary" style={{ height: `${Math.max(d.tokens > 0 ? 2 : 0, (d.tokens / max) * 128)}px` }} />
                  {(data.days === 7 || d.day.endsWith("01") || d === data.byDay.at(-1)) && (
                    <span className="text-[10px] text-muted">{dayLabel(d.day)}</span>
                  )}
                </div>
              ))}
            </div>
          </Section>
          <Section title="Theo giảng viên">
            {data.byTeacher.length === 0 ? (
              <p className="text-[13px] text-muted">Chưa giảng viên nào dùng API của trường trong kỳ.</p>
            ) : (
              <TableFrame>
                <thead>
                  <tr>
                    <Th>Giảng viên</Th>
                    <Th className="text-right">Token trong kỳ</Th>
                    <Th className="text-right">Hôm nay / hạn mức ngày</Th>
                  </tr>
                </thead>
                <tbody>
                  {data.byTeacher.map((t) => (
                    <tr key={t.accountId}>
                      <Td>
                        <p className="font-semibold">{t.name}</p>
                        <p className="text-[11px] text-muted">{t.email}</p>
                      </Td>
                      <Td className="text-right">{formatTokens(t.periodTokens)}</Td>
                      <Td className="text-right whitespace-nowrap">
                        {formatTokens(t.todayTokens)} / {t.dayLimit === null ? "—" : t.dayLimit === 0 ? "chặn" : formatTokens(t.dayLimit)}
                      </Td>
                    </tr>
                  ))}
                </tbody>
              </TableFrame>
            )}
          </Section>
          <Section title="Theo nhà cung cấp và mô hình">
            {data.byModel.length === 0 ? (
              <p className="text-[13px] text-muted">Chưa có yêu cầu nào trong kỳ.</p>
            ) : (
              <TableFrame>
                <thead>
                  <tr>
                    <Th>Nhà cung cấp</Th>
                    <Th>Mô hình</Th>
                    <Th className="text-right">Yêu cầu</Th>
                    <Th className="text-right">Token</Th>
                  </tr>
                </thead>
                <tbody>
                  {data.byModel.map((m) => (
                    <tr key={`${m.provider}-${m.model}`}>
                      <Td>{PROVIDER_LABEL[m.provider as AiProvider] ?? m.provider}</Td>
                      <Td>{m.model}</Td>
                      <Td className="text-right">{formatTokens(m.requests)}</Td>
                      <Td className="text-right">{formatTokens(m.tokens)}</Td>
                    </tr>
                  ))}
                </tbody>
              </TableFrame>
            )}
          </Section>
        </>
      )}
    </div>
  );
}

function Stat({ icon, title, children }: { icon: "speed" | "monitoring" | "groups"; title: string; children: ReactNode }) {
  return (
    <div className="flex items-start gap-3 rounded-xl border border-line bg-surface p-4">
      <IconTile icon={icon} />
      <div className="min-w-0">
        <p className="text-xs font-semibold text-muted">{title}</p>
        <p className="font-display text-[30px] leading-none text-ink">{children}</p>
      </div>
    </div>
  );
}
