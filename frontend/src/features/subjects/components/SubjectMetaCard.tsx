import { Button, Pill, Section } from "@/shared/ui";
import type { SubjectDetail } from "../model/types";
import { msg } from "@/i18n/msg";

/** Thẻ Trạng thái của môn, cùng kiểu thẻ Trạng thái ở Chi tiết tài khoản: trạng thái, ngày tạo, nút lưu trữ / mở lại. */
export function SubjectMetaCard({
  subject,
  disabled = false,
  onStatus,
}: {
  subject: SubjectDetail;
  disabled?: boolean;
  onStatus: () => void;
}) {
  const created = new Date(subject.createdAt);
  const createdLabel = Number.isNaN(created.getTime())
    ? msg("subjects.components.SubjectMetaCard.unavailable")
    : new Intl.DateTimeFormat("vi-VN", {
        timeZone: "Asia/Ho_Chi_Minh",
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      }).format(created);
  const archived = subject.status === "ARCHIVED";
  return (
    <Section title={msg("subjects.components.SubjectMetaCard.status")}>
      <dl className="flex flex-col gap-2.5 text-[13px]">
        <div className="flex items-center justify-between">
          <dt className="text-muted">{msg("subjects.components.SubjectMetaCard.status")}</dt>
          <dd>
            <Pill tone={archived ? "danger" : "success"}>{archived ? msg("subjects.components.SubjectMetaCard.archived") : msg("subjects.components.SubjectMetaCard.active")}</Pill>
          </dd>
        </div>
        <div className="flex items-center justify-between">
          <dt className="text-muted">{msg("subjects.components.SubjectMetaCard.created")}</dt>
          <dd className="text-ink">{createdLabel}</dd>
        </div>
      </dl>
      <Button
        id="subject-status-open"
        icon={archived ? "lock_open" : "archive"}
        variant={archived ? "primary" : "danger"}
        disabled={disabled}
        onClick={onStatus}
        data-testid="subject-status-open"
      >
        {archived ? msg("subjects.components.SubjectMetaCard.reopenSubject") : msg("subjects.components.SubjectMetaCard.archiveSubject")}
      </Button>
    </Section>
  );
}
