"use client";

import { useId, useState } from "react";
import { PROVIDER_LABEL, PROVIDERS, type AiProvider } from "@/shared/ai";
import { ApiError } from "@/shared/api/client";
import { Button, Dialog, Field, IconButton, Select } from "@/shared/ui";
import { PROVIDER_CONSOLE, errorMessage } from "./labels";
import type { ConnectionView, ProviderInfo, SaveConnection } from "./types";

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
  /** Lấy khóa đã lưu khi sửa (nút mắt, sao chép); không truyền thì chỉ xem, sao chép được khóa vừa nhập. */
  onReveal?: () => Promise<string>;
};

/**
 * Popup thêm hoặc sửa khóa API (c12a): khóa chỉ nhập, không hiện lại; sau khi lưu chỉ còn 4 ký tự cuối. Khi sửa, để
 * trống ô khóa là giữ khóa đang lưu (ô hiện 4 ký tự cuối làm gợi ý).
 */
export function ApiKeyDialog({ current, initialProvider, providers, taken = [], onClose, onSave, onTest, onReveal }: Props) {
  const ids = { provider: useId(), key: useId(), model: useId() };
  const [provider, setProvider] = useState<AiProvider>(initialProvider);
  const models = providers.find((p) => p.provider === provider)?.models ?? [];
  const [model, setModel] = useState(current?.model ?? models[0] ?? "");
  const [apiKey, setApiKey] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [test, setTest] = useState<KeyTestResult | null>(null);
  const [shown, setShown] = useState(false);
  const [saved, setSaved] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const editing = current?.configured === true;
  const canReveal = editing && onReveal !== undefined;

  /** Khóa đã lưu, tải một lần rồi giữ lại (để trống hoặc giữ nguyên là không đổi khóa). */
  const reveal = async (): Promise<string | null> => {
    if (saved !== null) return saved;
    if (!onReveal) return null;
    try {
      const key = await onReveal();
      setSaved(key);
      return key;
    } catch {
      setError("Không xem được khóa đã lưu, thử lại sau.");
      return null;
    }
  };

  // Ẩn lại khóa đã lưu thì về ô trống với gợi ý 4 ký tự cuối, không để một dãy chấm dài; khóa đang gõ thì chỉ che đi.
  const toggleShown = async () => {
    if (shown) {
      setShown(false);
      if (saved !== null && apiKey === saved) setApiKey("");
      return;
    }
    if (!apiKey && canReveal) {
      const key = await reveal();
      if (key === null) return;
      setApiKey(key);
    }
    setShown(true);
  };

  const copyKey = async () => {
    const text = apiKey || (canReveal ? await reveal() : null);
    if (!text) return;
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
    } catch {
      setCopied(false);
    }
  };

  const changeProvider = (next: AiProvider) => {
    setProvider(next);
    setModel(providers.find((p) => p.provider === next)?.models[0] ?? "");
    setTest(null);
  };

  const body = (): SaveConnection => ({
    apiKey: apiKey.trim() && apiKey.trim() !== saved ? apiKey.trim() : undefined,
    model,
    version: editing ? current?.version : undefined,
  });

  const save = async () => {
    if (!editing && !apiKey.trim()) {
      setError("Hãy nhập khóa API.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await onSave(provider, body());
      setApiKey("");
    } catch (e) {
      const problem = e instanceof ApiError ? e.problem : undefined;
      setError(errorMessage(problem?.code, problem?.detail ?? "Không lưu được khóa, thử lại sau."));
    } finally {
      setBusy(false);
    }
  };

  const runTest = async () => {
    if (!onTest) return;
    if (!editing && !apiKey.trim()) {
      setError("Hãy nhập khóa API.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      setTest(await onTest(provider, body()));
    } catch (e) {
      const problem = e instanceof ApiError ? e.problem : undefined;
      setTest({ ok: false, text: errorMessage(problem?.code, problem?.detail ?? "Không kiểm tra được") });
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog
      open
      title={editing ? "Sửa khóa API" : "Thêm khóa API"}
      icon="key"
      size="lg"
      onClose={onClose}
      dismissible={!busy}
      footer={
        <Button variant="primary" onClick={() => void save()} disabled={busy || !model}>
          Lưu khóa
        </Button>
      }
    >
      <Field label="Nhà cung cấp" htmlFor={ids.provider} required>
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
      <Field label="Khóa API" htmlFor={ids.key} required={!editing} error={error}>
        <div className="flex h-11 items-center gap-1 rounded-xl border border-primary/40 bg-primary-soft py-1 pr-1 pl-3 focus-within:border-focus">
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
            placeholder={editing ? `••••••••${current?.keyHint ?? ""}` : "Dán khóa API"}
            className="min-w-0 flex-1 bg-transparent font-mono text-xs text-ink outline-none placeholder:text-muted"
          />
          <IconButton
            icon={shown ? "visibility_off" : "visibility"}
            label={shown ? "Ẩn khóa" : "Xem khóa"}
            disabled={busy || (!apiKey && !canReveal)}
            onClick={() => void toggleShown()}
          />
          <IconButton
            icon={copied ? "check" : "copy"}
            label={copied ? "Đã sao chép" : "Sao chép khóa"}
            disabled={busy || (!apiKey && !canReveal)}
            onClick={() => void copyKey()}
          />
        </div>
      </Field>
      <Field label="Mô hình" htmlFor={ids.model} required>
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
              Kiểm tra
            </Button>
            {test && (
              <span
                role="status"
                className={
                  test.ok
                    ? "rounded-md bg-success-soft px-2 py-0.5 text-[13px] font-medium text-success"
                    : "rounded-md bg-danger-soft px-2 py-0.5 text-[13px] font-medium text-danger"
                }
              >
                {test.text}
              </span>
            )}
          </div>
          <p className="text-[11px] text-muted">Kiểm tra có thể tính phí nhỏ.</p>
        </div>
      )}
    </Dialog>
  );
}
