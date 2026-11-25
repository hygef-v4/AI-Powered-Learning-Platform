import { render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import { getProfile } from "@/features/identity/api";
import { LandingPage, landingFaqs } from "@/features/landing/pages/LandingPage";
import { SignedInRedirect } from "@/features/landing/components/SignedInRedirect";
import { buildStructuredData, serializeStructuredData } from "@/features/landing/structuredData";
import { buildTranslator } from "@/i18n/msg";

const replace = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ replace, push: vi.fn() }) }));
vi.mock("@/features/identity/api", () => ({ getProfile: vi.fn() }));

const getProfileMock = vi.mocked(getProfile);

function renderLanding(locale: "en" | "vi" = "vi") {
  getProfileMock.mockRejectedValue(new ApiError({ status: 401, code: "UNAUTHENTICATED" }));
  const t = buildTranslator(locale);
  const data = serializeStructuredData(
    buildStructuredData({ name: "Learning Platform", description: "Mô tả", language: locale, faqs: landingFaqs(t) }),
  );
  return render(<LandingPage t={t} locale={locale} structuredData={data} nonce="n1" />);
}

describe("LandingPage", () => {
  afterEach(() => vi.clearAllMocks());

  it("H1 có tên ALAPEdu; mọi nút bắt đầu và đăng nhập trỏ tới /login", () => {
    renderLanding();
    const h1 = screen.getByRole("heading", { level: 1 });
    expect(h1).toHaveTextContent("ALAPEdu");
    expect(h1).toHaveTextContent("Mở khoá tương lai của việc học.");
    const signIns = screen.getAllByRole("link", { name: /Đăng nhập|Bắt đầu|Dùng thử/ });
    expect(signIns.length).toBeGreaterThanOrEqual(3);
    for (const link of signIns) expect(link).toHaveAttribute("href", "/login");
    expect(screen.getByRole("link", { name: "Chính sách quyền riêng tư" })).toHaveAttribute("href", "/privacy");
  });

  it("có đủ năm tính năng chính, đúng các từ khóa giao bài, vẽ sơ đồ, chấm điểm, tạo đề, làm bài nhóm", () => {
    renderLanding();
    for (const title of ["Giao bài dễ dàng", "Vẽ sơ đồ trực tuyến", "Chấm điểm bằng AI", "Tạo đề bằng AI", "Làm bài nhóm"]) {
      expect(screen.getByRole("heading", { level: 3, name: title })).toBeInTheDocument();
    }
  });

  it("dịch theo ngôn ngữ của lượt tải trang", () => {
    renderLanding("en");
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Unlock the future of learning.");
    expect(screen.getAllByRole("link", { name: "Sign in" })[0]).toHaveAttribute("href", "/login");
    expect(screen.getByRole("button", { name: "English" })).toHaveAttribute("aria-pressed", "true");
  });

  it("nhãn lợi ích là một danh sách thật; trang không còn phần đánh giá", () => {
    renderLanding();
    const pills = screen.getByRole("list", { name: "Lợi ích của ALAP" });
    expect(within(pills).getAllByRole("listitem")).toHaveLength(18);
    expect(document.getElementById("reviews")).toBeNull();
  });

  it("chèn JSON-LD có nonce, có hỏi đáp FAQPage và không có đánh giá", () => {
    const { container } = renderLanding();
    const script = container.querySelector('script[type="application/ld+json"]');
    expect(script).toHaveAttribute("nonce", "n1");
    const data = JSON.parse(script!.textContent!);
    const types = data["@graph"].map((node: { "@type": string }) => node["@type"]);
    expect(types).toEqual(["Organization", "WebSite", "SoftwareApplication", "FAQPage"]);
    expect(data["@graph"][0].name).toBe("ALAPEdu");
    expect(data["@graph"][0].alternateName).toContain("alapedu");
    expect(data["@graph"][3].mainEntity[0].name).toBe("ALAPEdu là gì?");
    expect(JSON.stringify(data)).not.toMatch(/Review|AggregateRating/);
    expect(serializeStructuredData({ text: "</script><b>" })).not.toContain("<");
  });
});

describe("SignedInRedirect", () => {
  afterEach(() => vi.clearAllMocks());

  it("đã đăng nhập thì chuyển tới trang chủ theo role", async () => {
    getProfileMock.mockResolvedValue({ homePath: "/classes" } as Awaited<ReturnType<typeof getProfile>>);
    render(<SignedInRedirect />);
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/classes"));
  });

  it.each([
    ["chưa đăng nhập", new ApiError({ status: 401, code: "UNAUTHENTICATED" })],
    ["mất mạng", new TypeError("Failed to fetch")],
  ])("%s thì giữ nguyên landing", async (_label, error) => {
    getProfileMock.mockRejectedValue(error);
    render(<SignedInRedirect />);
    await waitFor(() => expect(getProfileMock).toHaveBeenCalled());
    await Promise.resolve();
    expect(replace).not.toHaveBeenCalled();
  });
});
