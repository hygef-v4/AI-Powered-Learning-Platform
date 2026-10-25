"use client";

import { useId, useState } from "react";
import { PROVIDER_LABEL, PROVIDERS, type AiProvider } from "@/shared/ai";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Dialog, Field, Select, TextInput } from "@/shared/ui";
import { PROVIDER_CONSOLE, errorMessage } from "./labels";
import type { ConnectionView, ProviderInfo, SaveConnection } from "./types";

type Props = {
  /** Kết nối đang thay; rỗng là thêm mới. */
  current: ConnectionView | null;
  initialProvider: AiProvider;
  providers: ProviderInfo[];
  onClose: () => void;
  onSave: (provider: AiProvider, body: SaveConnection) => Promise<void>;
  /** Nhà cung cấp đã có khóa: thêm mới không chọn được (mỗi nhà cung cấp một khóa, dùng Thay khóa). */
  taken?: AiProvider[];
  /** Kiểm tra kết nối đã lưu (chỉ có khi đang thay khóa đã lưu). */
  onTest?: (provider: AiProvider) => Promise<ConnectionView>;
};

/**
 * Popup Thêm khóa API (c12a): khóa chỉ nhập, không hiện lại; sau khi lưu chỉ còn 4 ký tự cuối. Đổi mô hình của khóa
 * đã lưu không cần nhập lại khóa.
 */
export function ApiKeyDialog({ current, initialProvider, providers, taken = [], onClose, onSave, onTest }: Props) {
  const ids = { provider: useId(), key: useId(), model: useId() };
  const [provider, setProvider] = useState<AiProvider>(initialProvider);
  const models = providers.find((p) => p.provider === provider)?.models ?? [];
  const [model, setModel] = useState(current?.model ?? models[0] ?? "");
  const [apiKey, setApiKey] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [test, setTest] = useState<{ ok: boolean; text: string; ms: number } | null>(null);
  const replacing = current?.configured === true;
  const keyRequired = !replacing;

  const changeProvider = (next: AiProvider) => {
    setProvider(next);
    setModel(providers.find((p) => p.provider === next)?.models[0] ?? "");
  };

  const save = async () => {
    if (keyRequired && !apiKey.trim()) {
      setError("Hãy nhập khóa API.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await onSave(provider, {
        apiKey: apiKey.trim() || undefined,
        model,
        version: replacing ? current?.version : undefined,
      });
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
    setBusy(true);
    const started = performance.now();
    try {
      const result = await onTest(provider);
      const ok = result.lastTestResult === "Đạt";
      setTest({ ok, text: result.lastTestResult ?? "Không có kết quả", ms: Math.round(performance.now() - started) });
    } catch (e) {
      const problem = e instanceof ApiError ? e.problem : undefined;
      setTest({ ok: false, text: errorMessage(problem?.code, "Không kiểm tra được"), ms: 0 });
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog
      open
      title={replacing ? "Thay khóa API" : "Thêm khóa API"}
      icon="key"
      size="lg"
      onClose={onClose}
      dismissible={!busy}
      footer={
        <>
          <Button onClick={onClose} disabled={busy}>
            Hủy
          </Button>
          <Button variant="primary" icon="save" onClick={() => void save()} disabled={busy || !model}>
            Lưu khóa
          </Button>
        </>
      }
    >
      <Field label="Nhà cung cấp" htmlFor={ids.provider} required>
        <Select
          id={ids.provider}
          value={provider}
          disabled={replacing || busy}
          onChange={(event) => changeProvider(event.target.value as AiProvider)}
        >
          {PROVIDERS.filter((p) => replacing || p === initialProvider || !taken.includes(p)).map((p) => (
            <option key={p} value={p}>
              {PROVIDER_LABEL[p]} — {PROVIDER_CONSOLE[p]}
            </option>
          ))}
        </Select>
      </Field>
      <Field
        label="Khóa API"
        htmlFor={ids.key}
        required={keyRequired}
        hint={
          replacing
            ? `Để trống nếu chỉ đổi mô hình. Khóa đang lưu: ••••${current?.keyHint ?? ""}`
            : "Khóa không hiện lại; sau khi lưu chỉ hiện 4 ký tự cuối."
        }
        error={error}
      >
        <TextInput
          id={ids.key}
          type="password"
          autoComplete="off"
          spellCheck={false}
          value={apiKey}
          disabled={busy}
          onChange={(event) => setApiKey(event.target.value)}
          placeholder="Dán khóa API"
        />
      </Field>
      <Field label="Mô hình" htmlFor={ids.model} required>
        <Select id={ids.model} value={model} disabled={busy} onChange={(event) => setModel(event.target.value)}>
          {models.map((m) => (
            <option key={m} value={m}>
              {m}
            </option>
          ))}
        </Select>
      </Field>
      {onTest && replacing && (
        <div className="flex flex-col gap-2">
          <div className="flex items-center gap-2">
            <Button size="sm" icon="verified_user" onClick={() => void runTest()} disabled={busy}>
              Kiểm tra kết nối
            </Button>
            {test && (
              <span className={test.ok ? "text-success" : "text-danger"}>
                {test.text}
                {test.ms > 0 && ` · ${test.ms} ms`}
              </span>
            )}
          </div>
          <Alert tone="warning">Kiểm tra gửi một yêu cầu rất nhỏ tới nhà cung cấp và có thể bị tính phí trên khóa của bạn.</Alert>
        </div>
      )}
    </Dialog>
  );
}
