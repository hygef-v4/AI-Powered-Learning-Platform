import { Button, Pill, Section } from "@/shared/ui";
import { initials } from "@/shared/layout/navigation";
import type { SubjectDetail } from "../model/types";

export function ManagerCard({ subject, onChange, disabled = false }: { subject: SubjectDetail; onChange?: () => void; disabled?: boolean }) {
  return <Section title="Chủ nhiệm môn" actions={onChange && <Button id="subject-manager-open" size="sm" onClick={onChange} disabled={disabled} data-testid="subject-manager-open">{subject.managerId === null ? "Phân công" : "Đổi chủ nhiệm"}</Button>}>
    {subject.managerId === null ? <div><Pill>Chưa phân công</Pill></div>
      : subject.manager === null ? <p className="text-sm text-muted">Không tra được thông tin chủ nhiệm môn.</p>
        : <div className="flex items-center gap-3">
          <span aria-hidden="true" className="flex size-10 shrink-0 items-center justify-center rounded-full bg-accent-soft font-semibold text-accent">{initials(subject.manager.displayName)}</span>
          <div className="min-w-0"><p className="text-sm font-semibold">{subject.manager.displayName}</p><p className="break-all text-xs text-muted">{subject.manager.email}</p></div>
        </div>}
  </Section>;
}
