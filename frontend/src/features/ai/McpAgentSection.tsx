"use client";

import { useId, useState } from "react";
import { formatDate, formatShort, masked } from "@/shared/ai";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Dialog, Field, Pill, Section, TableFrame, Td, TextInput, Th, cx } from "@/shared/ui";
import { ACCESS_HINT, ACCESS_LABEL, MAX_AGENTS, MCP_STATUS, errorMessage } from "./labels";
import { mcpConfigSample, mcpEndpoint } from "./SetupGuides";
import type { McpAccess, McpCreated, McpCredential } from "./types";

type Props = {
  credentials: McpCredential[];
  onCreate: (label: string, access: McpAccess, days: number) => Promise<McpCreated>;
  onRevoke: (id: string) => Promise<void>;
};

/** Bảng credential MCP (UC 32): tối đa 5 còn hiệu lực; token chỉ hiện một lần lúc tạo. */
export function McpAgentSection({ credentials, onCreate, onRevoke }: Props) {
  const [creating, setCreating] = useState(false);
  const [created, setCreated] = useState<McpCreated | null>(null);
  const [revoking, setRevoking] = useState<McpCredential | null>(null);
  const [error, setError] = useState<string | null>(null);
  const active = credentials.filter((c) => c.status === "ACTIVE").length;

  const revoke = async (credential: McpCredential) => {
    setError(null);
    try {
      await onRevoke(credential.id);
    } catch {
      setError("Không thu hồi được agent, thử lại sau.");
    } finally {
      setRevoking(null);
    }
  };

  return (
    <Section
      title="Agent MCP"
      icon="smart_toy"
      tone="accent"
      description={`Kết nối agent riêng để dùng công cụ của nền tảng · quyền Chỉ đọc hoặc Đọc và ghi · tối đa ${MAX_AGENTS} agent còn hiệu lực`}
      actions={
        <Button variant="primary" size="sm" icon="add" onClick={() => setCreating(true)} disabled={active >= MAX_AGENTS}>
          Kết nối agent mới
        </Button>
      }
    >
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
      {credentials.length === 0 ? (
        <p className="text-[13px] text-muted">Chưa có agent nào được kết nối.</p>
      ) : (
        <TableFrame>
          <thead>
            <tr>
              <Th>Nhãn và token</Th>
              <Th>Quyền</Th>
              <Th>Ngày tạo</Th>
              <Th>Hạn dùng</Th>
              <Th>Lần dùng cuối</Th>
              <Th>Trạng thái</Th>
              <Th className="text-right">Thao tác</Th>
            </tr>
          </thead>
          <tbody>
            {credentials.map((c) => (
              <tr key={c.id}>
                <Td>
                  <p className="font-semibold">{c.label}</p>
                  <p className="font-mono text-[11px] text-muted">{masked(c.tokenHint)}</p>
                </Td>
                <Td>{ACCESS_LABEL[c.access]}</Td>
                <Td>{formatDate(c.createdAt)}</Td>
                <Td>{formatDate(c.expiresAt)}</Td>
                <Td>{c.lastUsedAt ? formatShort(c.lastUsedAt) : "—"}</Td>
                <Td>
                  <Pill tone={MCP_STATUS[c.status].tone}>{MCP_STATUS[c.status].text}</Pill>
                </Td>
                <Td className="text-right">
                  {c.status === "ACTIVE" && (
                    <Button size="sm" variant="ghost" onClick={() => setRevoking(c)}>
                      Thu hồi
                    </Button>
                  )}
                </Td>
              </tr>
            ))}
          </tbody>
        </TableFrame>
      )}
      {creating && (
        <McpCredentialDialog
          active={active}
          onClose={() => setCreating(false)}
          onCreate={async (label, access, days) => {
            const result = await onCreate(label, access, days);
            setCreating(false);
            setCreated(result);
          }}
        />
      )}
      {created && <McpTokenDialog created={created} onClose={() => setCreated(null)} />}
      <Dialog
        open={revoking !== null}
        title="Thu hồi agent?"
        icon="warning"
        tone="danger"
        onClose={() => setRevoking(null)}
        footer={
          <>
            <Button onClick={() => setRevoking(null)}>Hủy</Button>
            <Button variant="danger" onClick={() => revoking && void revoke(revoking)}>
              Thu hồi
            </Button>
          </>
        }
      >
        <p>Agent “{revoking?.label}” không gọi được công cụ của nền tảng từ lời gọi tiếp theo. Thao tác không hoàn tác được.</p>
      </Dialog>
    </Section>
  );
}

const DAYS = [7, 30, 90];

