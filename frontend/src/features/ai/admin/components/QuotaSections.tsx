"use client";

import { useCallback, useEffect, useId, useState } from "react";
import { formatTokens } from "@/shared/ai";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Dialog, Field, IconButton, SettingsGroup, TableFrame, Td, TextInput, Th } from "@/shared/ui";
import {
  deleteTeacherQuota,
  findTeachers,
  getPolicy,
  getTeacherQuotas,
  savePolicy,
  setTeacherQuota,
  type QuotaPolicy,
  type TeacherCandidate,
  type TeacherQuota,
} from "../api";

const MAX_TOKENS = 1_000_000_000;
const TOKENS_ERROR = "Nhập số token nguyên từ 0 tới 1.000.000.000";

/** Ô số token: rỗng hoặc không phải số nguyên 0…1 tỉ thì trả null. Dấu chấm phân nhóm bỏ đi trước khi gửi. */
function parseTokens(value: string): number | null {
  const text = value.replace(/[.\s]/g, "");
  if (!/^\d+$/.test(text)) return null;
  const n = Number(text);
  return n <= MAX_TOKENS ? n : null;
}

/** Chỉ giữ chữ số rồi thêm dấu chấm mỗi 3 chữ số khi đang gõ: "1000000" thành "1.000.000". */
function groupDigits(value: string): string {
  return value.replace(/\D/g, "").replace(/^0+(?=\d)/, "").replace(/\B(?=(\d{3})+(?!\d))/g, ".");
}

/** Hạn mức chung (BR-U06-12): mỗi giảng viên mỗi ngày và trần toàn hệ thống; một mức cho mọi mô hình. */
export function QuotaPolicySection() {
  const ids = { teacher: useId(), global: useId() };
  const [policy, setPolicy] = useState<QuotaPolicy | null>(null);
  const [perTeacher, setPerTeacher] = useState("");
  const [global, setGlobal] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  const show = (loaded: QuotaPolicy) => {
    setPolicy(loaded);
    setPerTeacher(loaded.defaultDayTokens === null ? "" : groupDigits(String(loaded.defaultDayTokens)));
    setGlobal(loaded.globalDayTokens === null ? "" : groupDigits(String(loaded.globalDayTokens)));
  };

  useEffect(() => {
    let active = true;
    getPolicy().then(
      (loaded) => {
        if (active) show(loaded);
      },
      () => {
        if (active) setError("Không tải được hạn mức, thử lại sau.");
      },
    );
    return () => {
      active = false;
    };
  }, []);

  const teacherValue = parseTokens(perTeacher);
  const globalValue = parseTokens(global);
  const teacherError = perTeacher !== "" && teacherValue === null ? TOKENS_ERROR : null;
  const globalError = global !== "" && globalValue === null ? TOKENS_ERROR : null;
  const dirty = policy !== null && (teacherValue !== policy.defaultDayTokens || globalValue !== policy.globalDayTokens);

  const submit = async () => {
    if (!policy || teacherValue === null || globalValue === null) return;
    setBusy(true);
    setError(null);
    setSaved(false);
    try {
      show(await savePolicy(teacherValue, globalValue, policy.version));
      setSaved(true);
    } catch (e) {
      const problem = e instanceof ApiError ? e.problem : undefined;
      setError(
        problem?.status === 409
          ? "Hạn mức vừa được thay đổi ở nơi khác, hãy tải lại trang."
          : (problem?.detail ?? "Không lưu được hạn mức, thử lại sau."),
      );
    } finally {
      setBusy(false);
    }
  };

  return (
    <SettingsGroup id="ai-quota" title="Hạn mức chung">
      {policy && !policy.configured && <Alert tone="warning">Chưa cấu hình hạn mức: giảng viên chưa dùng được API của trường.</Alert>}
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
      {saved && !dirty && (
        <Alert tone="success" role="status">
          Đã lưu hạn mức.
        </Alert>
      )}
      <div className="grid grid-cols-1 items-center gap-y-3 py-2 md:grid-cols-[minmax(0,1fr)_1px_minmax(0,1fr)] md:gap-x-6">
        <div className="flex items-center justify-between gap-3">
          <label htmlFor={ids.teacher} className="text-sm font-medium whitespace-nowrap text-ink">
            Giảng viên / ngày
          </label>
          <TextInput
            id={ids.teacher}
            inputMode="numeric"
            suffix="token"
            wrapperClassName="w-1/2 min-w-[170px]"
            value={perTeacher}
            invalid={teacherError !== null}
            disabled={!policy || busy}
            onChange={(e) => {
              setPerTeacher(groupDigits(e.target.value));
              setSaved(false);
            }}
          />
        </div>
        <span aria-hidden="true" className="hidden h-8 w-px bg-line-strong md:block" />
        <div className="flex items-center justify-between gap-3">
          <label htmlFor={ids.global} className="text-sm font-medium whitespace-nowrap text-ink">
            Toàn hệ thống / ngày
          </label>
          <TextInput
            id={ids.global}
            inputMode="numeric"
            suffix="token"
            wrapperClassName="w-1/2 min-w-[170px]"
            value={global}
            invalid={globalError !== null}
            disabled={!policy || busy}
            onChange={(e) => {
              setGlobal(groupDigits(e.target.value));
              setSaved(false);
            }}
          />
        </div>
      </div>
      {(teacherError ?? globalError) && (
        <p role="alert" className="text-xs text-danger">
          {teacherError ?? globalError}
        </p>
      )}
      <div className="flex justify-end pt-2">
        <Button
          variant="primary"
          onClick={() => void submit()}
          disabled={!policy || busy || !dirty || teacherValue === null || globalValue === null}
        >
          Lưu
        </Button>
      </div>
    </SettingsGroup>
  );
}

