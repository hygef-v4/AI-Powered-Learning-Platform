/** Ghép class, bỏ giá trị rỗng. */
export function cx(...classes: Array<string | false | null | undefined>): string {
  return classes.filter(Boolean).join(" ");
}
