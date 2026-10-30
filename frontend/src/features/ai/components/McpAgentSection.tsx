"use client";

import { useId, useState, type ReactNode } from "react";
import { formatDate, masked } from "@/shared/ai";
import { ApiError } from "@/shared/api/client";
import { Button, Dialog, Field, IconButton, Pill, SettingsGroup, TableFrame, Td, TextInput, Th, cx } from "@/shared/ui";
import { revealMcpToken } from "../api";
import { ACCESS_LABEL, MAX_AGENTS, MCP_STATUS, errorMessage } from "../model/labels";
import { JsonCode, mcpConfigSample, mcpEndpoint } from "./SetupGuides";
import type { McpAccess, McpCreated, McpCredential } from "../model/types";

type Props = {
  credentials: McpCredential[];
  onCreate: (label: string, access: McpAccess, days: number) => Promise<McpCreated>;
  onUpdate: (id: string, label: string, access: McpAccess) => Promise<void>;
  onRevoke: (id: string) => Promise<void>;
};

/** Bảng credential MCP (UC 32): tối đa 5 còn hiệu lực; sửa nhãn và quyền, xem lại token trong popup Sửa. */
export function McpAgentSection({ credentials, onCreate, onUpdate, onRevoke }: Props) {
  const [editing, setEditing] = useState<McpCredential | "new" | null>(null);
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
    <SettingsGroup
      title="Agent MCP"
      action={
        <Button variant="primary" size="sm" icon="add" onClick={() => setEditing("new")} disabled={active >= MAX_AGENTS}>
          Kết nối agent mới
        </Button>
      }
    >
      {error && (
        <p role="alert" className="text-xs text-danger">
          {error}
        </p>
      )}
      <TableFrame filledHead className="mt-2 [&_table]:min-w-[600px] [&_table]:table-fixed">
        <colgroup><col /><col className="w-[120px]" /><col className="w-[120px]" /><col className="w-[110px]" /><col className="w-[84px]" /></colgroup>
        <thead>
          <tr>
            <Th>Nhãn và token</Th>
            <Th>Quyền</Th>
            <Th>Hạn dùng</Th>
            <Th>Trạng thái</Th>
            <Th>
              <span className="sr-only">Thao tác</span>
            </Th>
          </tr>
        </thead>
        <tbody>
          {credentials.length === 0 && (
            <tr>
              <Td colSpan={5} className="py-6 text-center! text-muted">
                Chưa có agent
              </Td>
            </tr>
          )}
          {credentials.map((c) => (
            <tr key={c.id}>
              <Td>
                <p className="truncate font-semibold" title={c.label}>{c.label}</p>
                <p className="font-mono text-[11px] text-muted">{masked(c.tokenHint)}</p>
              </Td>
              <Td className="whitespace-nowrap">{ACCESS_LABEL[c.access]}</Td>
              <Td className="whitespace-nowrap">{formatDate(c.expiresAt)}</Td>
              <Td>
                <Pill tone={MCP_STATUS[c.status].tone}>{MCP_STATUS[c.status].text}</Pill>
              </Td>
              <Td>
                {c.status === "ACTIVE" && (
                  <div className="flex justify-end gap-1">
                    <IconButton icon="edit" label={`Sửa agent ${c.label}`} onClick={() => setEditing(c)} />
                    <IconButton
                      icon="trash"
                      label={`Thu hồi agent ${c.label}`}
                      className="text-danger! hover:bg-danger-soft"
                      onClick={() => setRevoking(c)}
                    />
                  </div>
                )}
              </Td>
            </tr>
          ))}
        </tbody>
      </TableFrame>
      {editing && (
        <McpCredentialDialog
          active={active}
          current={editing === "new" ? null : editing}
          onClose={() => setEditing(null)}
          onSubmit={async (label, access, days) => {
            if (editing === "new") {
              const result = await onCreate(label, access, days);
              setEditing(null);
              setCreated(result);
            } else {
              await onUpdate(editing.id, label, access);
              setEditing(null);
            }
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
          <Button variant="danger" onClick={() => revoking && void revoke(revoking)}>
            Thu hồi
          </Button>
        }
      >
        <p>Bạn có chắc muốn thu hồi agent “{revoking?.label}” không?</p>
      </Dialog>
    </SettingsGroup>
  );
}

const DAYS = [7, 30, 90];

async function copyText(text: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    return false;
  }
}

/**
 * Popup Kết nối agent mới (c12b) và Sửa agent dùng chung, chỉ khác tiêu đề: nhãn, quyền; tạo mới có thêm hạn dùng 7 / 30
 * / 90 ngày, sửa có thêm ô xem lại token (mắt, sao chép).
 */
export function McpCredentialDialog({
  active,
  current = null,
  onClose,
  onSubmit,
}: {
  active: number;
  current?: McpCredential | null;
  onClose: () => void;
  onSubmit: (label: string, access: McpAccess, days: number) => Promise<void>;
}) {
  const labelId = useId();
  const [label, setLabel] = useState(current?.label ?? "");
  const [access, setAccess] = useState<McpAccess>(current?.access ?? "READ_ONLY");
  const [days, setDays] = useState(30);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const editing = current !== null;

  const submit = async () => {
    const name = label.trim();
    if (!name || name.length > 60) {
      setError("Nhãn cần 1–60 ký tự.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await onSubmit(name, access, days);
    } catch (e) {
      const problem = e instanceof ApiError ? e.problem : undefined;
      setError(errorMessage(problem?.code, problem?.detail ?? (editing ? "Không lưu được agent, thử lại sau." : "Không tạo được token, thử lại sau.")));
      setBusy(false);
    }
  };

  return (
    <Dialog
      open
      title={editing ? "Sửa agent" : "Kết nối agent mới"}
      icon="smart_toy"
      tone="accent"
      size={editing ? "lg" : "md"}
      onClose={onClose}
      dismissible={!busy}
      footer={
        <Button variant="primary" onClick={() => void submit()} disabled={busy}>
          {editing ? "Lưu" : "Tạo token"}
        </Button>
      }
    >
      <Field label="Nhãn" htmlFor={labelId} required error={error}>
        <TextInput id={labelId} value={label} maxLength={60} onChange={(e) => setLabel(e.target.value)} disabled={busy} />
      </Field>
      {current && <TokenField credential={current} />}
      <fieldset className="flex flex-col gap-1.5">
        <legend className="mb-1.5 text-xs font-semibold text-body">
          Quyền<span className="text-danger"> *</span>
        </legend>
        {(["READ_ONLY", "READ_WRITE"] as McpAccess[]).map((option) => (
          <label
            key={option}
            className={cx(
              "flex cursor-pointer items-center gap-2 rounded-xl border px-3 py-2.5",
              access === option ? "border-primary bg-primary-soft" : "border-line-strong",
            )}
          >
            <input type="radio" name="mcp-access" checked={access === option} onChange={() => setAccess(option)} disabled={busy} />
            <span className="font-semibold text-ink">{ACCESS_LABEL[option]}</span>
          </label>
        ))}
      </fieldset>
      {!editing && (
        <>
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
                    days === d ? "border-primary/30 bg-primary-soft text-primary-ink" : "border-line-strong bg-surface text-body",
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
        </>
      )}
    </Dialog>
  );
}

/** Ô token trong popup Sửa: ẩn mặc định, nút mắt và sao chép luôn có; token tạo trước bản này không xem lại được (nút khóa). */
function TokenField({ credential }: { credential: McpCredential }) {
  const [token, setToken] = useState<string | null>(null);
  const [shown, setShown] = useState(false);
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    if (token) return token;
    try {
      const loaded = (await revealMcpToken(credential.id)).token;
      setToken(loaded);
      return loaded;
    } catch {
      setError("Không xem được token, thử lại sau.");
      return null;
    }
  };

  return (
    <div className="flex flex-col gap-1.5">
      <span className="text-xs font-semibold text-body">Token</span>
      <TokenCard value={shown && token ? token : `mcp_••••••••${credential.tokenHint}`}>
        <IconButton
          icon={shown ? "visibility_off" : "visibility"}
          label={!credential.revealable ? "Token cũ, không xem lại được" : shown ? "Ẩn token" : "Xem token"}
          disabled={!credential.revealable}
          onClick={() => {
            if (shown) setShown(false);
            else void load().then((t) => t && setShown(true));
          }}
        />
        <IconButton
          icon={copied ? "check" : "copy"}
          label={!credential.revealable ? "Token cũ, không sao chép được" : copied ? "Đã sao chép" : "Sao chép token"}
          disabled={!credential.revealable}
          onClick={() => void load().then(async (t) => t && setCopied(await copyText(t)))}
        />
      </TokenCard>
      {error && <p className="text-xs text-danger">{error}</p>}
    </div>
  );
}

/** Thẻ cam chứa token, các biểu tượng (xem, sao chép) nằm trong thẻ bên phải; dùng chung cho popup Tạo và Sửa. */
function TokenCard({ value, children }: { value: string; children?: ReactNode }) {
  return (
    <div className="flex min-h-11 items-center gap-1 rounded-xl border border-primary/40 bg-primary-soft py-1.5 pr-1.5 pl-3">
      <code className="min-w-0 flex-1 overflow-x-auto font-mono text-xs whitespace-nowrap text-ink">{value}</code>
      {children}
    </div>
  );
}

/** Popup token sau khi tạo (c12c): token trong thẻ cam có biểu tượng sao chép, đoạn cấu hình mẫu đã điền token. */
export function McpTokenDialog({ created, onClose }: { created: McpCreated; onClose: () => void }) {
  const [copied, setCopied] = useState<string | null>(null);
  const config = mcpConfigSample(mcpEndpoint(), created.token);
  const copy = async (what: string, text: string) => setCopied((await copyText(text)) ? what : null);
  return (
    <Dialog
      open
      title="Token agent"
      icon="key"
      size="lg"
      onClose={onClose}
      footer={
        <Button variant="primary" onClick={onClose}>
          Đóng
        </Button>
      }
    >
      <TokenCard value={created.token}>
        <IconButton
          icon={copied === "token" ? "check" : "copy"}
          label={copied === "token" ? "Đã sao chép" : "Sao chép token"}
          onClick={() => void copy("token", created.token)}
        />
      </TokenCard>
      <dl className="grid grid-cols-[110px_1fr] gap-x-3 gap-y-1 text-xs">
        <dt className="text-muted">Nhãn</dt>
        <dd className="text-ink">{created.credential.label}</dd>
        <dt className="text-muted">Quyền</dt>
        <dd className="text-ink">{ACCESS_LABEL[created.credential.access]}</dd>
        <dt className="text-muted">Hết hạn</dt>
        <dd className="text-ink">{formatDate(created.credential.expiresAt)}</dd>
      </dl>
      <div className="flex flex-col gap-1.5">
        <span className="text-xs font-semibold text-body">Đoạn cấu hình mẫu</span>
        <JsonCode
          text={config}
          action={
            <IconButton
              icon={copied === "config" ? "check" : "copy"}
              label={copied === "config" ? "Đã sao chép" : "Sao chép cấu hình"}
              className="text-[#d4dbe3] hover:bg-white/10 hover:text-white"
              onClick={() => void copy("config", config)}
            />
          }
        />
      </div>
      <span className="sr-only" role="status">
        {copied ? "Đã sao chép" : ""}
      </span>
    </Dialog>
  );
}
