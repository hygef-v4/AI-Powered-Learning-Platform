import { cx } from "./cx";
import { Icon, type IconName } from "./Icon";
import { SOFT_TONES, type Tone } from "./tones";

const SIZES = {
  sm: { box: "size-[30px]", icon: 17 },
  md: { box: "size-[38px]", icon: 21 },
};

/** Ô vuông màu nhạt chứa icon, đặt cạnh tiêu đề thẻ hoặc popup. */
export function IconTile({ icon, tone = "primary", size = "md" }: { icon: IconName; tone?: Tone; size?: "sm" | "md" }) {
  return (
    <span className={cx("inline-flex shrink-0 items-center justify-center rounded-[10px]", SIZES[size].box, SOFT_TONES[tone])}>
      <Icon name={icon} size={SIZES[size].icon} />
    </span>
  );
}
