"use client";

import { useCallback, useEffect, useId, useState } from "react";
import { formatTokens } from "@/shared/ai";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Dialog, Field, Section, TableFrame, Td, TextInput, Th } from "@/shared/ui";
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
} from "./api";

const MAX_TOKENS = 1_000_000_000;

/** Ô số token: rỗng hoặc không phải số nguyên 0…1 tỉ thì trả null. */
function parseTokens(value: string): number | null {
  const text = value.replace(/[.\s]/g, "");
  if (!/^\d+$/.test(text)) return null;
  const n = Number(text);
  return n <= MAX_TOKENS ? n : null;
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
    setPerTeacher(loaded.defaultDayTokens === null ? "" : String(loaded.defaultDayTokens));
    setGlobal(loaded.globalDayTokens === null ? "" : String(loaded.globalDayTokens));
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
  const teacherError = perTeacher !== "" && teacherValue === null ? "Nhập số token nguyên từ 0 tới 1.000.000.000" : null;
  const globalError = global !== "" && globalValue === null ? "Nhập số token nguyên từ 0 tới 1.000.000.000" : null;

  const submit = async () => {
    if (!policy || teacherValue === null || globalValue === null || teacherError || globalError) return;
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
    <Section title="Hạn mức chung" icon="speed" description="Một mức áp cho mọi mô hình. Đặt lại lúc 00:00 (giờ Việt Nam).">
      {policy && !policy.configured && (
        <Alert tone="warning">Chưa cấu hình hạn mức: giảng viên chưa dùng được API của trường.</Alert>
      )}
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
      {saved && (
        <Alert tone="success" role="status">
          Đã lưu hạn mức.
        </Alert>
      )}
      <div className="grid gap-3 md:grid-cols-2">
        <Field label="Mỗi giảng viên / ngày" htmlFor={ids.teacher} required error={teacherError}>
          <TextInput
            id={ids.teacher}
            inputMode="numeric"
            suffix="token"
            value={perTeacher}
            invalid={teacherError !== null}
            disabled={!policy || busy}
            onChange={(e) => setPerTeacher(e.target.value)}
          />
        </Field>
        <Field label="Trần toàn hệ thống / ngày" htmlFor={ids.global} required error={globalError}>
          <TextInput
            id={ids.global}
            inputMode="numeric"
            suffix="token"
            value={global}
            invalid={globalError !== null}
            disabled={!policy || busy}
            onChange={(e) => setGlobal(e.target.value)}
          />
        </Field>
      </div>
      <p className="text-[11px] text-muted">
        Hết hạn mức, giảng viên vẫn dùng được API cá nhân hoặc agent MCP của mình.
      </p>
      <div>
        <Button
          variant="primary"
          icon="save"
          onClick={() => void submit()}
          disabled={!policy || busy || teacherValue === null || globalValue === null || teacherError !== null || globalError !== null}
        >
          Lưu hạn mức
        </Button>
      </div>
    </Section>
  );
}

