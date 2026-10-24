import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import { useSession } from "../session";
import { AccountDetailPage } from "./AccountDetailPage";
import { changeRole, changeStatus, commitImport, createAccount, getAccount, updateAccount, validateImport, type AccountDetail } from "./api";
import { ImportAccountsPanel } from "./ImportAccountsPanel";
import { NewAccountDialog } from "./NewAccountDialog";

vi.mock("./api", () => ({
  createAccount: vi.fn(),
  getAccount: vi.fn(),
  updateAccount: vi.fn(),
  changeRole: vi.fn(),
  changeStatus: vi.fn(),
  validateImport: vi.fn(),
  commitImport: vi.fn(),
}));
vi.mock("next/navigation", () => ({ useParams: () => ({ id: "acc-1" }) }));

const EMAIL = /^Email trường/;
const NAME = /^Họ và tên/;
vi.mock("../session", () => ({ useSession: vi.fn() }));

const ACCOUNT: AccountDetail = {
  id: "acc-1",
  email: "sv01@fpt.edu.vn",
  displayName: "Sinh viên 01",
  phoneNumber: null,
  role: "STUDENT",
  status: "ACTIVE",
  createdAt: "2026-10-17T08:00:00Z",
  updatedAt: "2026-10-17T08:00:00Z",
  version: 3,
};

function signedInAs(accountId: string) {
  vi.mocked(useSession).mockReturnValue({
    status: "authenticated",
    profile: { accountId, email: "ad@fpt.edu.vn", displayName: "Admin", phoneNumber: null, role: "ADMIN", status: "ACTIVE", homePath: "/admin/accounts" },
    reload: vi.fn(),
    clear: vi.fn(),
  });
}

