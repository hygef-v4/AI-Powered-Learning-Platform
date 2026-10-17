/** Logo "Nền tảng học tập": ô vuông xanh đậm với mũ tốt nghiệp. */
export function BrandLogo() {
  return (
    <div className="flex items-center gap-2">
      <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-[#1f2a6b] text-white">
        <svg viewBox="0 0 24 24" className="h-5 w-5" fill="currentColor" aria-hidden="true">
          <path d="M12 3 1 9l11 6 9-4.91V17h2V9L12 3Zm-6 9.18v4L12 20l6-3.82v-4L12 15.5l-6-3.32Z" />
        </svg>
      </span>
      <span className="text-sm font-semibold text-slate-900">Nền tảng học tập</span>
    </div>
  );
}
