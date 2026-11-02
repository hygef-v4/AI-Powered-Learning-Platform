import { Fragment } from "react";
import { Icon } from "@/shared/ui/Icon";

/**
 * Thanh 3 bước của Kích hoạt và Khôi phục (design `c02a`–`c04`): bước xong là vòng xanh lá có dấu ✓, bước hiện tại là vòng
 * xanh đậm có số, bước sau màu xám; nhãn chỉ ghi tên bước (số đã ở trong vòng). Luôn nằm trên một dòng ở mọi cỡ màn hình:
 * vạch nối co giãn theo chỗ trống, màn quá hẹp thì nhãn tự cắt bớt.
 */
export function Stepper({ steps, current }: { steps: string[]; current: number }) {
  return (
    <ol className="flex w-full min-w-0 flex-nowrap items-center gap-1.5 text-xs font-semibold whitespace-nowrap" aria-label="Các bước">
      {steps.map((label, index) => {
        const state = index < current ? "done" : index === current ? "current" : "todo";
        return (
          <Fragment key={label}>
            {index > 0 && (
              <li aria-hidden="true" className={`h-0.5 min-w-2 flex-1 rounded-full ${index <= current ? "bg-success" : "bg-line"}`} />
            )}
            <li className="flex min-w-0 shrink items-center gap-1.5" aria-current={state === "current" ? "step" : undefined}>
              {state === "done" ? (
                <span className="flex size-5 shrink-0 items-center justify-center rounded-full border border-success-soft bg-success-soft text-success">
                  <Icon name="check" size={13} />
                </span>
              ) : (
                <span
                  className={`flex size-5 shrink-0 items-center justify-center rounded-full text-[11px] ${
                    state === "current" ? "bg-primary-strong font-bold text-on-primary" : "bg-sunken text-muted"
                  }`}
                >
                  {index + 1}
                </span>
              )}
              <span
                className={`min-w-0 truncate ${state === "done" ? "text-success" : state === "current" ? "text-primary-ink" : "text-muted"}`}
                title={label}
              >
                {label}
              </span>
            </li>
          </Fragment>
        );
      })}
    </ol>
  );
}