describe("NewAccountDialog", () => {
  afterEach(() => vi.clearAllMocks());

  it("tạo tài khoản chờ kích hoạt", async () => {
    vi.mocked(createAccount).mockResolvedValue({ ...ACCOUNT, status: "PENDING" });
    const onCreated = vi.fn();
    render(<NewAccountDialog onClose={vi.fn()} onCreated={onCreated} />);

    fireEvent.change(screen.getByLabelText(EMAIL), { target: { value: "gv01@fpt.edu.vn" } });
    fireEvent.change(screen.getByLabelText(NAME), { target: { value: "Giảng viên 01" } });
    fireEvent.change(screen.getByLabelText(/^Vai trò/), { target: { value: "TEACHER" } });
    fireEvent.click(screen.getByRole("button", { name: "Thêm tài khoản" }));

    await waitFor(() => expect(onCreated).toHaveBeenCalled());
    expect(createAccount).toHaveBeenCalledWith("gv01@fpt.edu.vn", "Giảng viên 01", "TEACHER");
    expect(screen.getByText(/Người dùng tự kích hoạt bằng email trường và mã OTP/)).toBeInTheDocument();
  });

  it("email đã tồn tại thì báo lỗi rõ", async () => {
    vi.mocked(createAccount).mockRejectedValue(new ApiError({ status: 409, code: "EMAIL_EXISTS" }));
    render(<NewAccountDialog onClose={vi.fn()} onCreated={vi.fn()} />);

    fireEvent.change(screen.getByLabelText(EMAIL), { target: { value: "sv01@fpt.edu.vn" } });
    fireEvent.change(screen.getByLabelText(NAME), { target: { value: "SV" } });
    fireEvent.click(screen.getByRole("button", { name: "Thêm tài khoản" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Email đã có tài khoản.");
  });
});

describe("ImportAccountsPanel", () => {
  afterEach(() => vi.clearAllMocks());

  it("kiểm tệp rồi xác nhận gửi lại cùng tệp", async () => {
    const rows = [
      { rowNumber: 2, email: "sv01@fpt.edu.vn", displayName: "A", role: "STUDENT", outcome: "VALID" as const, reason: null },
      { rowNumber: 3, email: "x@gmail.com", displayName: "B", role: "STUDENT", outcome: "REJECTED" as const, reason: "EMAIL_DOMAIN_NOT_ALLOWED" },
    ];
    vi.mocked(validateImport).mockResolvedValue({ fileChecksum: "c", rows, validCount: 1, rejectedCount: 1, committed: false });
    vi.mocked(commitImport).mockResolvedValue({
      fileChecksum: "c",
      rows: [{ ...rows[0]!, outcome: "CREATED" }, rows[1]!],
      validCount: 1,
      rejectedCount: 1,
      committed: true,
    });
    const onImported = vi.fn();
    render(<ImportAccountsPanel onImported={onImported} onClose={vi.fn()} />);
    const file = new File(["email,display_name,role\n"], "tk.csv", { type: "text/csv" });

    fireEvent.change(screen.getByLabelText("Chọn tệp CSV"), { target: { files: [file] } });

    expect(await screen.findByText("Lỗi: email không thuộc tên miền trường")).toBeInTheDocument();
    expect(validateImport).toHaveBeenCalledWith(file);
    fireEvent.click(screen.getByRole("button", { name: "Xác nhận nhập 1 tài khoản" }));

    await waitFor(() => expect(onImported).toHaveBeenCalledWith(1));
    expect(commitImport).toHaveBeenCalledWith(file);
    expect(screen.getByRole("status")).toHaveTextContent("Đã tạo 1 tài khoản");
  });
});

describe("AccountDetailPage", () => {
  beforeEach(() => vi.mocked(getAccount).mockResolvedValue(ACCOUNT));
  afterEach(() => vi.clearAllMocks());

  it("sửa thông tin gửi kèm version; xung đột thì báo và tải lại", async () => {
    signedInAs("admin-1");
    vi.mocked(updateAccount).mockRejectedValue(new ApiError({ status: 409, code: "VERSION_CONFLICT" }));
    render(<AccountDetailPage />);

    fireEvent.change(await screen.findByLabelText(NAME), { target: { value: "Tên mới" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu thay đổi" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Dữ liệu đã được thay đổi ở phiên khác");
    expect(updateAccount).toHaveBeenCalledWith("acc-1", "Tên mới", "", 3);
    await waitFor(() => expect(getAccount).toHaveBeenCalledTimes(2));
  });

  it("bị chặn đổi vai trò thì hiện mã, tên môn và lớp thay cho ID", async () => {
    signedInAs("admin-1");
    vi.mocked(changeRole).mockRejectedValue(
      new ApiError({
        status: 409, code: "ROLE_CHANGE_BLOCKED", managedSubjects: ["sub-uuid"], taughtClasses: ["class-uuid"],
        subjects: [{ id: "sub-uuid", code: "SWR302", name: "Software Requirements" }],
        classes: [{ id: "class-uuid", code: "SE1801", name: "Lớp SE1801" }],
      } as never),
    );
    render(<AccountDetailPage />);

    fireEvent.click(await screen.findByRole("button", { name: "Đổi vai trò" }));
    fireEvent.change(screen.getByLabelText(/^Vai trò mới/), { target: { value: "TEACHER" } });
    fireEvent.click(screen.getAllByRole("button", { name: "Đổi vai trò" })[1]!);

    const dialog = await screen.findByRole("dialog", { name: "Không đổi được vai trò" });
    expect(within(dialog).getByRole("link", { name: "SWR302" })).toHaveAttribute("href", "/admin/subjects/sub-uuid");
    expect(dialog).toHaveTextContent("Software Requirements");
    expect(dialog).toHaveTextContent("SE1801");
    expect(dialog).toHaveTextContent("Lớp SE1801");
    expect(dialog).not.toHaveTextContent("class-uuid");
  });

  it("đổi vai trò cần xác nhận; bị chặn thì hiện môn/lớp còn phụ trách", async () => {
    signedInAs("admin-1");
    vi.mocked(changeRole).mockRejectedValue(
      new ApiError({ status: 409, code: "ROLE_CHANGE_BLOCKED", managedSubjects: [], taughtClasses: ["lop-1"] } as never),
    );
    render(<AccountDetailPage />);

    fireEvent.click(await screen.findByRole("button", { name: "Đổi vai trò" }));
    expect(screen.getByRole("dialog")).toHaveTextContent("sẽ bị đăng xuất");
    fireEvent.change(screen.getByLabelText(/^Vai trò mới/), { target: { value: "TEACHER" } });
    fireEvent.click(screen.getAllByRole("button", { name: "Đổi vai trò" })[1]!);

    expect(await screen.findByRole("dialog", { name: "Không đổi được vai trò" })).toHaveTextContent("lop-1");
    expect(changeRole).toHaveBeenCalledWith("acc-1", "TEACHER", 3);
  });

  it("không cho tự đổi vai trò hay tự vô hiệu hóa", async () => {
    signedInAs("acc-1");
    render(<AccountDetailPage />);

    expect(await screen.findByRole("button", { name: "Đổi vai trò" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Vô hiệu hóa tài khoản" })).toBeDisabled();
    expect(changeStatus).not.toHaveBeenCalled();
  });

  it("mở Nhật ký audit đã lọc theo tài khoản", async () => {
    signedInAs("admin-1");
    render(<AccountDetailPage />);

    expect(await screen.findByRole("link", { name: "Mở Nhật ký audit của tài khoản" })).toHaveAttribute(
      "href",
      "/admin/audit?objectType=ACCOUNT&objectId=acc-1",
    );
  });
});
