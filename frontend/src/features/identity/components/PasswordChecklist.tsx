import { checkPassword } from "../passwordRules";

/** Hai điều kiện hiện dưới ô mật khẩu như mock-up: "Ít nhất 8 ký tự", "Có cả chữ và số". */
export function PasswordChecklist({ password }: { password: string }) {
  const checks = checkPassword(password);
  const items = [
    { ok: checks.minLength, label: "Ít nhất 8 ký tự" },
    { ok: checks.letterAndDigit, label: "Có cả chữ và số" },
  ];
  return (
    <ul className="mt-2 flex gap-4 text-xs">
      {items.map((item) => (
        <li key={item.label} className={item.ok ? "text-emerald-600" : "text-slate-400"}>
          <span aria-hidden="true">{item.ok ? "✓" : "○"}</span> {item.label}
          <span className="sr-only">{item.ok ? " (đạt)" : " (chưa đạt)"}</span>
        </li>
      ))}
    </ul>
  );
}
