"use client";

import { useState } from "react";
import { PROVIDER_LABEL, PROVIDERS, masked, type AiProvider } from "@/shared/ai";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Dialog, IconButton, SettingsGroup, TableFrame, Td, Th, cx } from "@/shared/ui";
import { ApiKeyDialog } from "./ApiKeyDialog";
import { errorMessage, shortTestResult } from "./labels";
import type { ConnectionView, ProviderInfo, SaveConnection } from "./types";

const OK = "Đạt";

type Props = {
  connections: ConnectionView[];
  providers: ProviderInfo[];
  onSave: (provider: AiProvider, body: SaveConnection) => Promise<void>;
  onTest: (provider: AiProvider) => Promise<ConnectionView>;
  onRevoke: (provider: AiProvider) => Promise<void>;
};

/**
 * Khóa API cá nhân (UC 31), cùng kiểu bảng Khóa API của trường ở Cài đặt AI: mỗi nhà cung cấp một khóa, chỉ hiện khóa đã
 * thêm; Kiểm tra, thay, thu hồi trên từng dòng. Khóa luôn che, chỉ 4 ký tự cuối.
 */
export function PersonalApiSection({ connections, providers, onSave, onTest, onRevoke }: Props) {
  const [editing, setEditing] = useState<{ provider: AiProvider; current: ConnectionView | null } | null>(null);
  const [revoking, setRevoking] = useState<ConnectionView | null>(null);
  const [testing, setTesting] = useState<AiProvider | null>(null);
  const [error, setError] = useState<string | null>(null);
  const configured = PROVIDERS.flatMap((p) => connections.filter((c) => c.provider === p && c.configured));
  const taken = configured.map((c) => c.provider);
  const firstMissing = PROVIDERS.find((p) => !taken.includes(p));

  const test = async (provider: AiProvider) => {
    setTesting(provider);
    setError(null);
    try {
      await onTest(provider);
    } catch (e) {
      setError(errorMessage(e instanceof ApiError ? e.problem.code : undefined, "Không kiểm tra được kết nối."));
    } finally {
      setTesting(null);
    }
  };

  const revoke = async (provider: AiProvider) => {
    setRevoking(null);
    setError(null);
    try {
      await onRevoke(provider);
    } catch {
      setError("Không thu hồi được khóa, thử lại sau.");
    }
  };

  return (
    <SettingsGroup
      title="Khóa API cá nhân"
      action={
        <Button variant="primary" size="sm" icon="add" onClick={() => firstMissing && setEditing({ provider: firstMissing, current: null })} disabled={!firstMissing}>
          Thêm khóa
        </Button>
      }
    >
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
      <TableFrame filledHead className="mt-2 [&_table]:min-w-[720px] [&_table]:table-fixed">
        <colgroup><col className="w-[140px]" /><col className="w-[150px]" /><col /><col className="w-[260px]" /><col className="w-[84px]" /></colgroup><thead>
          <tr>
            <Th>Nhà cung cấp</Th>
            <Th>Khóa</Th>
            <Th>Mô hình</Th>
            <Th>Kiểm tra</Th>
            <Th>
              <span className="sr-only">Thao tác</span>
            </Th>
          </tr>
        </thead>
        <tbody>
          {configured.length === 0 && (
            <tr>
              <Td colSpan={5} className="py-6 text-center! text-muted">
                Chưa có khóa
              </Td>
            </tr>
          )}
          {configured.map((c) => {
            const label = PROVIDER_LABEL[c.provider];
            return (
              <tr key={c.provider}>
                <Td className="font-semibold whitespace-nowrap">{label}</Td>
                <Td className="font-mono text-xs whitespace-nowrap">{masked(c.keyHint)}</Td>
                <Td className="truncate" title={c.model ?? undefined}>{c.model}</Td>
                <Td>
                  <div className="flex items-center justify-between gap-3">
                    {c.lastTestResult ? (
                      <span
                        title={c.lastTestResult}
                        className={cx(
                          "min-w-0 truncate rounded-md px-2 py-0.5 text-[13px] font-medium",
                          c.lastTestResult === OK ? "bg-success-soft text-success" : "bg-danger-soft text-danger",
                        )}
                      >
                        {shortTestResult(c.lastTestResult)}
                      </span>
                    ) : (
                      <span />
                    )}
                    <Button size="sm" className="shrink-0" disabled={testing !== null} onClick={() => void test(c.provider)}>
                      {testing === c.provider ? "Đang kiểm tra…" : "Kiểm tra"}
                    </Button>
                  </div>
                </Td>
                <Td>
                  <div className="flex items-center justify-end gap-1">
                    <IconButton icon="edit" label={`Thay khóa ${label}`} disabled={testing !== null} onClick={() => setEditing({ provider: c.provider, current: c })} />
                    <IconButton
                      icon="trash"
                      label={`Thu hồi khóa ${label}`}
                      disabled={testing !== null}
                      className="text-danger! hover:bg-danger-soft"
                      onClick={() => setRevoking(c)}
                    />
                  </div>
                </Td>
              </tr>
            );
          })}
        </tbody>
      </TableFrame>
      <p className="pt-2 text-[11px] text-muted">Kiểm tra có thể tính phí nhỏ.</p>
      {editing && (
        <ApiKeyDialog
          key={`${editing.provider}-${editing.current?.version ?? "new"}`}
          current={editing.current}
          initialProvider={editing.provider}
          providers={providers}
          taken={taken}
          onClose={() => setEditing(null)}
          onSave={async (provider, body) => {
            await onSave(provider, body);
            setEditing(null);
          }}
          onTest={
            editing.current?.configured
              ? async (provider) => {
                  const result = await onTest(provider);
                  return { ok: result.lastTestResult === OK, text: result.lastTestResult ?? "Không có kết quả" };
                }
              : undefined
          }
        />
      )}
      <Dialog
        open={revoking !== null}
        title="Thu hồi khóa API?"
        icon="warning"
        tone="danger"
        onClose={() => setRevoking(null)}
        footer={
          <Button variant="danger" onClick={() => revoking && void revoke(revoking.provider)}>
            Thu hồi
          </Button>
        }
      >
        <p>Bạn có chắc muốn thu hồi khóa {revoking ? `${PROVIDER_LABEL[revoking.provider]} ••••${revoking.keyHint ?? ""}` : ""} không?</p>
      </Dialog>
    </SettingsGroup>
  );
}
