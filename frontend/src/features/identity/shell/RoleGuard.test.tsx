import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import { getProfile } from "../api";
import { SessionProvider } from "../session";
import type { Profile } from "../types";
import { RoleGuard } from "./RoleGuard";

const replace = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ replace }) }));
vi.mock("../api", () => ({ getProfile: vi.fn() }));

const getProfileMock = vi.mocked(getProfile);

function profile(role: Profile["role"], homePath: string): Profile {
  return { accountId: "1", email: "a@fpt.edu.vn", displayName: "A", phoneNumber: null, role, status: "ACTIVE", homePath };
}

function renderGuarded() {
  render(
    <SessionProvider>
      <RoleGuard roles={["ADMIN"]}>
        <p>Nội dung quản trị</p>
      </RoleGuard>
    </SessionProvider>,
  );
}

describe("RoleGuard", () => {
  afterEach(() => {
    vi.clearAllMocks();
    window.history.replaceState({}, "", "/");
  });

  it("đúng role thì hiện nội dung", async () => {
    getProfileMock.mockResolvedValue(profile("ADMIN", "/admin/accounts"));
    renderGuarded();
    expect(await screen.findByText("Nội dung quản trị")).toBeInTheDocument();
  });

  it("sai role thì chuyển về trang của mình, không hiện nội dung", async () => {
    getProfileMock.mockResolvedValue(profile("STUDENT", "/profile"));
    renderGuarded();
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/profile"));
    expect(screen.queryByText("Nội dung quản trị")).not.toBeInTheDocument();
  });

  it("chưa đăng nhập thì về /login", async () => {
    getProfileMock.mockRejectedValue(new ApiError({ status: 401 }));
    renderGuarded();
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
  });

  it("hết phiên giữa chừng thì về /login kèm trang đang mở để quay lại", async () => {
    window.history.replaceState({}, "", "/admin/accounts?page=2");
    getProfileMock.mockRejectedValue(new ApiError({ status: 401 }));
    renderGuarded();
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login?next=%2Fadmin%2Faccounts%3Fpage%3D2"));
  });
});
