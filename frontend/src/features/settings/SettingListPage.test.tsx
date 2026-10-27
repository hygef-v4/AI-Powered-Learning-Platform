import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { SettingListPage } from "./SettingListPage";
import type { SettingItem, SettingsOverview } from "./types";

vi.mock("next/link", () => ({
  default: ({ href, children, ...rest }: { href: string; children: React.ReactNode }) => (
    <a href={href} {...rest}>
      {children}
    </a>
  ),
}));

function item(overrides: Partial<SettingItem>): SettingItem {
  return {
    key: "backup.enabled",
    group: "BACKUP",
    name: "Bật sao lưu",
    description: "Sao lưu cơ sở dữ liệu mỗi ngày",
    type: "BOOLEAN",
    value: false,
    defaultValue: false,
    constraints: {},
    version: 2,
    updatedBy: "Phòng Đào tạo",
    updatedAt: "2026-10-05T01:30:00Z",
    ...overrides,
  };
}

const MAX_UPLOAD = item({
  key: "files.maxUploadMb",
  group: "FILES",
  name: "Dung lượng tối đa",
  description: "",
  type: "INTEGER",
  value: 20,
  defaultValue: 20,
  constraints: { min: 1, max: 100, unit: "MB" },
});

const OVERVIEW: SettingsOverview = {
  groups: [
    { group: "FILES", items: [MAX_UPLOAD] },
    { group: "BACKUP", items: [item({}), item({ key: "backup.dailyAt", name: "Giờ chạy hằng ngày", type: "TIME_OF_DAY", value: "02:00", defaultValue: "02:00" })] },
  ],
  sections: [
    { sectionKey: "ai-connections", title: "API AI của trường", summary: "Gemini: đang bật", available: true },
    { sectionKey: "teacher-quota", title: "Hạn mức AI giảng viên", summary: null, available: true },
  ],
};

function backend() {
  const patches: { url: string; body: unknown }[] = [];
  vi.stubGlobal(
    "fetch",
    vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (init?.method === "PATCH") {
        const body = JSON.parse(String(init.body));
        patches.push({ url, body });
        return Promise.resolve(Response.json({ ...MAX_UPLOAD, value: body.value, version: 3 }));
      }
      return Promise.resolve(Response.json(OVERVIEW));
    }),
  );
  return patches;
}

describe("SettingListPage", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("một thẻ, mỗi mục một dòng; không hiện người sửa, giờ sửa hay dòng mô tả nhóm", async () => {
    backend();
    render(<SettingListPage />);
    expect(await screen.findByText("Dung lượng tối đa")).toBeInTheDocument();
    expect(screen.queryByText("Phòng Đào tạo")).not.toBeInTheDocument();
    expect(screen.queryByText(/Sao lưu cơ sở dữ liệu lên Google Drive/)).not.toBeInTheDocument();
    expect(screen.getAllByRole("tooltip")[0]).toHaveTextContent("Sao lưu cơ sở dữ liệu mỗi ngày");
    expect(screen.getAllByText("Kết nối AI")).toHaveLength(1);
    expect(screen.getByRole("link", { name: /Kết nối AI/ })).toHaveAttribute("href", "/admin/settings/sections/ai-connections");
    const titles = screen.getAllByRole("heading", { level: 2 }).map((h) => h.textContent);
    expect(titles.slice(0, 2)).toEqual(["AI", "Tệp"]);
  });

  it("giờ chạy hằng ngày bị khóa khi chưa bật sao lưu", async () => {
    backend();
    render(<SettingListPage />);
    expect(await screen.findByLabelText("Giờ chạy hằng ngày")).toBeDisabled();
  });

  it("nút Lưu chỉ hiện khi sửa; giới hạn hợp lệ chỉ hiện khi nhập sai", async () => {
    const patches = backend();
    render(<SettingListPage />);
    const field = await screen.findByLabelText("Dung lượng tối đa");
    expect(screen.queryByRole("button", { name: "Lưu" })).not.toBeInTheDocument();

    fireEvent.change(field, { target: { value: "500" } });
    expect(screen.getByRole("alert")).toHaveTextContent("Giá trị phải từ 1 đến 100");
    expect(screen.getByRole("button", { name: "Lưu" })).toBeDisabled();

    fireEvent.change(field, { target: { value: "50" } });
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Lưu" }));
    await waitFor(() => expect(patches).toHaveLength(1));
    expect(patches[0]).toEqual({ url: "/api/v1/admin/settings/files.maxUploadMb", body: { value: 50, version: 2 } });
    await waitFor(() => expect(screen.queryByRole("button", { name: "Lưu" })).not.toBeInTheDocument());
  });
});
