"use client";

import type { ReactNode } from "react";
import { Button, Dialog, Pill } from "@/shared/ui";
import { SYSTEM_ACTOR } from "./AuditTable";
import { RESULT_LABELS, actionLabel, formatFull, objectTypeLabel } from "./labels";
import type { AuditLogItem } from "./types";

function Row({ label, children }: { label: string; children: ReactNode }) {
  return (
    <>
      <dt className="text-muted">{label}</dt>
      <dd className="min-w-0 break-all text-ink">{children}</dd>
    </>
  );
}

/** Popup Chi tiết bản ghi (artboard a13b): chỉ đọc, không có nút sửa hay xóa. */
export function AuditDetailDialog({ item, onClose }: { item: AuditLogItem | null; onClose: () => void }) {
  return (
    <Dialog
      open={item !== null}
      title="Chi tiết bản ghi"
      icon="receipt_long"
      onClose={onClose}
      footer={
        <Button onClick={onClose} data-testid="audit-detail-close-button">
          Đóng
        </Button>
      }
    >
      {item && (
        <div className="flex flex-col gap-3.5" data-testid="audit-detail">
          <p className="text-xs text-muted">Bản ghi không sửa hoặc xóa được</p>
          <dl className="grid grid-cols-[130px_1fr] gap-x-4 gap-y-2.5 text-[13px]">
            <Row label="Mã">
              <span className="font-mono text-xs">{item.id}</span>
            </Row>
            <Row label="Thời gian">{formatFull(item.occurredAt)}</Row>
            <Row label="Người thực hiện">{item.actorId ? (item.actorEmail ?? item.actorId) : SYSTEM_ACTOR}</Row>
            <Row label="Hành động">{actionLabel(item.action)}</Row>
            <Row label="Đối tượng">
              {item.objectType ? objectTypeLabel(item.objectType) : "—"}
              {item.objectId && (
                <>
                  {" · "}
                  <span className="font-mono text-xs">{item.objectId}</span>
                </>
              )}
            </Row>
            <Row label="Kết quả">
              <Pill tone={RESULT_LABELS[item.result].tone}>{RESULT_LABELS[item.result].label}</Pill>
            </Row>
            <Row label="Lý do">{item.reason ?? "—"}</Row>
          </dl>
          <div className="flex flex-col gap-1.5">
            <p className="text-xs font-semibold text-body">Chi tiết (JSON, chỉ đọc)</p>
            <pre
              className="m-0 max-h-72 overflow-auto rounded-xl bg-ink px-3.5 py-3 font-mono text-xs leading-relaxed whitespace-pre-wrap text-line-strong"
              data-testid="audit-detail-json"
            >
              {JSON.stringify(item.details ?? {}, null, 2)}
            </pre>
          </div>
        </div>
      )}
    </Dialog>
  );
}
