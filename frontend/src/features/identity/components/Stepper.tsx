import { Icon } from "@/shared/ui/Icon";

/**
 * Thanh 3 bước của Kích hoạt và Khôi phục (design `c02a`–`c04`): bước xong là vòng xanh lá có dấu ✓,
 * bước hiện tại là vòng xanh đậm có số, bước sau màu xám; nối bằng vạch ngắn.
 */
export function Stepper({ steps, current }: { steps: string[]; current: number }) {
  return (
    <ol className="flex flex-wrap items-center gap-x-2 gap-y-1.5 text-xs font-semibold whitespace-nowrap" aria-label="Các bước">
      {steps.map((label, index) => {
        const state = index < current ? "done" : index === current ? "current" : "todo";
        return (
          <li key={label} className="flex items-center gap-2" aria-current={state === "current" ? "step" : undefined}>
            {index > 0 && (
              <span aria-hidden="true" className={`h-0.5 w-6 ${index <= current ? "bg-success" : "bg-line"}`} />
            )}
            {state === "done" ? (
              <span className="flex size-5 items-center justify-center rounded-full border border-success-soft bg-success-soft text-success">
                <Icon name="check" size={13} />
              </span>
            ) : (
              <span
                className={`flex size-5 items-center justify-center rounded-full text-[11px] ${
                  state === "current" ? "bg-primary-strong font-bold text-on-primary" : "bg-sunken text-muted"
                }`}
              >
                {index + 1}
              </span>
            )}
            <span
              className={
                state === "done" ? "text-success" : state === "current" ? "text-primary-ink" : "text-muted"
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
