import {render, screen, waitFor} from "@testing-library/react";
import {afterEach, expect, it, vi} from "vitest";
import Page from "@/app/admin/page";
import {navEntries} from "@/features/identity/slots";
import {getProfile} from "@/features/identity/api";
import {SessionProvider} from "@/features/identity/hooks/session";
import {RoleGuard} from "@/features/identity/shell/components/RoleGuard";
import {routes} from "../model/testFixtures";
import "@/features/admindashboard/register";

const replace = vi.fn();
vi.mock("next/navigation", () => ({useRouter: () => ({replace})}));
vi.mock("@/features/identity/api", () => ({getProfile: vi.fn()}));
afterEach(() => {vi.unstubAllGlobals(); vi.clearAllMocks();});
function renderRoute() {
  render(<SessionProvider><RoleGuard roles={["ADMIN"]}><Page/></RoleGuard></SessionProvider>);
}
it("uses the existing unique ADMIN navigation entry and mounts the dashboard", async () => {
  vi.mocked(getProfile).mockResolvedValue({accountId: "a", email: "a@example.test", displayName: "Admin", phoneNumber: null, role: "ADMIN", status: "ACTIVE", homePath: "/admin"});
  vi.stubGlobal("fetch", routes());
  renderRoute();
  expect(await screen.findByTestId("dashboard-accounts")).toHaveTextContent("3");
  expect(navEntries.entries().filter(entry => entry.key === "admin-dashboard")).toEqual([
    expect.objectContaining({href: "/admin", roles: ["ADMIN"]}),
  ]);
});
it("does not mount dashboard or fetch counts for a non-ADMIN session", async () => {
  vi.mocked(getProfile).mockResolvedValue({accountId: "s", email: "s@example.test", displayName: "Student", phoneNumber: null, role: "STUDENT", status: "ACTIVE", homePath: "/classes"});
  const request = vi.fn();
  vi.stubGlobal("fetch", request);
  renderRoute();
  await waitFor(() => expect(replace).toHaveBeenCalledWith("/classes"));
  expect(screen.queryByTestId("dashboard-accounts")).not.toBeInTheDocument();
  expect(request).not.toHaveBeenCalled();
});
