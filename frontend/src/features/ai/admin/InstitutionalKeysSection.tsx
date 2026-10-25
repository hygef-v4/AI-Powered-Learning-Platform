"use client";

import { useCallback, useEffect, useState } from "react";
import { PROVIDER_LABEL, PROVIDERS, formatShort, masked, type AiProvider } from "@/shared/ai";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Dialog, Pill, Section, Switch, TableFrame, Td, Th } from "@/shared/ui";
import { ApiKeyDialog } from "../ApiKeyDialog";
import { getProviders } from "../api";
import { errorMessage } from "../labels";
import type { ConnectionView, ProviderInfo } from "../types";
import { getInstitutional, revokeInstitutional, saveInstitutional, setInstitutionalEnabled, testInstitutional } from "./api";

const CONFLICT = "Khóa vừa được thay đổi ở nơi khác, hãy tải lại.";

/** Khóa API của trường (UC 14, 15): thêm, kiểm tra, bật/tắt, thay, thu hồi; 409 báo tải lại. */
export function InstitutionalKeysSection() {
  const [connections, setConnections] = useState<ConnectionView[] | null>(null);
  const [providers, setProviders] = useState<ProviderInfo[]>([]);
  const [editing, setEditing] = useState<{ provider: AiProvider; current: ConnectionView | null } | null>(null);
  const [revoking, setRevoking] = useState<AiProvider | null>(null);
  const [busy, setBusy] = useState<AiProvider | null>(null);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(
    () =>
      Promise.all([getInstitutional(), getProviders()]).then(
        ([loaded, catalog]) => {
          setConnections(loaded);
          setProviders(catalog);
        },
        () => setError("Không tải được khóa API của trường, thử lại sau."),
      ),
    [],
  );

  useEffect(() => {
    let active = true;
    Promise.all([getInstitutional(), getProviders()]).then(
      ([loaded, catalog]) => {
        if (!active) return;
        setConnections(loaded);
        setProviders(catalog);
      },
      () => {
        if (active) setError("Không tải được khóa API của trường, thử lại sau.");
      },
    );
    return () => {
      active = false;
    };
  }, []);

  const run = async (provider: AiProvider, action: () => Promise<unknown>, fallback: string) => {
    setBusy(provider);
    setError(null);
    try {
      await action();
      await reload();
    } catch (e) {
      const problem = e instanceof ApiError ? e.problem : undefined;
      setError(problem?.status === 409 ? CONFLICT : errorMessage(problem?.code, fallback));
    } finally {
      setBusy(null);
    }
  };

  const byProvider = new Map((connections ?? []).map((c) => [c.provider, c]));
  const taken = PROVIDERS.filter((p) => byProvider.get(p)?.configured);
  const firstMissing = PROVIDERS.find((p) => !taken.includes(p));

  return (
    <Section
      title="Khóa API của trường"
      icon="key"
      description="Giảng viên dùng khóa đang bật khi chọn nguồn API của trường. Tắt vẫn giữ khóa nhưng không dùng cho yêu cầu mới."
      actions={
        <Button variant="primary" size="sm" icon="add" onClick={() => firstMissing && setEditing({ provider: firstMissing, current: null })} disabled={!firstMissing}>
          Thêm khóa
        </Button>
      }
    >
      {error && (
        <Alert tone="danger" role="alert" action={<Button size="sm" onClick={() => void reload()}>Tải lại</Button>}>
          {error}
        </Alert>
      )}
      {!connections && !error && <p className="text-[13px] text-muted">Đang tải...</p>}
      {connections && (
        <TableFrame>
          <thead>
            <tr>
              <Th>Nhà cung cấp</Th>
              <Th>Khóa</Th>
              <Th>Mô hình</Th>
              <Th>Kiểm tra gần nhất</Th>
              <Th>Bật</Th>
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
                      <Pill tone={c.lastTestResult === "Đạt" ? "success" : "danger"} className="max-w-[240px]">
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
                    {configured && c ? (
                      <Switch
                        checked={c.status === "ACTIVE"}
                        label=""
                        ariaLabel={`Bật khóa ${PROVIDER_LABEL[provider]}`}
                        disabled={busy !== null}
                        onChange={(enabled) =>
                          void run(provider, () => setInstitutionalEnabled(provider, enabled, c.version), "Không đổi được trạng thái khóa.")
                        }
                      />
                    ) : (
                      <span className="text-muted">—</span>
                    )}
                  </Td>
                  <Td>
                    <div className="flex justify-end gap-1.5">
                      {configured ? (
                        <>
                          <Button
                            size="sm"
                            disabled={busy !== null}
                            onClick={() => void run(provider, () => testInstitutional(provider), "Không kiểm tra được kết nối.")}
                          >
                            {busy === provider ? "Đang xử lý..." : "Kiểm tra"}
                          </Button>
                          <Button size="sm" disabled={busy !== null} onClick={() => setEditing({ provider, current: c ?? null })}>
                            Thay khóa
                          </Button>
                          <Button size="sm" variant="ghost" disabled={busy !== null} onClick={() => setRevoking(provider)}>
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
      )}
      <p className="text-[11px] text-muted">Mỗi lần lưu, bật/tắt hoặc thu hồi được ghi vào Nhật ký audit. Kiểm tra có thể tính phí nhỏ trên khóa của trường.</p>
      {editing && (
        <ApiKeyDialog
          key={`${editing.provider}-${editing.current?.version ?? "new"}`}
          current={editing.current}
          initialProvider={editing.provider}
          providers={providers}
          taken={taken}
          onClose={() => setEditing(null)}
          onSave={async (provider, body) => {
            await saveInstitutional(provider, body);
            setEditing(null);
            await reload();
          }}
          onTest={
            editing.current?.configured
              ? async (provider) => {
                  const result = await testInstitutional(provider);
                  await reload();
                  return result;
                }
              : undefined
          }
        />
      )}
      <Dialog
        open={revoking !== null}
        title="Thu hồi khóa của trường?"
        icon="warning"
        tone="danger"
        onClose={() => setRevoking(null)}
        footer={
          <>
            <Button onClick={() => setRevoking(null)}>Hủy</Button>
            <Button
              variant="danger"
              onClick={() => {
                const provider = revoking;
                setRevoking(null);
                if (provider) void run(provider, () => revokeInstitutional(provider), "Không thu hồi được khóa.");
              }}
            >
              Thu hồi
            </Button>
          </>
        }
      >
        <p>
          Khóa {revoking ? PROVIDER_LABEL[revoking] : ""} bị xóa khỏi hệ thống. Giảng viên không dùng được API của trường qua
          nhà cung cấp này cho tới khi thêm khóa mới.
        </p>
      </Dialog>
    </Section>
  );
}
