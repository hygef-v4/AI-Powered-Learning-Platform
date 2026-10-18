import { Icon } from "@/shared/ui/Icon";

/**
 * Thanh 3 bước của Kích hoạt và Khôi phục (design `c02a`–`c04`): bước xong là vòng xanh lá có dấu ✓,
 * bước hiện tại là vòng xanh đậm có số, bước sau màu xám; nối bằng vạch ngắn.
 */
export function Stepper({ steps, current }: { steps: string[]; current: number }) {
  return (
    <ol className="flex flex-wrap items-center gap-2 text-xs font-semibold" aria-label="Các bước">
      {steps.map((label, index) => {
        const state = index < current ? "done" : index === current ? "current" : "todo";
        return (
          <li key={label} className="flex items-center gap-2" aria-current={state === "current" ? "step" : undefined}>
            {index > 0 && (
              <span aria-hidden="true" className={`h-0.5 w-6 ${index <= current ? "bg-[#6ee7b7]" : "bg-[#e2e8f0]"}`} />
            )}
            {state === "done" ? (
              <span className="flex size-5 items-center justify-center rounded-full border border-[#a7f3d0] bg-[#ecfdf5] text-[#059669]">
                <Icon name="check" size={13} />
              </span>
            ) : (
              <span
                className={`flex size-5 items-center justify-center rounded-full text-[11px] ${
                  state === "current" ? "bg-primary-strong font-bold text-white" : "bg-[#f1f5f9] text-[#94a3b8]"
                }`}
              >
                {index + 1}
              </span>
            )}
            <span
              className={
                state === "done" ? "text-[#059669]" : state === "current" ? "text-primary-strong" : "text-[#94a3b8]"
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
