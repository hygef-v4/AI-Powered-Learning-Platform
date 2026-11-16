import { Alert, Pill } from "@/shared/ui";
import { msg } from "@/i18n/msg";

export type ActivityInterval = { start: string; end: string | null; certainty: "KNOWN" | "UNKNOWN" };
export type ActivitySummary = {
  policyState: "DISABLED" | "NO_DATA" | "AVAILABLE" | "EXPIRED";
  awayCount: number;
  knownAwayDuration: string | number;
  intervals: ActivityInterval[];
  unknownGaps: number;
  reliabilityNote: string;
};

/** Thời lượng ISO-8601 (PT1M5S) hoặc số giây thành "1 phút 5 giây". */
export function formatDuration(value: string | number): string {
  let seconds = 0;
  if (typeof value === "number") {
    seconds = Math.round(value);
  } else {
    const match = /^PT(?:(\d+)H)?(?:(\d+)M)?(?:([\d.]+)S)?$/.exec(value);
    if (match) seconds = Number(match[1] ?? 0) * 3600 + Number(match[2] ?? 0) * 60 + Math.round(Number(match[3] ?? 0));
  }
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const rest = seconds % 60;
  return [hours && msg("shared.activity.ActivityTimeline.gio", { hours: String(hours) }), minutes && msg("shared.activity.ActivityTimeline.phut", { minutes: String(minutes) }), (rest || (!hours && !minutes)) && msg("shared.activity.ActivityTimeline.giay", { rest: String(rest) })]
    .filter(Boolean).join(" ");
}

function formatTime(value: string | null) {
  return value ? new Date(value).toLocaleTimeString("vi-VN", { timeZone: "Asia/Ho_Chi_Minh" }) : "?";
}

/** Dòng thời gian rời trang cho giảng viên (U09, U11): chỉ là dấu hiệu, không kết luận gian lận (BR-U08-53). */
export function ActivityTimeline({ summary }: { summary: ActivitySummary }) {
  if (summary.policyState === "DISABLED") return <p className="text-xs text-muted">{msg("shared.activity.ActivityTimeline.baiKhongBatGhiNhanHoat")}</p>;
  if (summary.policyState === "EXPIRED") return <p className="text-xs text-muted">{msg("shared.activity.ActivityTimeline.duLieuDaQuaThoiHan")}</p>;
  if (summary.policyState === "NO_DATA") return <p className="text-xs text-muted">{msg("shared.activity.ActivityTimeline.chuaCoDuLieuHoatDong")}</p>;
  return (
    <div className="flex flex-col gap-2">
      <p className="text-[13px] text-ink">
        {msg("shared.activity.ActivityTimeline.roiTrang")}{summary.awayCount} {msg("shared.activity.ActivityTimeline.lanTongThoiGianDaBiet")}{formatDuration(summary.knownAwayDuration)}
      </p>
      <ul className="flex flex-col gap-1 text-xs">
        {summary.intervals.map((interval, index) => (
          <li key={index} className="flex items-center gap-2">
            <Pill tone={interval.certainty === "KNOWN" ? "warning" : "neutral"}>{interval.certainty === "KNOWN" ? msg("shared.activity.ActivityTimeline.roiTrang2") : msg("shared.activity.ActivityTimeline.khongRo")}</Pill>
            {formatTime(interval.start)} – {formatTime(interval.end)}
          </li>
        ))}
      </ul>
      <Alert tone="info">{summary.reliabilityNote}</Alert>
    </div>
  );
}
