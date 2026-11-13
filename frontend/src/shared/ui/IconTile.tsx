import { cx } from "./cx";
import { Icon, type IconName } from "./Icon";
import { SOFT_TONES, type Tone } from "./tones";

const SIZES = {
  sm: { box: "size-7", icon: 14 },
  md: { box: "size-9", icon: 18 },
};

/** Ô vuông màu nhạt chứa icon. */
export function IconTile({ icon, tone = "primary", size = "md" }: { icon: IconName; tone?: Tone; size?: "sm" | "md" }) {
  return (
    <span className={cx("inline-flex shrink-0 items-center justify-center rounded-lg", SIZES[size].box, SOFT_TONES[tone])}>
      <Icon name={icon} size={SIZES[size].icon} />
    </span>
  );
}
