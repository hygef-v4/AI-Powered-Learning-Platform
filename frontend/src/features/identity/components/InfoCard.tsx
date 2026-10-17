/** Thẻ thông tin trắng ở góc trên panel phải (ví dụ "Lần đầu sử dụng?", "Bảo mật tài khoản"). */
export function InfoCard({ title, subtitle, points }: { title: string; subtitle: string; points: string[] }) {
  return (
    <div className="w-72 rounded-xl bg-white p-4 text-slate-800 shadow-lg">
      <div className="flex items-start gap-3">
        <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-indigo-50 text-indigo-600">
          <svg viewBox="0 0 24 24" className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
            <path d="M12 3 4 6v6c0 5 3.4 8.5 8 9 4.6-.5 8-4 8-9V6l-8-3Z" />
          </svg>
        </span>
        <div>
          <p className="text-sm font-semibold">{title}</p>
          <p className="text-xs text-slate-500">{subtitle}</p>
        </div>
      </div>
      <ul className="mt-3 space-y-1 border-t border-slate-100 pt-3 text-xs text-slate-600">
        {points.map((point) => (
          <li key={point} className="flex gap-2">
            <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-indigo-500" />
            {point}
          </li>
        ))}
      </ul>
    </div>
  );
}
