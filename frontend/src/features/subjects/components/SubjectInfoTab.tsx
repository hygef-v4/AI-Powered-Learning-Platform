import { Section } from "@/shared/ui";
import { SubjectEditForm } from "./SubjectEditForm";
import type { SubjectMutationProps } from "../hooks/useSubjectMutation";
import { msg } from "@/i18n/msg";

/** Thẻ Thông tin môn học: sửa ngay trên thẻ như Thông tin tài khoản, không có nút "Chỉnh sửa" riêng. */
export function SubjectInfoTab(props: SubjectMutationProps) {
  return (
    <Section title={msg("subjects.components.SubjectInfoTab.thongTinMonHoc")}>
      <SubjectEditForm {...props} />
    </Section>
  );
}
