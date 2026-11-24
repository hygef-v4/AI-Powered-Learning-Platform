"use client";

import { useEffect, useState } from "react";
import {
  PROVIDER_LABEL, formatTokens, masked, saveSourcePreference, type SourceOption, type SourcePreference,
} from "@/shared/ai";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Pill, SettingsGroup, cx } from "@/shared/ui";
import { getSources } from "../api";
import { describeErrorCode } from "../model/labels";
import { msg } from "@/i18n/msg";

const AUTO = "AUTO";

type Choice = { key: string; preference: SourcePreference; title: string; detail: string; reason: string | null;
  disabled: boolean; costBearer: "SCHOOL" | "SELF" | null };

function buildKey(source: SourcePreference["source"], provider: SourcePreference["provider"]) {
  return source === null ? AUTO : `${source}:${provider ?? ""}`;
}

/** Các lựa chọn: Tự động, API của trường (nếu được dùng) và mỗi nhà cung cấp đã có khóa cá nhân đang bật. */
function buildChoices(options: SourceOption[]): Choice[] {
  const choices: Choice[] = [{
    key: AUTO,
    preference: { source: null, provider: null },
    title: msg("ai.components.SourceSection.automatic"),
    detail: options.some((o) => o.source === "SHARED")
      ? msg("ai.components.SourceSection.automaticDetailShared")
      : msg("ai.components.SourceSection.automaticDetailPersonal"),
    reason: null,
    disabled: false,
    costBearer: null,
  }];
  for (const option of options) {
    const shared = option.source === "SHARED";
    if (!shared && !option.available && !option.preferred) continue;
    const quota = shared && option.quota
      ? msg("ai.components.SourceSection.tokensLeftToday", {
        left: formatTokens(option.quota.left), limit: formatTokens(option.quota.limit),
      })
      : "";
    choices.push({
      key: buildKey(option.source, shared ? null : option.provider),
      preference: { source: option.source, provider: shared ? null : option.provider },
      title: shared
        ? msg("ai.components.SourceSection.schoolApi")
        : msg("ai.components.SourceSection.personalKey", { provider: option.provider ? PROVIDER_LABEL[option.provider] : "" }),
      detail: [option.model, !shared && option.keyHint ? masked(option.keyHint) : "", quota].filter(Boolean).join(" · "),
      reason: option.available ? null : option.reason,
      disabled: !option.available && !option.preferred,
      costBearer: option.costBearer,
    });
  }
  return choices;
}

/**
 * Thẻ Nguồn AI dùng mặc định trên tab API cá nhân (FB-U06-01, yêu cầu 2026-11-23): chọn một nguồn cho mọi tác vụ AI
 * thay cho công tắc từng khóa. Chọn nguồn cụ thể thì tác vụ chỉ chạy bằng nguồn đó, lỗi thì báo chứ không đổi nguồn;
 * Tự động giữ thứ tự API của trường rồi khóa cá nhân (BR-U06-10).
 */
export function SourceSection({ refreshKey }: { refreshKey: number }) {
  const [options, setOptions] = useState<SourceOption[] | null>(null);
  const [selected, setSelected] = useState<string>(AUTO);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    let active = true;
    getSources().then(
      (loaded) => {
        if (!active) return;
        setOptions(loaded);
        setError(null);
        const preferred = loaded.find((o) => o.preferred);
        setSelected(preferred ? buildKey(preferred.source, preferred.source === "SHARED" ? null : preferred.provider) : AUTO);
      },
      () => {
        if (active) setError(msg("ai.components.SourceSection.couldNotLoadSources"));
      },
    );
    return () => {
      active = false;
    };
  }, [refreshKey]);

  const choices = options ? buildChoices(options) : [];

  async function handleSelect(choice: Choice) {
    if (choice.disabled || saving || choice.key === selected) return;
    const previous = selected;
    setSelected(choice.key);
    setSaving(true);
    setSaved(false);
    setError(null);
    try {
      await saveSourcePreference(choice.preference);
      setSaved(true);
    } catch (e) {
      setSelected(previous);
      const problem = e instanceof ApiError ? e.problem : undefined;
      setError(describeErrorCode(problem?.code, msg("ai.components.SourceSection.couldNotSaveSource")));
    } finally {
      setSaving(false);
    }
  }

  return (
    <SettingsGroup title={msg("ai.components.SourceSection.title")}>
      <p className="pb-3 text-[13px] text-muted">{msg("ai.components.SourceSection.intro")}</p>
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      {!options && !error && <p className="py-2 text-[13px] text-muted">{msg("ai.components.SourceSection.loading")}</p>}
      {options && (
        <div role="radiogroup" aria-label={msg("ai.components.SourceSection.title")} className="flex flex-col gap-1.5">
          {choices.map((choice) => {
            const checked = selected === choice.key;
            return (
              <label key={choice.key} data-testid={`ai-source-${choice.key}`}
                className={cx("group flex items-start gap-3 rounded-xl border px-4 py-3 transition-[background-color,border-color,box-shadow] duration-200",
                  checked ? "border-primary bg-primary-soft shadow-[0_0_0_3px_var(--c-primary-soft)]" : "border-line hover:border-line-strong hover:bg-sunken",
                  choice.disabled ? "cursor-not-allowed opacity-55 hover:border-line hover:bg-transparent" : "cursor-pointer")}>
                <input type="radio" name="ai-source" value={choice.key} checked={checked} disabled={choice.disabled || saving}
                  onChange={() => void handleSelect(choice)} className="peer sr-only" />
                <span aria-hidden
                  className={cx("mt-0.5 flex size-[18px] shrink-0 items-center justify-center rounded-full border-2 transition-colors duration-200 peer-focus-visible:ring-2 peer-focus-visible:ring-focus peer-focus-visible:ring-offset-2",
                    checked ? "border-primary" : "border-line-strong group-hover:border-muted")}>
                  <span className={cx("size-2 rounded-full bg-primary transition-transform duration-200 ease-[cubic-bezier(0.16,1,0.3,1)]",
                    checked ? "scale-100" : "scale-0")} />
                </span>
                <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                  <span className="flex flex-wrap items-center gap-2">
                    <span className={cx("text-[14px] text-ink", checked ? "font-semibold" : "font-medium")}>{choice.title}</span>
                    {choice.costBearer && (
                      <Pill tone={choice.costBearer === "SCHOOL" ? "primary" : "accent"}>
                        {choice.costBearer === "SCHOOL" ? msg("ai.components.SourceSection.paidBySchool") : msg("ai.components.SourceSection.paidByYou")}
                      </Pill>
                    )}
                  </span>
                  {choice.detail && <span className="text-xs text-muted">{choice.detail}</span>}
                  {choice.reason && <span className="text-xs text-warning">{choice.reason}</span>}
                </span>
              </label>
            );
          })}
        </div>
      )}
      <p className="pt-2 text-xs text-muted" role="status" aria-live="polite">
        {saving ? msg("ai.components.SourceSection.saving") : saved ? msg("ai.components.SourceSection.saved") : msg("ai.components.SourceSection.note")}
      </p>
      {options && options.every((o) => o.source === "SHARED" || !o.available) && (
        <p className="pt-1 text-xs text-muted">{msg("ai.components.SourceSection.addKeyHint")}</p>
      )}
      {error && !options && (
        <Button size="sm" className="mt-2 self-start" onClick={() => window.location.reload()}>
          {msg("ai.components.SourceSection.reload")}</Button>
      )}
    </SettingsGroup>
  );
}