/** Mức riêng từng giảng viên: thay cho mức chung; 0 là chặn dùng API trường. */
export function TeacherQuotaTable() {
  const [quotas, setQuotas] = useState<TeacherQuota[] | null>(null);
  const [editing, setEditing] = useState<TeacherQuota | "new" | null>(null);
  const [removing, setRemoving] = useState<TeacherQuota | null>(null);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(
    () =>
      getTeacherQuotas().then(
        (loaded) => {
          setQuotas(loaded);
          setError(null);
        },
        () => setError("Không tải được mức riêng, thử lại sau."),
      ),
    [],
  );

  useEffect(() => {
    let active = true;
    getTeacherQuotas().then(
      (loaded) => {
        if (active) setQuotas(loaded);
      },
      () => {
        if (active) setError("Không tải được mức riêng, thử lại sau.");
      },
    );
    return () => {
      active = false;
    };
  }, []);

  const remove = async (quota: TeacherQuota) => {
    setRemoving(null);
    try {
      await deleteTeacherQuota(quota.accountId);
      await reload();
    } catch {
      setError("Không xóa được mức riêng, thử lại sau.");
    }
  };

  return (
    <SettingsGroup
      title="Mức riêng từng giảng viên"
      action={
        <Button variant="primary" size="sm" icon="add" onClick={() => setEditing("new")}>
          Thêm
        </Button>
      }
    >
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
      {!quotas && !error && <p className="py-3 text-[13px] text-muted">Đang tải…</p>}
      {quotas && (
        <TableFrame filledHead className="mt-2 [&_table]:table-fixed">
          <colgroup><col /><col className="w-[150px]" /><col className="w-[84px]" /></colgroup><thead>
            <tr>
              <Th>Giảng viên</Th>
              <Th className="text-right">Token / ngày</Th>
              <Th className="w-[1%]">
                <span className="sr-only">Thao tác</span>
              </Th>
            </tr>
          </thead>
          <tbody>
            {quotas.length === 0 && (
              <tr>
                <Td colSpan={3} className="py-6 text-center! text-muted">
                  Chưa có mức riêng
                </Td>
              </tr>
            )}
            {quotas.map((t) => (
              <tr key={t.accountId}>
                <Td>
                  <p className="font-semibold">{t.name}</p>
                  <p className="text-[11px] text-muted">{t.email}</p>
                </Td>
                <Td className="text-right whitespace-nowrap tabular-nums">{t.dayTokens === 0 ? "0 · chặn" : formatTokens(t.dayTokens)}</Td>
                <Td>
                  <div className="flex justify-end gap-1">
                    <IconButton icon="edit" label={`Sửa mức riêng của ${t.name}`} onClick={() => setEditing(t)} />
                    <IconButton
                      icon="trash"
                      label={`Xóa mức riêng của ${t.name}`}
                      className="text-danger! hover:bg-danger-soft"
                      onClick={() => setRemoving(t)}
                    />
                  </div>
                </Td>
              </tr>
            ))}
          </tbody>
        </TableFrame>
      )}
      {editing && (
        <TeacherQuotaDialog
          current={editing === "new" ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={async () => {
            setEditing(null);
            await reload();
          }}
        />
      )}
      <Dialog
        open={removing !== null}
        title="Xóa mức riêng?"
        icon="warning"
        tone="warning"
        onClose={() => setRemoving(null)}
        footer={
          <Button variant="danger" onClick={() => removing && void remove(removing)}>
            Xóa
          </Button>
        }
      >
        <p>Bạn có chắc muốn xóa mức riêng của {removing?.name} không?</p>
      </Dialog>
    </SettingsGroup>
  );
}

function TeacherQuotaDialog({
  current,
  onClose,
  onSaved,
}: {
  current: TeacherQuota | null;
  onClose: () => void;
  onSaved: () => Promise<void>;
}) {
  const ids = { email: useId(), tokens: useId(), list: useId() };
  const [query, setQuery] = useState("");
  const [candidates, setCandidates] = useState<TeacherCandidate[]>([]);
  const [teacher, setTeacher] = useState<TeacherCandidate | null>(current);
  const [tokens, setTokens] = useState(current ? groupDigits(String(current.dayTokens)) : "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const value = parseTokens(tokens);
  const typed = query.trim();

  useEffect(() => {
    if (current || teacher || typed === "") return;
    let active = true;
    const timer = setTimeout(() => {
      findTeachers(typed).then(
        (found) => {
          if (active) setCandidates(found);
        },
        () => {
          if (active) setCandidates([]);
        },
      );
    }, 150);
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [typed, teacher, current]);

  const shown = teacher || typed === "" ? [] : candidates;

  const save = async () => {
    if (!teacher || value === null) return;
    setBusy(true);
    setError(null);
    try {
      await setTeacherQuota(teacher.accountId, value);
      await onSaved();
    } catch (e) {
      const problem = e instanceof ApiError ? e.problem : undefined;
      setError(problem?.code === "AI_QUOTA_NOT_TEACHER" ? "Tài khoản này không phải giảng viên." : (problem?.detail ?? "Không lưu được mức riêng."));
      setBusy(false);
    }
  };

  return (
    <Dialog
      open
      title={current ? "Sửa mức riêng" : "Thêm mức riêng"}
      icon="person"
      onClose={onClose}
      dismissible={!busy}
      footer={
        <Button variant="primary" onClick={() => void save()} disabled={busy || !teacher || value === null}>
          Lưu
        </Button>
      }
    >
      {current ? (
        <p>
          <span className="font-semibold text-ink">{current.name}</span> · {current.email}
        </p>
      ) : (
        <Field label="Email giảng viên" htmlFor={ids.email} required>
          <div>
            <TextInput
              id={ids.email}
              type="email"
              autoComplete="off"
              role="combobox"
              aria-expanded={shown.length > 0}
              aria-controls={ids.list}
              aria-autocomplete="list"
              maxLength={100}
              placeholder="ten@fpt.edu.vn"
              value={teacher ? teacher.email : query}
              onChange={(e) => {
                setTeacher(null);
                setQuery(e.target.value);
              }}
              disabled={busy}
            />
            {shown.length > 0 && (
              <ul
                id={ids.list}
                role="listbox"
                aria-label="Giảng viên gợi ý"
                className="mt-1 max-h-56 overflow-y-auto rounded-xl border border-line bg-surface py-1"
              >
                {shown.map((c) => (
                  <li key={c.accountId} role="option" aria-selected={false}>
                    <button
                      type="button"
                      onClick={() => setTeacher(c)}
                      className="flex w-full flex-col px-3 py-2 text-left hover:bg-sunken"
                    >
                      <span className="text-sm text-ink">{c.email}</span>
                      <span className="text-[11px] text-muted">{c.name}</span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
          {teacher && <p className="text-xs text-muted">{teacher.name}</p>}
        </Field>
      )}
      <Field
        label="Token / ngày"
        htmlFor={ids.tokens}
        required
        error={tokens !== "" && value === null ? TOKENS_ERROR : error}
      >
        <TextInput id={ids.tokens} inputMode="numeric" suffix="token" value={tokens} onChange={(e) => setTokens(groupDigits(e.target.value))} disabled={busy} />
      </Field>
    </Dialog>
  );
}
