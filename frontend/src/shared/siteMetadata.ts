import { msg } from "@/i18n/msg";

export const SITE_NAME = msg("shared.siteMetadata.learningPlatform");
export const SITE_TAGLINE = msg("shared.siteMetadata.teachAndLearnBetterWith");
export const SITE_TITLE = `${SITE_NAME} – ${SITE_TAGLINE}`;
export const SITE_DESCRIPTION =
  msg("shared.siteMetadata.manageClassesMaterialsAssignmentsAnd");
export const SITE_URL = process.env.SITE_URL ?? "https://alapedu.online";
export const SITE_HOST = new URL(SITE_URL).host;
