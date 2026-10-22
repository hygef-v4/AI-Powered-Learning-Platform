"use client";

import { useState } from "react";
import { PROVIDER_LABEL, PROVIDERS, formatShort, masked, type AiProvider } from "@/shared/ai";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Dialog, Pill, Section, TableFrame, Td, Th } from "@/shared/ui";
import { ApiKeyDialog } from "./ApiKeyDialog";
import { errorMessage } from "./labels";
import type { ConnectionView, ProviderInfo, SaveConnection } from "./types";

type Props = {
  connections: ConnectionView[];
  providers: ProviderInfo[];
  onSave: (provider: AiProvider, body: SaveConnection) => Promise<void>;
  onTest: (provider: AiProvider) => Promise<ConnectionView>;
  onRevoke: (provider: AiProvider) => Promise<void>;
};

/** Bảng khóa API cá nhân theo nhà cung cấp (UC 31); khóa luôn che, chỉ 4 ký tự cuối. */
export function PersonalApiSection({ connections, providers, onSave, onTest, onRevoke }: Props) {
  const [editing, setEditing] = useState<{ provider: AiProvider; current: ConnectionView | null } | null>(null);
  const [revoking, setRevoking] = useState<AiProvider | null>(null);
  const [testing, setTesting] = useState<AiProvider | null>(null);
  const [error, setError] = useState<string | null>(null);
  const byProvider = new Map(connections.map((c) => [c.provider, c]));
  const firstMissing = PROVIDERS.find((p) => !byProvider.get(p)?.configured) ?? "GEMINI";

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
    setError(null);
    try {
      await onRevoke(provider);
      setRevoking(null);
    } catch {
      setRevoking(null);
      setError("Không thu hồi được khóa, thử lại sau.");
    }
  };

  return (
    <Section
      title="API cá nhân"
      icon="key"
      description="Khóa của riêng bạn; chi phí tính vào tài khoản nhà cung cấp của bạn và không giới hạn bởi trường."
      actions={
        <Button variant="primary" size="sm" icon="add" onClick={() => setEditing({ provider: firstMissing, current: null })}>
          Thêm khóa
        </Button>
      }
    >
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
      <TableFrame>
        <thead>
          <tr>
            <Th>Nhà cung cấp</Th>
            <Th>Khóa</Th>
            <Th>Mô hình</Th>
            <Th>Kiểm tra gần nhất</Th>
            <Th className="text-right">Thao tác</Th>
          </tr>
        </thead>
        <tbody>
          {PROVIDERS.map((provider) => {
            const c = byProvider.get(provider);
            const configured = c?.configured === true;
            return (
              <tr key={provider}>
                <Td className="font-semibold">{PROVIDER_LABEL[provider]}</Td>
                <Td className="whitespace-nowrap">
                  {configured ? <span className="font-mono text-xs">{masked(c?.keyHint)}</span> : <span className="text-muted">Chưa cấu hình</span>}
                </Td>
                <Td className="whitespace-nowrap">{configured ? c?.model : "—"}</Td>
                <Td>
                  {configured && c?.lastTestAt ? (
                    <Pill tone={c.lastTestResult === "Đạt" ? "success" : "danger"} className="max-w-[260px]">
                      <span className="truncate" title={c.lastTestResult ?? undefined}>
                        {c.lastTestResult ?? "—"}
                      </span>
                      <span className="shrink-0">· {formatShort(c.lastTestAt)}</span>
                    </Pill>
                  ) : (
                    <span className="text-muted">—</span>
                  )}
                </Td>
                <Td>
                  <div className="flex justify-end gap-1.5">
                    {configured ? (
                      <>
                        <Button size="sm" onClick={() => void test(provider)} disabled={testing !== null}>
                          {testing === provider ? "Đang kiểm tra..." : "Kiểm tra"}
                        </Button>
                        <Button size="sm" onClick={() => setEditing({ provider, current: c ?? null })}>
                          Thay khóa
                        </Button>
                        <Button size="sm" variant="ghost" onClick={() => setRevoking(provider)}>
                          Thu hồi
                        </Button>
                      </>
                    ) : (
                      <Button size="sm" icon="add" onClick={() => setEditing({ provider, current: null })}>
                        Thêm khóa
                      </Button>
                    )}
                  </div>
                </Td>
              </tr>
            );
          })}
        </tbody>
      </TableFrame>
      <p className="text-[11px] text-muted">Nút Kiểm tra gửi một yêu cầu rất nhỏ và có thể phát sinh chi phí nhỏ trên khóa của bạn.</p>
      {editing && (
        <ApiKeyDialog
          key={`${editing.provider}-${editing.current?.version ?? "new"}`}
          current={editing.current}
          initialProvider={editing.provider}
          providers={providers}
          onClose={() => setEditing(null)}
          onSave={async (provider, body) => {
            await onSave(provider, body);
            setEditing(null);
          }}
          onTest={editing.current?.configured ? onTest : undefined}
        />
      )}
      <Dialog
        open={revoking !== null}
        title="Thu hồi khóa API?"
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
        <p>
          Khóa {revoking ? PROVIDER_LABEL[revoking] : ""} bị xóa khỏi hệ thống. Yêu cầu đang chờ dùng khóa này sẽ không chạy
          được; bạn có thể thêm lại khóa sau.
        </p>
      </Dialog>
    </Section>
  );
}
