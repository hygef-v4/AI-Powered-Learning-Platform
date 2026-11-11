"use client";

import { useCallback, useEffect, useState } from "react";
import { PROVIDER_LABEL, masked } from "@/shared/ai";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Dialog, IconButton, SettingsGroup, Switch, TableFrame, Td, Th, cx } from "@/shared/ui";
import type { AiProvider } from "@/shared/ai";
import { ApiKeyDialog } from "./ApiKeyDialog";
import { getProviders } from "../api";
import { errorMessage, shortTestResult } from "../model/labels";
import type { ConnectionView, ProviderInfo, SaveConnection } from "../model/types";

/** Các lời gọi của một bảng khóa: khóa của trường (Admin) hoặc khóa cá nhân. */
export type KeysApi = {
  list: () => Promise<ConnectionView[]>;
  add: (provider: AiProvider, body: SaveConnection) => Promise<unknown>;
  save: (id: string, body: SaveConnection) => Promise<unknown>;
  test: (id: string) => Promise<unknown>;
  testDraft: (provider: AiProvider, body: SaveConnection, id?: string | null) => Promise<{ ok: boolean; message: string }>;
  setEnabled: (id: string, enabled: boolean, version: number) => Promise<unknown>;
  revoke: (id: string) => Promise<unknown>;
};

const CONFLICT = "Khóa vừa được thay đổi ở nơi khác, hãy tải lại.";
const OK = "Đạt";

/**
 * Bảng khóa API dùng chung cho khóa của trường (UC 14, 15) và khóa cá nhân (UC 31; yêu cầu 2026-11-05): nhiều khóa cho
 * mỗi nhà cung cấp; kiểm tra, bật/tắt, thay, thu hồi trên từng dòng; 409 báo tải lại.
 */
export function KeysSection({ title, api, loadError, revokeTitle }: {
  title: string;
  api: KeysApi;
  loadError: string;
  revokeTitle: string;
}) {
  const [connections, setConnections] = useState<ConnectionView[] | null>(null);
  const [providers, setProviders] = useState<ProviderInfo[]>([]);
  const [editing, setEditing] = useState<ConnectionView | "new" | null>(null);
  const [revoking, setRevoking] = useState<ConnectionView | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(
    () =>
      Promise.all([api.list(), getProviders()]).then(
        ([loaded, catalog]) => {
          setConnections(loaded.filter((c) => c.configured && c.id));
          setProviders(catalog);
        },
        () => setError(loadError),
      ),
    [api, loadError],
  );

  useEffect(() => {
    let active = true;
    Promise.all([api.list(), getProviders()]).then(
      ([loaded, catalog]) => {
        if (!active) return;
        setConnections(loaded.filter((c) => c.configured && c.id));
        setProviders(catalog);
      },
      () => {
        if (active) setError(loadError);
      },
    );
    return () => {
      active = false;
    };
  }, [api, loadError]);

  const run = async (id: string, action: () => Promise<unknown>, fallback: string) => {
    setBusy(id);
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

  // Bật / tắt một khóa: chỉ công tắc đó tự khóa trong lúc lưu (Switch), các nút và công tắc khác trong bảng vẫn dùng được.
  const toggle = async (id: string, enabled: boolean, version: number) => {
    setError(null);
    try {
      await api.setEnabled(id, enabled, version);
      await reload();
    } catch (e) {
      const problem = e instanceof ApiError ? e.problem : undefined;
      setError(problem?.status === 409 ? CONFLICT : errorMessage(problem?.code, "Không đổi được trạng thái khóa."));
    }
  };

  const current = editing === "new" ? null : editing;

  return (
    <SettingsGroup
      title={title}
      action={
        <Button variant="primary" size="sm" icon="add" onClick={() => setEditing("new")}>
          Thêm khóa
        </Button>
      }
    >
      {error && (
        <Alert tone="danger" role="alert" action={<Button size="sm" onClick={() => void reload()}>Tải lại</Button>}>
          {error}
        </Alert>
      )}
      {!connections && !error && <p className="py-3 text-[13px] text-muted">Đang tải…</p>}
      {connections && (
        <TableFrame filledHead className="mt-2 [&_table]:min-w-[800px] [&_table]:table-fixed">
          <colgroup><col className="w-[140px]" /><col className="w-[150px]" /><col /><col className="w-[240px]" /><col className="w-[140px]" /></colgroup><thead>
            <tr>
              <Th>Nhà cung cấp</Th>
              <Th>Khóa</Th>
              <Th>Mô hình</Th>
              <Th>Kiểm tra</Th>
              <Th>
                <span className="sr-only">Bật và thao tác</span>
              </Th>
            </tr>
          </thead>
          <tbody>
            {connections.length === 0 && (
              <tr>
                <Td colSpan={5} className="py-6 text-center! text-muted">
                  Chưa có khóa
                </Td>
              </tr>
            )}
            {connections.map((c) => {
              const id = c.id as string;
              const label = PROVIDER_LABEL[c.provider];
              return (
                <tr key={id}>
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
                      <Button
                        size="sm"
                        className="shrink-0"
                        disabled={busy !== null}
                        onClick={() => void run(id, () => api.test(id), "Không kiểm tra được kết nối.")}
                      >
                        {busy === id ? "Đang kiểm tra…" : "Kiểm tra"}
                      </Button>
                    </div>
                  </Td>
                  <Td>
                    <div className="flex items-center justify-end gap-1">
                      <Switch
                        checked={c.status === "ACTIVE"}
                        label=""
                        ariaLabel={`Bật khóa ${label} ••••${c.keyHint ?? ""}`}
                        onChange={(enabled) => toggle(id, enabled, c.version)}
                      />
                      <IconButton icon="edit" label={`Thay khóa ${label}`} disabled={busy !== null} onClick={() => setEditing(c)} />
                      <IconButton
                        icon="trash"
                        label={`Thu hồi khóa ${label}`}
                        disabled={busy !== null}
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
      )}
      <p className="pt-2 text-xs text-muted">Kiểm tra có thể tính phí nhỏ.</p>
      {editing && (
        <ApiKeyDialog
          key={current ? `${current.id}-${current.version}` : "new"}
          current={current}
          initialProvider={current?.provider ?? (providers[0]?.provider ?? "GEMINI")}
          providers={providers}
          onClose={() => setEditing(null)}
          onSave={async (provider, body) => {
            if (current?.id) await api.save(current.id, body);
            else await api.add(provider, body);
            setEditing(null);
            await reload();
          }}
          onTest={async (provider, body) => {
            const result = await api.testDraft(provider, body, current?.id);
            return { ok: result.ok, text: result.message };
          }}
        />
      )}
      <Dialog
        open={revoking !== null}
        title={revokeTitle}
        icon="warning"
        tone="danger"
        onClose={() => setRevoking(null)}
        footer={
          <Button
            variant="danger"
            onClick={() => {
              const target = revoking;
              setRevoking(null);
              if (target?.id) void run(target.id, () => api.revoke(target.id as string), "Không thu hồi được khóa.");
            }}
          >
            Thu hồi
          </Button>
        }
      >
        <p>Bạn có chắc muốn thu hồi khóa {revoking ? `${PROVIDER_LABEL[revoking.provider]} ••••${revoking.keyHint ?? ""}` : ""} không?</p>
      </Dialog>
    </SettingsGroup>
  );
}
