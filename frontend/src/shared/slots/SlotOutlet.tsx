import type { Slot } from "./registry";

/** Vẽ mọi component đã đăng ký vào slot theo thứ tự; slot trống thì không vẽ gì. */
export function SlotOutlet<P extends object>({ slot, props }: { slot: Slot<P>; props: P }) {
  return (
    <>
      {slot.entries().map(({ key, component: Component }) => (
        <Component key={key} {...props} />
      ))}
    </>
  );
}
