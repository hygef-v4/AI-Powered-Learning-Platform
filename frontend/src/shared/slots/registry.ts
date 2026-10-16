// Slot frontend: unit trước khai báo chỗ gắn, unit sau đăng ký nội dung vào đó, unit trước không import unit sau.

import type { ComponentType } from "react";

export type RegistryEntry = {
  key: string;
  order?: number;
};

export type Registry<T extends RegistryEntry> = {
  readonly name: string;
  register(entry: T): void;
  entries(): T[];
};

export function createRegistry<T extends RegistryEntry>(name: string): Registry<T> {
  const items = new Map<string, T>();
  return {
    name,
    // Đăng ký lại cùng key thì thay mục cũ (an toàn khi hot reload).
    register(entry) {
      items.set(entry.key, entry);
    },
    entries() {
      return [...items.values()].sort((a, b) => (a.order ?? 0) - (b.order ?? 0) || a.key.localeCompare(b.key));
    },
  };
}

/** Mục gắn một component vào slot; props do unit khai báo slot quyết định. */
export type SlotEntry<P> = RegistryEntry & {
  component: ComponentType<P>;
};

export type Slot<P> = Registry<SlotEntry<P>>;

export function createSlot<P>(name: string): Slot<P> {
  return createRegistry<SlotEntry<P>>(name);
}
