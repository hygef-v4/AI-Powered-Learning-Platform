"use client";

import { useId, useState } from "react";
import { PROVIDER_LABEL, PROVIDERS, type AiProvider } from "@/shared/ai";
import { ApiError } from "@/shared/api/client";
import { Button, Dialog, Field, IconButton, Select, cx } from "@/shared/ui";
import { PROVIDER_CONSOLE, defaultModel, describeErrorCode, shortenTestResult } from "../model/labels";
import type { ConnectionView, ProviderInfo, SaveConnection } from "../model/types";
import { msg } from "@/i18n/msg";

export type KeyTestResult = { ok: boolean; text: string };

type Props = {
  /** Kết nối đang sửa; rỗng là thêm mới. Thêm và sửa dùng chung popup, chỉ khác tiêu đề. */
  current: ConnectionView | null;
  initialProvider: AiProvider;
  providers: ProviderInfo[];
  onClose: () => void;
  onSave: (provider: AiProvider, body: SaveConnection) => Promise<void>;
  /** Nhà cung cấp đã có khóa: thêm mới không chọn được (API cá nhân mỗi nhà cung cấp một khóa). */
  taken?: AiProvider[];
  /** Kiểm tra với khóa, mô hình đang nhập; không truyền thì không có nút Kiểm tra. */
  onTest?: (provider: AiProvider, body: SaveConnection) => Promise<KeyTestResult>;
};

/**
 * Popup thêm hoặc sửa khóa API (c12a): khóa chỉ ghi, đã lưu thì không tải lại về trình duyệt (2026-11-11); sau khi lưu
 * chỉ còn 4 ký tự cuối. Khi sửa, để trống ô khóa là giữ khóa đang lưu (ô hiện 4 ký tự cuối làm gợi ý). Nút mắt và sao
 * chép chỉ dùng cho khóa đang gõ.
 */
