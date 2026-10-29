import Link from "next/link";
import { Icon } from "@/shared/ui/Icon";
import { IconTile } from "@/shared/ui/IconTile";
import { Pill } from "@/shared/ui/Pill";
import { settingsSectionRegistry } from "./slots";
import type { SettingsSection } from "./types";

/** Thẻ phần cài đặt do unit khác lưu (API AI chung, quota giảng viên của U06); chỉ hiện tóm tắt đã che. */
export function DelegatedSectionCard({ section }: { section: SettingsSection }) {
  const entry = settingsSectionRegistry.entries().find((e) => e.key === section.sectionKey);
  const body = (
    <>
      <IconTile icon={entry?.icon ?? "tune"} tone={entry?.tone ?? "primary"} />
      <div className="min-w-0 flex-1">
        <h2 className="text-sm font-bold text-ink">{section.title}</h2>
        {section.available ? (
          section.summary && <p className="mt-[3px] text-xs text-muted">{section.summary}</p>
        ) : (
          <div className="mt-2">
            <Pill tone="warning">Không khả dụng, thử lại sau</Pill>
          </div>
        )}
      </div>
      {section.available && <Icon name="chevron_right" className="text-muted" />}
    </>
  );
  const box = "flex items-start gap-3 rounded-2xl border border-line bg-white p-[18px]";
  if (!section.available) return <div className={box}>{body}</div>;
  return (
    <Link
      href={`/admin/settings/sections/${encodeURIComponent(section.sectionKey)}`}
      className={`${box} transition-colors hover:border-line-strong hover:bg-canvas`}
    >
      {body}
    </Link>
  );
}