/** Popup Kết nối agent mới (c12b): nhãn, quyền, hạn dùng 7 / 30 / 90 ngày (mặc định 30). */
export function McpCredentialDialog({
  active,
  onClose,
  onCreate,
}: {
  active: number;
  onClose: () => void;
  onCreate: (label: string, access: McpAccess, days: number) => Promise<void>;
}) {
  const labelId = useId();
  const [label, setLabel] = useState("");
  const [access, setAccess] = useState<McpAccess>("READ_ONLY");
  const [days, setDays] = useState(30);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submit = async () => {
    const name = label.trim();
    if (!name || name.length > 60) {
      setError("Nhãn cần 1–60 ký tự.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await onCreate(name, access, days);
    } catch (e) {
      const problem = e instanceof ApiError ? e.problem : undefined;
      setError(errorMessage(problem?.code, problem?.detail ?? "Không tạo được token, thử lại sau."));
      setBusy(false);
    }
  };

  return (
    <Dialog
      open
      title="Kết nối agent mới"
      icon="smart_toy"
      tone="accent"
      onClose={onClose}
      dismissible={!busy}
      footer={
        <>
          <Button onClick={onClose} disabled={busy}>
            Hủy
          </Button>
          <Button variant="primary" icon="key" onClick={() => void submit()} disabled={busy}>
            Tạo token
          </Button>
        </>
      }
    >
      <Field label="Nhãn" htmlFor={labelId} required hint="Ví dụ tên ứng dụng agent" error={error}>
        <TextInput id={labelId} value={label} maxLength={60} onChange={(e) => setLabel(e.target.value)} disabled={busy} />
      </Field>
      <fieldset className="flex flex-col gap-1.5">
        <legend className="mb-1.5 text-xs font-semibold text-body">
          Quyền<span className="text-danger"> *</span>
        </legend>
        {(["READ_ONLY", "READ_WRITE"] as McpAccess[]).map((option) => (
          <label
            key={option}
            className={cx(
              "flex cursor-pointer items-start gap-2 rounded-xl border px-3 py-2.5",
              access === option ? "border-primary bg-primary-soft" : "border-line-strong",
            )}
          >
            <input type="radio" name="mcp-access" checked={access === option} onChange={() => setAccess(option)} disabled={busy} className="mt-0.5" />
            <span>
              <span className="block font-semibold text-ink">{ACCESS_LABEL[option]}</span>
              <span className="text-xs text-muted">{ACCESS_HINT[option]}</span>
            </span>
          </label>
        ))}
      </fieldset>
      <fieldset>
        <legend className="mb-1.5 text-xs font-semibold text-body">Hạn dùng</legend>
        <div className="flex gap-1.5" role="radiogroup" aria-label="Hạn dùng">
          {DAYS.map((d) => (
            <button
              key={d}
              type="button"
              role="radio"
              aria-checked={days === d}
              onClick={() => setDays(d)}
              disabled={busy}
              className={cx(
                "rounded-full border px-3 py-1 text-xs font-semibold",
                days === d ? "border-primary/30 bg-primary-soft text-primary" : "border-line-strong bg-white text-body",
              )}
            >
              {d} ngày
            </button>
          ))}
        </div>
      </fieldset>
      <p className="text-xs text-muted">
        Bạn đang có {active}/{MAX_AGENTS} agent còn hiệu lực.
      </p>
    </Dialog>
  );
}

/** Popup token hiện một lần (c12c): sao chép trước khi đóng; đóng rồi không xem lại được. */
export function McpTokenDialog({ created, onClose }: { created: McpCreated; onClose: () => void }) {
  const [copied, setCopied] = useState<string | null>(null);
  const endpoint = mcpEndpoint();
  const copy = async (what: string, text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(what);
    } catch {
      setCopied(null);
    }
  };
  return (
    <Dialog
      open
      title="Token agent"
      icon="key"
      tone="warning"
      size="lg"
      onClose={onClose}
      dismissible={false}
      footer={
        <Button variant="primary" onClick={onClose}>
          Tôi đã sao chép, đóng
        </Button>
      }
    >
      <Alert tone="warning" title="Sao chép token trước khi đóng">
        Token chỉ hiện một lần. Ai có token này có thể dùng công cụ của nền tảng bằng tài khoản của bạn.
      </Alert>
      <div className="flex items-center gap-2 rounded-xl border border-line-strong bg-shell px-3 py-2">
        <code className="min-w-0 flex-1 font-mono text-xs break-all text-ink">{created.token}</code>
        <Button size="sm" onClick={() => void copy("token", created.token)}>
          {copied === "token" ? "Đã sao chép" : "Sao chép"}
        </Button>
      </div>
      <dl className="grid grid-cols-[110px_1fr] gap-x-3 gap-y-1 text-xs">
        <dt className="text-muted">Nhãn</dt>
        <dd className="text-ink">{created.credential.label}</dd>
        <dt className="text-muted">Quyền</dt>
        <dd className="text-ink">{ACCESS_LABEL[created.credential.access]}</dd>
        <dt className="text-muted">Hết hạn</dt>
        <dd className="text-ink">{formatDate(created.credential.expiresAt)}</dd>
        <dt className="text-muted">Địa chỉ MCP</dt>
        <dd className="font-mono text-ink">{endpoint}</dd>
      </dl>
      <div className="flex flex-col gap-1.5">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold text-body">Đoạn cấu hình mẫu</span>
          <Button size="sm" variant="ghost" onClick={() => void copy("config", mcpConfigSample(endpoint, created.token))}>
            {copied === "config" ? "Đã sao chép" : "Sao chép cấu hình"}
          </Button>
        </div>
        <pre className="overflow-x-auto rounded-xl bg-shell p-3 font-mono text-[11px] text-ink">{mcpConfigSample(endpoint)}</pre>
      </div>
    </Dialog>
  );
}
