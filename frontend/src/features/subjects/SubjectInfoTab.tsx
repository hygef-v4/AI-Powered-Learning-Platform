import { Button, Section } from "@/shared/ui";
import { SubjectEditForm } from "./SubjectEditForm";
import type { SubjectMutationProps } from "./subjectMutation";

export function SubjectInfoTab({ editing, disabled, onEdit, onCancel, ...props }: SubjectMutationProps & { editing: boolean; disabled: boolean; onEdit: () => void; onCancel: () => void }) {
  const { subject } = props;
  return <Section title="Thông tin môn học" actions={!editing && <Button size="sm" onClick={onEdit} disabled={disabled} id="subject-edit-open" data-testid="subject-edit-open">Chỉnh sửa thông tin</Button>}>
    {editing ? <SubjectEditForm {...props} onCancel={onCancel} /> : <dl className="flex flex-col gap-2 text-sm">
      <dt className="text-xs text-muted">Mã môn</dt><dd className="font-mono font-semibold">{subject.code}</dd>
      <dt className="mt-2 text-xs text-muted">Tên môn học</dt><dd className="break-words [overflow-wrap:anywhere]">{subject.name}</dd>
      <dt className="mt-2 text-xs text-muted">Mô tả</dt><dd className="whitespace-pre-wrap break-words text-body">{subject.description || "Chưa có mô tả."}</dd>
    </dl>}
  </Section>;
}
