import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { useState } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import { ManagerPicker } from "@/features/subjects/components/ManagerPicker";
import { searchSubjectManagers, type ManagerCandidate, type SearchSubjectManagers } from "@/features/subjects/model/managerSearch";

const manager: ManagerCandidate = { accountId: "m1", displayName: "Chủ nhiệm A", email: "a@example.test", role: "SUBJECT_MANAGER", status: "ACTIVE" };
function Picker({ search }: { search?: SearchSubjectManagers }) {
  const [value, setValue] = useState<ManagerCandidate | null>(null);
  return <ManagerPicker value={value} onChange={setValue} searchManagers={search} />;
}
afterEach(() => { cleanup(); vi.useRealTimers(); });

describe("ManagerPicker boundary", () => {
  it("gọi danh sách tài khoản U01 lọc SUBJECT_MANAGER đang ACTIVE và đổi sang ứng viên", async () => {
    const fetchMock = vi.fn().mockImplementation(async () => new Response(JSON.stringify({
      items: [{ id: "m1", email: "a@example.test", displayName: "Chủ nhiệm A", role: "SUBJECT_MANAGER", status: "ACTIVE" }],
    }), { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    await expect(searchSubjectManagers({ q: " nguyen ", limit: 20 })).resolves.toEqual([manager]);
    const url = new URL(String(fetchMock.mock.calls[0]?.[0]), "http://localhost");
    expect(url.pathname).toBe("/api/v1/admin/accounts");
    expect(Object.fromEntries(url.searchParams)).toEqual({ role: "SUBJECT_MANAGER", status: "ACTIVE", page: "0", size: "20", q: "nguyen" });
    vi.unstubAllGlobals();
  });

  it("tìm lỗi thì báo không tải được, không giả danh sách rỗng", async () => {
    const search = vi.fn<SearchSubjectManagers>().mockRejectedValue(new ApiError({ status: 503 }));
    render(<Picker search={search} />);
    expect(await screen.findByText(/Không tải được danh sách chủ nhiệm/)).toBeInTheDocument();
    fireEvent.focus(screen.getByRole("combobox"));
    expect(screen.queryByRole("option")).not.toBeInTheDocument();
    expect(screen.getByRole("combobox")).toHaveAttribute("placeholder", "Chưa phân công");
  });

  it("chọn rồi bỏ, gửi q và limit20 tới boundary", async () => {
    const search = vi.fn<SearchSubjectManagers>().mockResolvedValue([manager]);
    render(<Picker search={search} />);
    fireEvent.focus(screen.getByRole("combobox"));
    await screen.findByRole("option", { name: /Chủ nhiệm A/ });
    expect(search).toHaveBeenCalledWith({ q: "", limit: 20 });
    fireEvent.click(screen.getByRole("option", { name: /Chủ nhiệm A/ }));
    expect(screen.queryByRole("listbox")).not.toBeInTheDocument();
    expect(screen.getByRole("combobox")).toHaveValue("a@example.test");
    fireEvent.click(screen.getByTestId("subject-manager-clear"));
    expect(screen.getByRole("combobox")).toHaveValue("");
  });

  it("debounce300ms, che kết quả cũ và bỏ response đến muộn", async () => {
    vi.useFakeTimers();
    let oldResolve!: (value: ManagerCandidate[]) => void;
    const search = vi.fn<SearchSubjectManagers>().mockReturnValueOnce(new Promise((resolve) => { oldResolve = resolve; })).mockResolvedValue([manager]);
    render(<Picker search={search} />);
    await act(async () => {});
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "  A  " } });
    expect(screen.queryByRole("option")).not.toBeInTheDocument();
    await act(async () => { vi.advanceTimersByTime(299); });
    expect(search).toHaveBeenCalledTimes(1);
    await act(async () => { vi.advanceTimersByTime(1); });
    expect(search).toHaveBeenLastCalledWith({ q: "A", limit: 20 });
    await act(async () => { oldResolve([{ ...manager, accountId: "old", displayName: "Kết quả cũ" }]); });
    expect(screen.queryByRole("option", { name: /Kết quả cũ/ })).not.toBeInTheDocument();
    expect(screen.getByRole("option", { name: /Chủ nhiệm A/ })).toBeInTheDocument();
  });

  it("empty khác unavailable, retry phục hồi", async () => {
    const search = vi.fn<SearchSubjectManagers>().mockRejectedValueOnce(new Error("provider secret")).mockResolvedValue([]);
    render(<Picker search={search} />);
    await screen.findByText(/Không tải được danh sách chủ nhiệm/);
    expect(screen.queryByText(/provider secret/)).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("subject-manager-retry"));
    fireEvent.focus(screen.getByRole("combobox"));
    await screen.findByText("Không có chủ nhiệm phù hợp");
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it.each([401, 403])("lỗi quyền%s không hiện raw reason hoặc option cũ", async (status) => {
    const search = vi.fn<SearchSubjectManagers>().mockRejectedValue(new ApiError({ status, detail: "provider secret" }));
    render(<Picker search={search} />);
    await waitFor(() => expect(screen.getByRole("alert")).toHaveTextContent(status === 401 ? "đăng nhập lại" : "không có quyền"));
    expect(screen.queryByText(/provider secret/)).not.toBeInTheDocument();
    fireEvent.focus(screen.getByRole("combobox"));
    expect(screen.queryByRole("option")).not.toBeInTheDocument();
  });

  it("phòng adapter trả sai role/status và không hiện quá20 kết quả", async () => {
    const wrong = { ...manager, accountId: "wrong", displayName: "Không hợp lệ", status: "DISABLED" } as unknown as ManagerCandidate;
    const wrongRole = { ...manager, accountId: "wrong-role", displayName: "Sai vai trò", role: "TEACHER" } as unknown as ManagerCandidate;
    const many = Array.from({ length: 22 }, (_, i) => ({ ...manager, accountId: `id${i}` }));
    render(<Picker search={vi.fn<SearchSubjectManagers>().mockResolvedValue([wrong, wrongRole, ...many])} />);
    fireEvent.focus(screen.getByRole("combobox"));
    await waitFor(() => expect(screen.getAllByRole("option")).toHaveLength(20));
    expect(screen.queryByRole("option", { name: /Không hợp lệ|Sai vai trò/ })).not.toBeInTheDocument();
  });
});
