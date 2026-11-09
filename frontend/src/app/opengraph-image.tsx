/* eslint-disable @next/next/no-img-element -- ImageResponse (Satori) chỉ hiểu thẻ <img>, không dùng được next/image. */
import { readFile } from "node:fs/promises";
import { join } from "node:path";
import { ImageResponse } from "next/og";
import { INK_ORANGE, INK_TEAL } from "@/shared/ui/Logo";
import { SITE_HOST, SITE_NAME, SITE_TAGLINE } from "@/shared/siteMetadata";

export const alt = `${SITE_NAME} - ${SITE_TAGLINE}`;
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

const INK = "#27272a";
const MUTED = "#62626b";
const PRIMARY = "#466a96";
const PRIMARY_SOFT = "#eef2f7";
const HIGHLIGHTS = ["Lớp học & tài liệu", "Bài tập & đề thi", "Trợ lý AI"];

// Satori không đọc được woff2 nên dùng bản TTF tĩnh tách từ font của giao diện; ảnh được tạo lúc build.
const FONT_DIRECTORY = join(process.cwd(), "assets", "og");

async function loadOgFonts() {
  const fontFiles = [
    { weight: 500, subset: "latin" },
    { weight: 500, subset: "vietnamese" },
    { weight: 800, subset: "latin" },
    { weight: 800, subset: "vietnamese" },
  ] as const;
  return Promise.all(
    fontFiles.map(async ({ weight, subset }) => ({
      name: "Plus Jakarta Sans",
      data: await readFile(join(FONT_DIRECTORY, `plus-jakarta-sans-${weight}-${subset}.ttf`)),
      weight,
      style: "normal" as const,
    })),
  );
}

function createLogoDataUrl() {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="6 6 52 52"><rect x="9.5" y="41.5" width="45" height="12" rx="6" fill="none" stroke="${INK_ORANGE}" stroke-width="4"/><ellipse cx="25" cy="47.5" rx="8" ry="2.8" fill="${INK_TEAL}"/><g transform="translate(27 44) rotate(38)"><rect x="-3.2" y="-38" width="6.4" height="24" rx="3.2" fill="${INK_ORANGE}"/><rect x="-3.8" y="-15" width="7.6" height="3.6" rx="1" fill="${INK_TEAL}"/><path d="M-3.8-11.6C-3.8-5-1.2-1 0 1.8C1.2-1 3.8-5 3.8-11.6Z" fill="${INK_TEAL}"/></g></svg>`;
  return `data:image/svg+xml;base64,${Buffer.from(svg).toString("base64")}`;
}

export default async function OpengraphImage() {
  const logoUrl = createLogoDataUrl();
  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          position: "relative",
          background: "#ffffff",
          fontFamily: "Plus Jakarta Sans",
          color: INK,
        }}
      >
        <img src={logoUrl} width={520} height={520} alt="" style={{ position: "absolute", right: -70, bottom: -90, opacity: 0.08 }} />
        <div style={{ position: "absolute", left: 0, top: 0, bottom: 0, width: 16, display: "flex", flexDirection: "column" }}>
          <div style={{ flex: 1, background: INK_ORANGE }} />
          <div style={{ flex: 1, background: INK_TEAL }} />
        </div>
        <div style={{ display: "flex", flexDirection: "column", justifyContent: "space-between", padding: "72px 88px", width: "100%" }}>
          <div style={{ display: "flex", alignItems: "center", gap: 22 }}>
            <img src={logoUrl} width={92} height={92} alt="" />
            <div style={{ display: "flex", alignItems: "center", gap: 14, fontSize: 52, fontWeight: 800, letterSpacing: -1.5 }}>
              <span>Nền tảng</span>
              <span style={{ background: PRIMARY, color: "#ffffff", borderRadius: 14, padding: "0 16px 6px" }}>học tập</span>
            </div>
          </div>
          <div style={{ display: "flex", flexDirection: "column", gap: 22 }}>
            <div style={{ fontSize: 68, fontWeight: 800, lineHeight: 1.12, letterSpacing: -2, maxWidth: 900 }}>
              Dạy và học hiệu quả hơn cùng trợ lý AI
            </div>
            <div style={{ display: "flex", gap: 14 }}>
              {HIGHLIGHTS.map((highlight) => (
                <span
                  key={highlight}
                  style={{ fontSize: 26, fontWeight: 500, color: PRIMARY, background: PRIMARY_SOFT, borderRadius: 999, padding: "10px 24px" }}
                >
                  {highlight}
                </span>
              ))}
            </div>
          </div>
          <div style={{ display: "flex", fontSize: 26, fontWeight: 500, color: MUTED }}>{SITE_HOST}</div>
        </div>
      </div>
    ),
    { ...size, fonts: await loadOgFonts() },
  );
}
