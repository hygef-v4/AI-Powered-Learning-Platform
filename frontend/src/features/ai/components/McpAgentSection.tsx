"use client";

import { useId, useState, type ReactNode } from "react";
import { formatDate, masked } from "@/shared/ai";
import { ApiError } from "@/shared/api/client";
import { Button, Dialog, Field, IconButton, Pill, SettingsGroup, TableFrame, Td, TextInput, Th, cx } from "@/shared/ui";
import { ACCESS_LABEL, MAX_AGENTS, MCP_STATUS, describeErrorCode } from "../model/labels";
import { McpClientSetup } from "./SetupGuides";
import type { McpAccess, McpCreated, McpCredential } from "../model/types";
import { msg } from "@/i18n/msg";

type Props = {
  credentials: McpCredential[];
  onCreate: (label: string, access: McpAccess, days: number) => Promise<McpCreated>;
  onUpdate: (id: string, label: string, access: McpAccess) => Promise<void>;
  onRevoke: (id: string) => Promise<void>;
};

/** Bảng credential MCP (UC 32): tối đa 5 còn hiệu lực; sửa nhãn và hạ quyền; token chỉ hiện một lần lúc tạo. */
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
      setError(msg("ai.components.McpAgentSection.couldNotRevokeTheAgent"));
    } finally {
      setRevoking(null);
    }
  };

  return (
    <SettingsGroup
      title="Agent MCP"
      action={
        <Button variant="primary" size="sm" icon="add" onClick={() => setEditing("new")} disabled={active >= MAX_AGENTS}>
          {msg("ai.components.McpAgentSection.connectNewAgent")}</Button>
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
            <Th>{msg("ai.components.McpAgentSection.labelAndToken")}</Th>
            <Th>{msg("ai.components.McpAgentSection.permissions")}</Th>
            <Th>{msg("ai.components.McpAgentSection.expires")}</Th>
            <Th>{msg("ai.components.McpAgentSection.status")}</Th>
            <Th>
              <span className="sr-only">{msg("ai.components.McpAgentSection.actions")}</span>
            </Th>
          </tr>
        </thead>
        <tbody>
          {credentials.length === 0 && (
            <tr>
              <Td colSpan={5} className="py-6 text-center! text-muted">
                {msg("ai.components.McpAgentSection.noAgentsYet")}</Td>
            </tr>
          )}
          {credentials.map((c) => (
            <tr key={c.id}>
              <Td>
                <p className="truncate font-semibold" title={c.label}>{c.label}</p>
                <p className="font-mono text-xs text-muted">{masked(c.tokenHint)}</p>
              </Td>
              <Td className="whitespace-nowrap">{ACCESS_LABEL[c.access]}</Td>
              <Td className="whitespace-nowrap">{formatDate(c.expiresAt)}</Td>
              <Td>
                <Pill tone={MCP_STATUS[c.status].tone}>{MCP_STATUS[c.status].text}</Pill>
              </Td>
              <Td>
                {c.status === "ACTIVE" && (
                  <div className="flex justify-end gap-1">
                    <IconButton icon="edit" label={msg("ai.components.McpAgentSection.editAgent", { label: String(c.label) })} onClick={() => setEditing(c)} />
                    <IconButton
                      icon="trash"
                      label={msg("ai.components.McpAgentSection.revokeAgent", { label: String(c.label) })}
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
        title={msg("ai.components.McpAgentSection.revokeAgent2")}
        icon="warning"
        tone="danger"
        onClose={() => setRevoking(null)}
        footer={
          <Button variant="danger" onClick={() => revoking && void revoke(revoking)}>
            {msg("ai.components.McpAgentSection.revoke")}</Button>
        }
      >
        <p>{msg("ai.components.McpAgentSection.areYouSureYouWant")}{revoking?.label}{msg("ai.components.McpAgentSection.text")}</p>
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
 * / 90 ngày. Sửa chỉ hiện token đã che; agent chỉ đọc không nâng lên đọc và ghi được (phải tạo agent mới).
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
  const upgradeBlocked = current?.access === "READ_ONLY";

  const submit = async () => {
    const name = label.trim();
    if (!name || name.length > 60) {
      setError(msg("ai.components.McpAgentSection.theLabelMustBe1"));
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await onSubmit(name, access, days);
    } catch (e) {
      const problem = e instanceof ApiError ? e.problem : undefined;
      setError(describeErrorCode(problem?.code, problem?.detail ?? (editing ? msg("ai.components.McpAgentSection.couldNotSaveTheAgent") : msg("ai.components.McpAgentSection.couldNotCreateTheToken"))));
      setBusy(false);
    }
  };

  return (
    <Dialog
      open
      title={editing ? msg("ai.components.McpAgentSection.editAgent2") : msg("ai.components.McpAgentSection.connectNewAgent")}
      icon="smart_toy"
      tone="accent"
      size={editing ? "lg" : "md"}
      onClose={onClose}
      dismissible={!busy}
      footer={
        <Button variant="primary" onClick={() => void submit()} disabled={busy}>
          {editing ? msg("ai.components.McpAgentSection.save") : msg("ai.components.McpAgentSection.createToken")}
        </Button>
      }
    >
      <Field label={msg("ai.components.McpAgentSection.label")} htmlFor={labelId} required error={error}>
        <TextInput id={labelId} value={label} maxLength={60} onChange={(e) => setLabel(e.target.value)} disabled={busy} />
      </Field>
      {current && (
        <div className="flex flex-col gap-1.5">
          <span className="text-xs font-semibold text-body">Token</span>
          <TokenCard value={`mcp_••••••••${current.tokenHint}`} />
          <p className="text-xs text-muted">{msg("ai.components.McpAgentSection.tokenShownOnce")}</p>
        </div>
      )}
      <fieldset className="flex flex-col gap-1.5">
        <legend className="mb-1.5 text-xs font-semibold text-body">
          {msg("ai.components.McpAgentSection.permissions")}<span className="text-danger"> *</span>
        </legend>
        {(["READ_ONLY", "READ_WRITE"] as McpAccess[]).map((option) => {
          const locked = upgradeBlocked && option === "READ_WRITE";
          return (
            <label
              key={option}
              className={cx(
                "flex items-center gap-2 rounded-xl border px-3 py-2.5",
                locked ? "cursor-not-allowed opacity-60" : "cursor-pointer",
                access === option ? "border-primary bg-primary-soft" : "border-line-strong",
              )}
            >
              <input
                type="radio"
                name="mcp-access"
                checked={access === option}
                onChange={() => setAccess(option)}
                disabled={busy || locked}
              />
              <span className="font-semibold text-ink">{ACCESS_LABEL[option]}</span>
            </label>
          );
        })}
        {upgradeBlocked && <p className="text-xs text-muted">{msg("ai.components.McpAgentSection.upgradeNeedsNewAgent")}</p>}
      </fieldset>
      {!editing && (
        <>
          <fieldset>
            <legend className="mb-1.5 text-xs font-semibold text-body">{msg("ai.components.McpAgentSection.expires")}</legend>
            <div className="flex gap-1.5" role="radiogroup" aria-label={msg("ai.components.McpAgentSection.expires")}>
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
                  {d} {msg("ai.components.McpAgentSection.days")}</button>
              ))}
            </div>
          </fieldset>
          <p className="text-xs text-muted">
            {msg("ai.components.McpAgentSection.youHave")}{active}/{MAX_AGENTS} {msg("ai.components.McpAgentSection.activeAgents")}</p>
        </>
      )}
    </Dialog>
  );
}

/** Thẻ cam chứa token, biểu tượng sao chép nằm trong thẻ bên phải; dùng chung cho popup Tạo và Sửa. */
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
          {msg("ai.components.McpAgentSection.close")}</Button>
      }
    >
      <TokenCard value={created.token}>
        <IconButton
          icon={copied === "token" ? "check" : "copy"}
          label={copied === "token" ? msg("ai.components.McpAgentSection.copied") : msg("ai.components.McpAgentSection.copyToken")}
          onClick={() => void copy("token", created.token)}
        />
      </TokenCard>
      <dl className="grid grid-cols-[110px_1fr] gap-x-3 gap-y-1 text-xs">
        <dt className="text-muted">{msg("ai.components.McpAgentSection.label")}</dt>
        <dd className="text-ink">{created.credential.label}</dd>
        <dt className="text-muted">{msg("ai.components.McpAgentSection.permissions")}</dt>
        <dd className="text-ink">{ACCESS_LABEL[created.credential.access]}</dd>
        <dt className="text-muted">{msg("ai.components.McpAgentSection.expired")}</dt>
        <dd className="text-ink">{formatDate(created.credential.expiresAt)}</dd>
      </dl>
      <McpClientSetup token={created.token} />
      <span className="sr-only" role="status">
        {copied ? msg("ai.components.McpAgentSection.copied") : ""}
      </span>
    </Dialog>
  );
}
