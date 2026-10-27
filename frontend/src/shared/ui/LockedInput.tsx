import { cx } from "./cx";
import { TextInput } from "./inputs";

/**
 * Ô không sửa được (email trường, vai trò, mã môn…): `disabled` nên không bấm, không focus được; chỉ đổi sang màu disable
 * (viền nhạt, nền chìm, chữ nhạt). Dùng chung cho Hồ sơ, Chi tiết tài khoản, Chi tiết môn học, Chi tiết lớp.
 */
export function LockedInput({ id, value, className }: { id: string; value: string; className?: string }) {
  return (
    <TextInput
      id={id}
      value={value}
      disabled
      wrapperClassName={cx("border-line-strong! bg-shell! opacity-100! cursor-not-allowed", className)}
      className="cursor-not-allowed text-muted"
    />
  );
}
