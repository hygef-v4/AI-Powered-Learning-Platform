export const SITE_NAME = "Nền tảng học tập";
export const SITE_TAGLINE = "Dạy và học hiệu quả hơn cùng trợ lý AI";
export const SITE_TITLE = `${SITE_NAME} – ${SITE_TAGLINE}`;
export const SITE_DESCRIPTION =
  "Quản lý lớp học, tài liệu, bài tập và đề thi; luyện tập và nhận phản hồi tức thì với trợ lý AI.";
export const SITE_URL = process.env.SITE_URL ?? "https://alapedu.online";
export const SITE_HOST = new URL(SITE_URL).host;
