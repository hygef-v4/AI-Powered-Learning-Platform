import { cx } from "./cx";
import { Icon } from "./Icon";
import { TextInput } from "./inputs";

/**
 * Ô không sửa được (email trường, vai trò, mã môn…): `disabled` nên không bấm, không focus được; viền nhạt, nền chìm,
 * chữ nhạt, khóa nhỏ. Dùng chung cho Hồ sơ, Chi tiết tài khoản, Chi tiết môn học.
 */
export function LockedInput({ id, value, className, lock = true }: { id: string; value: string; className?: string; lock?: boolean }) {
  return (
    <TextInput
      id={id}
      value={value}
      disabled
      wrapperClassName={cx("border-line-strong! bg-shell! opacity-100! cursor-not-allowed", className)}
      className="cursor-not-allowed text-muted"
      suffix={lock ? <Icon name="lock" size={16} className="text-faint" /> : undefined}
    />
  );
}
