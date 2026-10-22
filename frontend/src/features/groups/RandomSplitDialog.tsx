"use client";

import { useId, useState } from "react";
import { ApiError } from "@/shared/api/client";
import { Alert, Button, Dialog, Field, TableFrame, Td, TextInput, Th } from "@/shared/ui";
import { randomPreview } from "./api";
import { personLabel } from "./GroupEditor";
import type { Draft, DraftView, GroupSetView } from "./types";

type Props = {
  classId: string;
  set: GroupSetView;
  onClose: () => void;
  /** Bản xem trước đưa sang trình sửa để chỉnh rồi mới lưu (BR-U12-03). */
  onPreview: (drafts: Draft[]) => void;
};

/** Popup Chia ngẫu nhiên (t02d): sĩ số tối đa 2–20, chỉ chia người chưa có nhóm. */
export function RandomSplitDialog({ classId, set, onClose, onPreview }: Props) {
  const sizeId = useId();
  const [size, setSize] = useState("4");
  const [preview, setPreview] = useState<DraftView[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const n = set.ungrouped.length;
  const max = Number(size);
  const valid = Number.isInteger(max) && max >= 2 && max <= 20;
  const groups = valid && n > 0 ? Math.ceil(n / max) : 0;
  const people = new Map(set.ungrouped.map((p) => [p.accountId, p]));
  set.groups.forEach((g) => g.members.forEach((m) => people.set(m.accountId, m)));

  const load = async () => {
    setBusy(true);
    setError(null);
    try {
      setPreview(await randomPreview(classId, max));
    } catch (e) {
      setError(e instanceof ApiError ? (e.problem.detail ?? "Không chia được") : "Không chia được, thử lại sau.");
    } finally {
      setBusy(false);
    }
  };

  const generated = preview?.filter((d) => d.generated) ?? [];

  return (
    <Dialog
      open
      title="Chia ngẫu nhiên"
      icon="groups"
      size="lg"
      onClose={onClose}
      dismissible={!busy}
      footer={
        <>
          <Button onClick={onClose}>Hủy</Button>
          {preview ? (
            <Button
              variant="primary"
              onClick={() =>
                onPreview(preview.map((d, i) => ({ key: d.id ?? `random-${i}`, id: d.id, name: d.name, leaderId: d.leaderId, memberIds: d.memberIds })))
              }
            >
              Sửa rồi lưu {generated.length} nhóm mới
            </Button>
          ) : (
            <Button variant="primary" onClick={() => void load()} disabled={!valid || n === 0 || busy}>
              Xem trước
            </Button>
          )}
        </>
      }
    >
      <p className="text-muted">{n} sinh viên chưa có nhóm; nhóm đã có giữ nguyên.</p>
      <Field
        label="Số thành viên tối đa mỗi nhóm"
        htmlFor={sizeId}
        required
        error={valid ? null : "Từ 2 đến 20"}
        hint={groups > 0 ? `Sẽ tạo ${groups} nhóm, mỗi nhóm ${Math.floor(n / groups)}–${Math.ceil(n / groups)} người` : undefined}
      >
        <TextInput id={sizeId} inputMode="numeric" value={size} onChange={(e) => { setSize(e.target.value); setPreview(null); }} />
      </Field>
      {error && (
        <Alert tone="danger" role="alert">
          {error}
        </Alert>
      )}
      {preview && (
        <>
          <p className="text-xs text-muted">Trưởng nhóm được chọn ngẫu nhiên; bạn đổi được trước khi lưu.</p>
          <TableFrame>
            <thead>
              <tr>
                <Th>Nhóm</Th>
                <Th>Thành viên</Th>
                <Th>Trưởng nhóm</Th>
              </tr>
            </thead>
            <tbody>
              {generated.map((d) => (
                <tr key={d.name}>
                  <Td className="font-semibold">{d.name}</Td>
                  <Td>{d.memberIds.map((id) => personLabel(people.get(id))).join(", ")}</Td>
                  <Td>{personLabel(people.get(d.leaderId ?? ""))}</Td>
                </tr>
              ))}
            </tbody>
          </TableFrame>
        </>
      )}
    </Dialog>
  );
}
