import { msg } from "@/i18n/msg";

export const CONTRIBUTION_LEVELS = [
  {
    value: "EXCEEDS_EXPECTATIONS",
    label: msg("grading.model.contributionLevels.exceedsExpectations"),
    description: msg("grading.model.contributionLevels.completedTheAssignedPartWell"),
  },
  {
    value: "MEETS_EXPECTATIONS",
    label: msg("grading.model.contributionLevels.meetsRequirements"),
    description: msg("grading.model.contributionLevels.fullyCompletedTheAssignedPart"),
  },
  {
    value: "PARTIALLY_MEETS",
    label: msg("grading.model.contributionLevels.partlyMet"),
    description: msg("grading.model.contributionLevels.contributedButSomeContentIs"),
  },
  {
    value: "NEEDS_SUPPORT",
    label: msg("grading.model.contributionLevels.needsALotOfSupport"),
    description: msg("grading.model.contributionLevels.neededALotOfGuidance"),
  },
  {
    value: "NOT_COMPLETED",
    label: msg("grading.model.contributionLevels.notCompleted"),
    description: msg("grading.model.contributionLevels.didNotCompleteTheAssigned"),
  },
] as const;

export type ContributionLevel = (typeof CONTRIBUTION_LEVELS)[number]["value"];
