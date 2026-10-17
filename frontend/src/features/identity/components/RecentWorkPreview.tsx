/** Thẻ "Bài tập gần đây" chỉ để minh họa trên màn Đăng nhập (SRS 1.1); dữ liệu tĩnh, không gọi API. */
const ITEMS = [
  { title: "Sơ đồ lớp", meta: "SWP391", badge: "23:59", tone: "bg-red-50 text-red-600" },
  { title: "Tài liệu nhóm", meta: "Nhóm 3", badge: "4 ngày", tone: "bg-slate-100 text-slate-600" },
  { title: "Phân tích yêu cầu", meta: "Đã chấm", badge: "8.5", tone: "bg-emerald-50 text-emerald-600" },
];

export function RecentWorkPreview() {
  return (
    <div className="w-72 rounded-xl bg-white p-4 text-slate-800 shadow-lg" aria-hidden="true">
      <div className="flex items-center justify-between border-b border-slate-100 pb-2">
        <p className="text-sm font-semibold">Bài tập gần đây</p>
        <span className="text-slate-400">···</span>
      </div>
      <ul className="mt-2 space-y-3">
        {ITEMS.map((item) => (
          <li key={item.title} className="flex items-center gap-3">
            <span className="h-7 w-7 rounded-lg bg-indigo-50" />
            <div className="flex-1">
              <p className="text-xs font-semibold">{item.title}</p>
              <p className="text-[11px] text-slate-500">{item.meta}</p>
            </div>
            <span className={`rounded-md px-2 py-0.5 text-[11px] font-semibold ${item.tone}`}>{item.badge}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
