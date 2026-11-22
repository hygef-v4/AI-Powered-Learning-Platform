import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import { getProfile } from "@/features/identity/api";
import { LandingPage } from "@/features/landing/pages/LandingPage";
import { SignedInRedirect } from "@/features/landing/components/SignedInRedirect";
import { buildStructuredData, serializeStructuredData } from "@/features/landing/structuredData";
import { buildTranslator } from "@/i18n/msg";

const replace = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ replace, push: vi.fn() }) }));
vi.mock("@/features/identity/api", () => ({ getProfile: vi.fn() }));

const getProfileMock = vi.mocked(getProfile);

function renderLanding(locale: "en" | "vi" = "en") {
  getProfileMock.mockRejectedValue(new ApiError({ status: 401, code: "UNAUTHENTICATED" }));
  const data = serializeStructuredData(buildStructuredData({ name: "Learning Platform", description: "Mô tả", language: locale }));
  return render(<LandingPage t={buildTranslator(locale)} structuredData={data} nonce="n1" />);
}

describe("LandingPage", () => {
  afterEach(() => vi.clearAllMocks());

  it("có tiêu đề chính, các phần tính năng và nút đăng nhập trỏ tới /login", () => {
    renderLanding();
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Draw your UML diagrams.");
    for (const title of ["Diagram assignments", "AI grading proposals", "Quizzes and practice", "Connect your AI agent"]) {
      expect(screen.getByRole("heading", { level: 3, name: title })).toBeInTheDocument();
    }
    const signIns = screen.getAllByRole("link", { name: "Sign in" });
    expect(signIns.length).toBeGreaterThanOrEqual(2);
    for (const link of signIns) expect(link).toHaveAttribute("href", "/login");
    expect(screen.getByRole("link", { name: "Privacy policy" })).toHaveAttribute("href", "/privacy");
  });

  it("dịch theo ngôn ngữ của lượt tải trang", () => {
    renderLanding("vi");
    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Vẽ sơ đồ UML.");
    expect(screen.getAllByRole("link", { name: "Đăng nhập" })[0]).toHaveAttribute("href", "/login");
  });

  it("chèn JSON-LD có nonce, không đóng được thẻ script", () => {
    const { container } = renderLanding();
    const script = container.querySelector('script[type="application/ld+json"]');
    expect(script).toHaveAttribute("nonce", "n1");
    const data = JSON.parse(script!.textContent!);
    expect(data["@graph"].map((node: { "@type": string }) => node["@type"])).toEqual(["Organization", "WebSite", "SoftwareApplication"]);
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