export function ApiKeyDialog({ current, initialProvider, providers, taken = [], onClose, onSave, onTest }: Props) {
  const ids = { provider: useId(), key: useId(), model: useId() };
  const [provider, setProvider] = useState<AiProvider>(initialProvider);
  const models = providers.find((p) => p.provider === provider)?.models ?? [];
  const [model, setModel] = useState(current?.model ?? defaultModel(provider, models));
  const [apiKey, setApiKey] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [test, setTest] = useState<KeyTestResult | null>(null);
  const [shown, setShown] = useState(false);
  const [copied, setCopied] = useState(false);
  const editing = current?.configured === true;

  const copyKey = async () => {
    if (!apiKey) return;
    try {
      await navigator.clipboard.writeText(apiKey);
      setCopied(true);
    } catch {
      setCopied(false);
    }
  };

  const changeProvider = (next: AiProvider) => {
    setProvider(next);
    setModel(defaultModel(next, providers.find((p) => p.provider === next)?.models ?? []));
    setTest(null);
  };

  const buildBody = (): SaveConnection => ({
    apiKey: apiKey.trim() || undefined,
    model,
    version: editing ? current?.version : undefined,
  });

  const saveConnection = async () => {
    if (!editing && !apiKey.trim()) {
      setError(msg("ai.components.ApiKeyDialog.enterAnApiKey"));
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await onSave(provider, buildBody());
      setApiKey("");
    } catch (e) {
      const problem = e instanceof ApiError ? e.problem : undefined;
      setError(describeErrorCode(problem?.code, problem?.detail ?? msg("ai.components.ApiKeyDialog.couldNotSaveTheKey")));
    } finally {
      setBusy(false);
    }
  };

  const runTest = async () => {
    if (!onTest) return;
    if (!editing && !apiKey.trim()) {
      setError(msg("ai.components.ApiKeyDialog.enterAnApiKey"));
      return;
    }
    setBusy(true);
    setError(null);
    try {
      setTest(await onTest(provider, buildBody()));
    } catch (e) {
      const problem = e instanceof ApiError ? e.problem : undefined;
      setTest({ ok: false, text: describeErrorCode(problem?.code, problem?.detail ?? msg("ai.components.ApiKeyDialog.couldNotCheck")) });
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog
      open
      title={editing ? msg("ai.components.ApiKeyDialog.editApiKey") : msg("ai.components.ApiKeyDialog.addApiKey")}
      icon="key"
      size="lg"
      onClose={onClose}
      dismissible={!busy}
      footer={
        <Button variant="primary" onClick={() => void saveConnection()} disabled={busy || !model}>
          {msg("ai.components.ApiKeyDialog.saveKey")}</Button>
      }
    >
      <Field label={msg("ai.components.ApiKeyDialog.provider")} htmlFor={ids.provider} required>
        <Select
          id={ids.provider}
          value={provider}
          disabled={editing || busy}
          onChange={(event) => changeProvider(event.target.value as AiProvider)}
        >
          {PROVIDERS.filter((p) => editing || p === initialProvider || !taken.includes(p)).map((p) => (
            <option key={p} value={p}>
              {PROVIDER_LABEL[p]} — {PROVIDER_CONSOLE[p]}
            </option>
          ))}
        </Select>
      </Field>
      <Field label={msg("ai.components.ApiKeyDialog.apiKey")} htmlFor={ids.key} required={!editing} error={error}>
        <div
          className={cx(
            "flex items-center gap-1 py-1 pr-1 pl-3 focus-within:border-focus",
            // Thêm khóa: ô trắng như các ô nhập khác; Thay khóa: giữ ô cam của khóa đã lưu.
            editing
              ? "h-11 rounded-xl border border-primary/40 bg-primary-soft"
              : "h-10 rounded-lg border border-field bg-surface transition-[border-color,box-shadow] duration-200 focus-within:shadow-[0_0_0_3px_var(--c-primary-soft)] has-[input:disabled]:bg-sunken has-[input:disabled]:opacity-70",
          )}
        >
          <input
            id={ids.key}
            type={shown ? "text" : "password"}
            autoComplete="off"
            spellCheck={false}
            value={apiKey}
            disabled={busy}
            onChange={(event) => {
              setApiKey(event.target.value);
              setTest(null);
              setCopied(false);
            }}
            placeholder={editing ? `••••••••${current?.keyHint ?? ""}` : msg("ai.components.ApiKeyDialog.pasteTheApiKey")}
            className="min-w-0 flex-1 bg-transparent font-mono text-xs text-ink outline-none placeholder:text-muted"
          />
          <IconButton
            icon={shown ? "visibility_off" : "visibility"}
            label={shown ? msg("ai.components.ApiKeyDialog.hideKey") : msg("ai.components.ApiKeyDialog.showKey")}
            disabled={busy || !apiKey}
            onClick={() => setShown((value) => !value)}
          />
          <IconButton
            icon={copied ? "check" : "copy"}
            label={copied ? msg("ai.components.ApiKeyDialog.copied") : msg("ai.components.ApiKeyDialog.copyKey")}
            disabled={busy || !apiKey}
            onClick={() => void copyKey()}
          />
        </div>
      </Field>
      <Field label={msg("ai.components.ApiKeyDialog.model")} htmlFor={ids.model} required>
        <Select
          id={ids.model}
          value={model}
          disabled={busy}
          onChange={(event) => {
            setModel(event.target.value);
            setTest(null);
          }}
        >
          {models.map((m) => (
            <option key={m} value={m}>
              {m}
            </option>
          ))}
        </Select>
      </Field>
      {onTest && (
        <div className="flex flex-col gap-1.5">
          <div className="flex min-h-8 items-center gap-3">
            <Button size="sm" onClick={() => void runTest()} disabled={busy || !model}>
              {msg("ai.components.ApiKeyDialog.check")}</Button>
            {test && (
              <span
                role="status"
                title={test.ok ? undefined : test.text}
                className={
                  test.ok
                    ? "rounded-md bg-success-soft px-2 py-0.5 text-[13px] font-medium text-success"
                    : "rounded-md bg-danger-soft px-2 py-0.5 text-[13px] font-medium text-danger"
                }
              >
                {test.ok ? test.text : shortenTestResult(test.text)}
              </span>
            )}
          </div>
          <p className="text-xs text-muted">{msg("ai.components.ApiKeyDialog.checkingMayIncurASmall")}</p>
        </div>
      )}
    </Dialog>
  );
}
