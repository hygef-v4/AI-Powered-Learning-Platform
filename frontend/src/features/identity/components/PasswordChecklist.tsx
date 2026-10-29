import { Icon } from "@/shared/ui/Icon";
import { checkPassword } from "../passwordRules";

/**
 * Hai điều kiện dưới ô mật khẩu: "Ít nhất 8 ký tự", "Có cả chữ và số" (design `c02`, `c04`, `c06`).
 * `chips` vẽ thành nhãn bo tròn như popup Đổi mật khẩu; mặc định là dòng chữ có dấu ✓ như màn ba bước.
 */
export function PasswordChecklist({ password, chips }: { password: string; chips?: boolean }) {
  const checks = checkPassword(password);
  const items = [
    { ok: checks.minLength, label: "Ít nhất 8 ký tự" },
    { ok: checks.letterAndDigit, label: "Có cả chữ và số" },
  ];
  return (
    <ul className={chips ? "flex flex-wrap gap-2" : "mt-2.5 flex flex-wrap gap-4 text-xs font-medium"}>
      {items.map((item) =>
        chips ? (
          <li
            key={item.label}
            className={`inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-[11px] font-semibold ${
              item.ok ? "bg-success-soft text-success" : "bg-neutral-soft text-muted"
            }`}
          >
            <Icon name="check_circle" size={14} />
            {item.label}
            <span className="sr-only">{item.ok ? " (đạt)" : " (chưa đạt)"}</span>
          </li>
        ) : (
          <li key={item.label} className={`flex items-center gap-1 ${item.ok ? "text-success" : "text-muted"}`}>
            <Icon name="check" size={14} />
            {item.label}
            <span className="sr-only">{item.ok ? " (đạt)" : " (chưa đạt)"}</span>
          </li>
        ),
      )}
    </ul>
  );
}
