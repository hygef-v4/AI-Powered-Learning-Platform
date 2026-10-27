import { Button, Pill, Section } from "@/shared/ui";
import type { SubjectDetail } from "./types";

/** Thẻ Trạng thái của môn, cùng kiểu thẻ Trạng thái ở Chi tiết tài khoản: trạng thái, ngày tạo, nút lưu trữ / mở lại. */
export function SubjectMetaCard({ subject, disabled = false, onStatus }: { subject: SubjectDetail; disabled?: boolean; onStatus: () => void }) {
  const created = new Date(subject.createdAt);
  const createdLabel = Number.isNaN(created.getTime()) ? "Không khả dụng" : new Intl.DateTimeFormat("vi-VN", { timeZone: "Asia/Ho_Chi_Minh", day: "2-digit", month: "2-digit", year: "numeric" }).format(created);
  const archived = subject.status === "ARCHIVED";
  return <Section title="Trạng thái">
    <dl className="flex flex-col gap-2.5 text-[13px]">
      <div className="flex items-center justify-between">
        <dt className="text-muted">Trạng thái</dt>
        <dd><Pill tone={archived ? "neutral" : "success"}>{archived ? "Lưu trữ" : "Hoạt động"}</Pill></dd>
      </div>
      <div className="flex items-center justify-between">
        <dt className="text-muted">Ngày tạo</dt>
        <dd className="text-ink">{createdLabel}</dd>
      </div>
    </dl>
    <Button id="subject-status-open" icon={archived ? "lock_open" : "archive"} variant={archived ? "secondary" : "danger"}
      disabled={disabled} onClick={onStatus} data-testid="subject-status-open">
      {archived ? "Mở lại môn học" : "Lưu trữ môn học"}
    </Button>
  </Section>;
}
