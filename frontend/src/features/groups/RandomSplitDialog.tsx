"use client";

import { useId, useState } from "react";
import { Alert, Button, Dialog, Field, Pill, TableFrame, Td, TextInput, Th, showToast } from "@/shared/ui";
import { saveGroups } from "./api";
import { byGroupName, draftsFrom, freeNames, personLabel, saveErrorText, type StudentRow } from "./groupDrafts";
import type { Draft, GroupSetView } from "./types";

function shuffle<T>(items: T[]): T[] {
  const out = [...items];
  for (let i = out.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [out[i], out[j]] = [out[j]!, out[i]!];
  }
  return out;
}

/**
 * Chia người chưa có nhóm theo sĩ số tối đa `max` sao cho các nhóm hơn kém nhau nhiều nhất một người: số nhóm là
 * max(số nhóm đang có, ⌈tổng số sinh viên / max⌉), thiếu thì thêm nhóm mới; rồi xáo ngẫu nhiên người chưa có nhóm và lần lượt
 * đưa từng người vào nhóm ít người nhất (ngang nhau thì chọn ngẫu nhiên). Nhóm mới lấy người đầu tiên làm trưởng nhóm. Nhóm
 * đang có giữ nguyên thành viên nên chỉ lệch hơn một người khi chính chúng đã chênh nhau quá nhiều từ trước.
 */
export function splitUngrouped(drafts: Draft[], ungroupedIds: string[], max: number): Draft[] {
  const pool = shuffle(ungroupedIds);
  const next = drafts.map((d) => ({ ...d, memberIds: [...d.memberIds] }));
  const total = next.reduce((sum, d) => sum + d.memberIds.length, 0) + pool.length;
  const count = Math.max(next.length, Math.ceil(total / max));
  freeNames(next, count - next.length).forEach((name, i) => {
    next.push({ key: `random-${i + 1}`, id: null, name, leaderId: null, memberIds: [] });
  });
  for (const id of pool) {
    const smallest = Math.min(...next.map((d) => d.memberIds.length));
    shuffle(next.filter((d) => d.memberIds.length === smallest))[0]!.memberIds.push(id);
  }
  return next
    .filter((d) => d.memberIds.length > 0)
    .map((d) => (d.leaderId ? d : { ...d, leaderId: d.memberIds[0] ?? null }));
}

/** Sĩ số mặc định: 5 khi lớp có ít hơn 2 nhóm, còn lại bằng nhóm đông nhất (trong khoảng 2–20). */
function defaultSize(drafts: Draft[]) {
  if (drafts.length < 2) return 5;
  return Math.min(20, Math.max(2, ...drafts.map((d) => d.memberIds.length)));
}

/**
 * Popup Chia ngẫu nhiên (t02d, BR-U12-03): chỉ một ô Số thành viên tối đa (2–20, mặc định theo `defaultSize`). Luôn chia
 * người chưa có nhóm vào các nhóm đang có, không đủ chỗ thì thêm nhóm mới, các nhóm hơn kém nhau nhiều nhất một người. Xem
 * trước các nhóm thay đổi, Chia lại nếu chưa ưng, rồi mới lưu.
 */
export function RandomSplitDialog({ classId, set, people, onClose, onSaved }: {
  classId: string;
  set: GroupSetView;
  people: StudentRow[];
  onClose: () => void;
  onSaved: (next: GroupSetView) => void;
}) {
  const sizeId = useId();
  const drafts = draftsFrom(set);
  const grouped = new Set(drafts.flatMap((d) => d.memberIds));
  const ungrouped = people.filter((p) => !grouped.has(p.accountId));
  const [size, setSize] = useState(() => String(defaultSize(drafts)));
  const [preview, setPreview] = useState<Draft[] | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const max = Number(size);
  const valid = Number.isInteger(max) && max >= 2 && max <= 20;

  const split = () => setPreview(splitUngrouped(drafts, ungrouped.map((p) => p.accountId), max));

  const save = async () => {
    if (!preview) return;
    setBusy(true);
    setError(null);
    try {
      onSaved(await saveGroups(classId, set.version, preview));
      showToast("Đã chia nhóm");
    } catch (e) {
      setError(saveErrorText(e, preview));
    } finally {
      setBusy(false);
    }
  };

  // Chỉ hiện nhóm mới hoặc nhóm có thêm người, kèm đủ thành viên sau khi chia.
  const changed = (preview ?? [])
    .map((d) => ({ d, added: d.memberIds.filter((id) => !drafts.find((x) => x.key === d.key)?.memberIds.includes(id)) }))
    .filter(({ added }) => added.length > 0)
    .sort((a, b) => byGroupName(a.d, b.d));
  const nameOf = (accountId: string) => personLabel(people.find((p) => p.accountId === accountId));

  return (
    <Dialog open title="Chia ngẫu nhiên" size="lg" onClose={onClose} dismissible={!busy}
      footer={preview ? <>
        <Button icon="refresh" disabled={busy} onClick={split}>Chia lại</Button>
        <Button variant="primary" icon="save" disabled={busy} onClick={() => void save()}>Lưu {changed.length} nhóm</Button>
      </> : (
        <Button variant="primary" onClick={split} disabled={!valid || ungrouped.length === 0}>Xem trước</Button>
      )}>
      {error && <Alert tone="danger" role="alert">{error}</Alert>}
      <Field label="Số thành viên tối đa mỗi nhóm" htmlFor={sizeId} required error={valid ? null : "Từ 2 đến 20"}>
        <TextInput id={sizeId} inputMode="numeric" value={size} wrapperClassName="w-32"
          onChange={(e) => { setSize(e.target.value); setPreview(null); setError(null); }} />
      </Field>
      {preview && (
        <div className="max-h-80 overflow-y-auto [scrollbar-color:var(--c-muted)_transparent]">
          <TableFrame filledHead className="[&_table]:table-fixed">
            <colgroup><col className="w-[140px]" /><col /></colgroup>
            <thead><tr><Th>Nhóm</Th><Th>Thành viên</Th></tr></thead>
            <tbody>
              {changed.map(({ d }) => (
                <tr key={d.key}>
                  <Td className="font-semibold">{d.name}</Td>
                  <Td className="text-body">
                    {/* Trưởng nhóm đứng đầu, có nhãn bên cạnh tên. */}
                    <span className="flex flex-wrap items-center gap-x-1.5 gap-y-1">
                      {[...d.memberIds].sort((a, b) => Number(b === d.leaderId) - Number(a === d.leaderId)).map((id, index, all) => (
                        <span key={id} className="inline-flex items-center gap-1.5">
                          {nameOf(id)}
                          {id === d.leaderId && <Pill tone="warning">Trưởng nhóm</Pill>}
                          {index < all.length - 1 && <span className="text-muted">,</span>}
                        </span>
                      ))}
                    </span>
                  </Td>
                </tr>
              ))}
            </tbody>
          </TableFrame>
        </div>
      )}
    </Dialog>
  );
}