/** Mức riêng từng giảng viên: thay cho mức chung; 0 là chặn dùng API trường. */
export function TeacherQuotaTable() {
  const [quotas, setQuotas] = useState<TeacherQuota[] | null>(null);
  const [filter, setFilter] = useState("");
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

  const q = filter.trim().toLowerCase();
  const shown = (quotas ?? []).filter((t) => !q || t.name.toLowerCase().includes(q) || t.email.toLowerCase().includes(q));

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
    <Section
      title="Mức riêng từng giảng viên"
      icon="person"
      description="Mức riêng thay cho mức chung của giảng viên đó. Đặt 0 để chặn dùng API trường."
      actions={
        <Button size="sm" icon="add" onClick={() => setEditing("new")}>
          Thêm mức riêng
        </Button>
      }
    >
      <TextInput
        icon="search"
        placeholder="Tìm theo tên hoặc email"
        aria-label="Tìm giảng viên có mức riêng"
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
        wrapperClassName="max-w-[320px]"
      />
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
      {quotas && shown.length === 0 && (
        <p className="text-[13px] text-muted">{q ? "Không có giảng viên khớp." : "Chưa có giảng viên nào có mức riêng."}</p>
      )}
      {shown.length > 0 && (
        <TableFrame>
          <thead>
            <tr>
              <Th>Giảng viên</Th>
              <Th className="text-right">Token / ngày</Th>
              <Th className="text-right">Thao tác</Th>
            </tr>
          </thead>
          <tbody>
            {shown.map((t) => (
              <tr key={t.accountId}>
                <Td>
                  <p className="font-semibold">{t.name}</p>
                  <p className="text-[11px] text-muted">{t.email}</p>
                </Td>
                <Td className="text-right whitespace-nowrap">{t.dayTokens === 0 ? "0 · chặn" : formatTokens(t.dayTokens)}</Td>
                <Td>
                  <div className="flex justify-end gap-1.5">
                    <Button size="sm" onClick={() => setEditing(t)}>
                      Sửa
                    </Button>
                    <Button size="sm" variant="ghost" onClick={() => setRemoving(t)}>
                      Xóa
                    </Button>
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
          <>
            <Button onClick={() => setRemoving(null)}>Hủy</Button>
            <Button variant="danger" onClick={() => removing && void remove(removing)}>
              Xóa
            </Button>
          </>
        }
      >
        <p>{removing?.name} sẽ dùng lại mức chung mỗi giảng viên.</p>
      </Dialog>
    </Section>
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
  const ids = { search: useId(), tokens: useId() };
  const [query, setQuery] = useState("");
  const [candidates, setCandidates] = useState<TeacherCandidate[]>([]);
  const [teacher, setTeacher] = useState<TeacherCandidate | null>(current);
  const [tokens, setTokens] = useState(current ? String(current.dayTokens) : "");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const value = parseTokens(tokens);

  useEffect(() => {
    if (current || query.trim().length < 2) return;
    let active = true;
    const timer = setTimeout(() => {
      findTeachers(query.trim()).then(
        (found) => {
          if (active) setCandidates(found);
        },
        () => {
          if (active) setCandidates([]);
        },
      );
    }, 300);
    return () => {
      active = false;
      clearTimeout(timer);
    };
  }, [query, current]);

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
        <>
          <Button onClick={onClose} disabled={busy}>
            Hủy
          </Button>
          <Button variant="primary" icon="save" onClick={() => void save()} disabled={busy || !teacher || value === null}>
            Lưu
          </Button>
        </>
      }
    >
      {current ? (
        <p>
          <span className="font-semibold text-ink">{current.name}</span> · {current.email}
        </p>
      ) : (
        <Field label="Giảng viên" htmlFor={ids.search} required hint="Gõ ít nhất 2 ký tự của tên hoặc email">
          <TextInput
            id={ids.search}
            icon="search"
            value={teacher ? `${teacher.name} · ${teacher.email}` : query}
            onChange={(e) => {
              setTeacher(null);
              setQuery(e.target.value);
            }}
            disabled={busy}
          />
          {!teacher && candidates.length > 0 && (
            <ul role="listbox" aria-label="Giảng viên tìm được" className="max-h-48 overflow-y-auto rounded-xl border border-line">
              {candidates.map((c) => (
                <li key={c.accountId}>
                  <button
                    type="button"
                    role="option"
                    aria-selected={false}
                    onClick={() => setTeacher(c)}
                    className="flex w-full flex-col px-3 py-2 text-left hover:bg-shell"
                  >
                    <span className="font-semibold text-ink">{c.name}</span>
                    <span className="text-[11px] text-muted">{c.email}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </Field>
      )}
      <Field
        label="Token / ngày"
        htmlFor={ids.tokens}
        required
        hint="Đặt 0 để chặn dùng API trường."
        error={tokens !== "" && value === null ? "Nhập số token nguyên từ 0 tới 1.000.000.000" : error}
      >
        <TextInput id={ids.tokens} inputMode="numeric" suffix="token" value={tokens} onChange={(e) => setTokens(e.target.value)} disabled={busy} />
      </Field>
    </Dialog>
  );
}
