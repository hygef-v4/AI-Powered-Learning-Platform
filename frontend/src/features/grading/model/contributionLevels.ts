import { msg } from "@/i18n/msg";

export const CONTRIBUTION_LEVELS = [
  {
    value: "EXCEEDS_EXPECTATIONS",
    label: msg("grading.model.contributionLevels.vuotKyVong"),
    description: msg("grading.model.contributionLevels.hoanThanhTotPhanDuocGiao"),
  },
  {
    value: "MEETS_EXPECTATIONS",
    label: msg("grading.model.contributionLevels.datYeuCau"),
    description: msg("grading.model.contributionLevels.hoanThanhDayDuPhanDuoc"),
  },
  {
    value: "PARTIALLY_MEETS",
    label: msg("grading.model.contributionLevels.datMotPhan"),
    description: msg("grading.model.contributionLevels.coDongGopNhungConThieu"),
  },
  {
    value: "NEEDS_SUPPORT",
    label: msg("grading.model.contributionLevels.canHoTroNhieu"),
    description: msg("grading.model.contributionLevels.canDuocHuongDanNhacNho"),
  },
  {
    value: "NOT_COMPLETED",
    label: msg("grading.model.contributionLevels.khongHoanThanh"),
    description: msg("grading.model.contributionLevels.khongHoanThanhPhanDuocGiao"),
  },
] as const;

export type ContributionLevel = (typeof CONTRIBUTION_LEVELS)[number]["value"];
