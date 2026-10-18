import { Section } from "@/shared/ui";
import type { SubjectDetail } from "./types";

export function SubjectMetaCard({ subject }: { subject: SubjectDetail }) {
  const created = new Date(subject.createdAt);
  const createdLabel = Number.isNaN(created.getTime()) ? "Không khả dụng" : new Intl.DateTimeFormat("vi-VN", { timeZone: "Asia/Ho_Chi_Minh", day: "2-digit", month: "2-digit", year: "numeric" }).format(created);
  return <Section title="Thông tin môn">
    <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-3 text-xs">
      <dt className="text-muted">Mã môn</dt><dd className="font-mono font-semibold">{subject.code}</dd>
      <dt className="text-muted">Ngày tạo</dt><dd>{createdLabel}</dd>
      <dt className="text-muted">Số lớp</dt><dd>{subject.classCount === null ? "Không khả dụng" : subject.classCount}</dd>
    </dl>
  </Section>;
}
