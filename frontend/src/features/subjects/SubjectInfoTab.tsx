import { Section } from "@/shared/ui";
import { SubjectEditForm } from "./SubjectEditForm";
import type { SubjectMutationProps } from "./subjectMutation";

/** Thẻ Thông tin môn học: sửa ngay trên thẻ như Thông tin tài khoản, không có nút "Chỉnh sửa" riêng. */
export function SubjectInfoTab(props: SubjectMutationProps) {
  return <Section title="Thông tin môn học">
    <SubjectEditForm {...props} />
  </Section>;
}
