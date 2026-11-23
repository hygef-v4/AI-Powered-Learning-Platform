/** Hình người que của sơ đồ use case UML, vẽ bằng `currentColor`; trình đọc màn hình bỏ qua. */
export function UmlActor({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 40 60"
      aria-hidden="true"
      className={className}
      fill="none"
      stroke="currentColor"
      strokeWidth="2.5"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <circle cx="20" cy="10" r="7.5" />
      <path d="M20 17.5 V38 M6 26 H34 M20 38 L8 56 M20 38 L32 56" />
    </svg>
  );
}
