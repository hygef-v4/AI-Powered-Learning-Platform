import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SettingGroupTable } from "./SettingGroupTable";
import type { SettingItem } from "./types";

const push = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));

function item(overrides: Partial<SettingItem>): SettingItem {
  return {
    key: "backup.enabled",
    group: "BACKUP",
    name: "Bật sao lưu",
    description: "",
    type: "BOOLEAN",
    value: true,
    defaultValue: false,
    constraints: {},
    version: 2,
    updatedBy: "Phòng Đào tạo",
    updatedAt: "2026-10-05T01:30:00Z",
    ...overrides,
  };
}

describe("SettingGroupTable", () => {
  beforeEach(() => push.mockReset());

  it("hiện tên nhóm, giá trị dễ đọc, người sửa và giờ Việt Nam", () => {
    const daily = item({ key: "backup.dailyAt", name: "Giờ chạy hằng ngày", type: "TIME_OF_DAY", value: null, updatedBy: null, updatedAt: null });
    render(<SettingGroupTable group={{ group: "BACKUP", items: [item({}), daily] }} />);

    expect(screen.getByRole("heading", { name: "Backup" })).toBeInTheDocument();
    expect(screen.getByText("Bật")).toBeInTheDocument();
    expect(screen.getByText("Phòng Đào tạo")).toBeInTheDocument();
    expect(screen.getByText("05/10 08:30")).toBeInTheDocument();
    expect(screen.getByText("Chưa cấu hình")).toBeInTheDocument();
  });

  it("bấm dòng mở Setting Detail của mục đó", () => {
    render(<SettingGroupTable group={{ group: "BACKUP", items: [item({})] }} />);

    fireEvent.click(screen.getByText("Phòng Đào tạo"));

    expect(push).toHaveBeenCalledWith("/admin/settings/backup.enabled");
  });
});
