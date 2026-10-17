/** Thanh 3 bước: bước xong có ✓ xanh lá, bước hiện tại số trong tròn xanh đậm, bước sau màu xám. */
export function Stepper({ steps, current }: { steps: string[]; current: number }) {
  return (
    <ol className="flex items-center gap-2 text-xs" aria-label="Các bước">
      {steps.map((label, index) => {
        const state = index < current ? "done" : index === current ? "current" : "todo";
        return (
          <li key={label} className="flex items-center gap-2" aria-current={state === "current" ? "step" : undefined}>
            {index > 0 && <span className={`h-px w-6 ${index <= current ? "bg-emerald-400" : "bg-slate-200"}`} />}
            <span
              className={`flex h-5 w-5 items-center justify-center rounded-full text-[10px] font-semibold ${
                state === "done"
                  ? "bg-emerald-50 text-emerald-600"
                  : state === "current"
                    ? "bg-[#1f2a6b] text-white"
                    : "bg-slate-100 text-slate-400"
              }`}
            >
              {state === "done" ? "✓" : index + 1}
            </span>
            <span
              className={
                state === "done" ? "text-emerald-600" : state === "current" ? "font-semibold text-[#1f2a6b]" : "text-slate-400"
              }
            >
              {index + 1}. {label}
            </span>
          </li>
        );
      })}
    </ol>
  );
}
