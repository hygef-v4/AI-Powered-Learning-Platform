import Link from "next/link";
import type { SettingsSection } from "./types";

/** Phần cài đặt do unit khác lưu (API AI chung, quota giảng viên của U06); chỉ hiện tóm tắt đã che. */
export function DelegatedSectionCard({ section }: { section: SettingsSection }) {
  return (
    <div className="rounded border border-slate-200 p-4">
      <h3 className="font-medium">{section.title}</h3>
      {section.available ? (
        <>
          <p className="mt-1 text-sm text-slate-600">{section.summary}</p>
          <Link
            className="mt-2 inline-block text-sm text-blue-700 hover:underline"
            href={`/admin/settings/sections/${encodeURIComponent(section.sectionKey)}`}
          >
            Xem và sửa
          </Link>
        </>
      ) : (
        <p className="mt-1 text-sm text-amber-700">Không khả dụng, thử lại sau</p>
      )}
    </div>
  );
}
