import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api/client";
import { useSession } from "@/features/identity/hooks/session";
import { AccountDetailPage } from "@/features/identity/admin/pages/AccountDetailPage";
import { changeRole, changeStatus, commitImport, createAccount, getAccount, updateAccount, validateImport, type AccountDetail } from "@/features/identity/admin/api";
import { ImportAccountsPanel } from "@/features/identity/admin/components/ImportAccountsPanel";
import { NewAccountDialog } from "@/features/identity/admin/components/NewAccountDialog";

vi.mock("@/features/identity/admin/api", () => ({
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
vi.mock("@/features/identity/hooks/session", () => ({ useSession: vi.fn() }));

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

const TEACHER_ACCOUNT: AccountDetail = { ...ACCOUNT, role: "TEACHER", email: "gv01@fpt.edu.vn", displayName: "Giảng viên 01" };

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
    expect(createAccount).toHaveBeenCalledWith("gv01@fpt.edu.vn", "Giảng viên 01", "TEACHER", undefined);
    expect(screen.queryByLabelText(/^Mã số sinh viên/)).not.toBeInTheDocument();
    expect(screen.queryByText(/Người dùng tự kích hoạt/)).not.toBeInTheDocument();
  });

  it("email đã tồn tại thì báo lỗi rõ", async () => {
    vi.mocked(createAccount).mockRejectedValue(new ApiError({ status: 409, code: "EMAIL_EXISTS" }));
    render(<NewAccountDialog onClose={vi.fn()} onCreated={vi.fn()} />);

    fireEvent.change(screen.getByLabelText(EMAIL), { target: { value: "sv01@fpt.edu.vn" } });
    fireEvent.change(screen.getByLabelText(NAME), { target: { value: "SV" } });
    fireEvent.change(screen.getByLabelText(/^Mã số sinh viên/), { target: { value: " se000001 " } });
    fireEvent.click(screen.getByRole("button", { name: "Thêm tài khoản" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Email đã có tài khoản.");
    expect(createAccount).toHaveBeenCalledWith("sv01@fpt.edu.vn", "SV", "STUDENT", "SE000001");
  });

  it("sinh viên bắt buộc MSSV đúng định dạng; trùng mã thì báo rõ", async () => {
    vi.mocked(createAccount).mockRejectedValue(new ApiError({ status: 409, code: "STUDENT_CODE_EXISTS" }));
    render(<NewAccountDialog onClose={vi.fn()} onCreated={vi.fn()} />);

    fireEvent.change(screen.getByLabelText(EMAIL), { target: { value: "sv02@fpt.edu.vn" } });
    fireEvent.change(screen.getByLabelText(NAME), { target: { value: "SV" } });
    fireEvent.click(screen.getByRole("button", { name: "Thêm tài khoản" }));
    expect(await screen.findByText("Nhập MSSV.")).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(/^Mã số sinh viên/), { target: { value: "S123" } });
    fireEvent.click(screen.getByRole("button", { name: "Thêm tài khoản" }));
    expect(await screen.findByText(/MSSV gồm 2 chữ cái và 6 chữ số/)).toBeInTheDocument();
    expect(createAccount).not.toHaveBeenCalled();

    fireEvent.change(screen.getByLabelText(/^Mã số sinh viên/), { target: { value: "SE000002" } });
    fireEvent.click(screen.getByRole("button", { name: "Thêm tài khoản" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("MSSV này đã được dùng cho tài khoản khác.");
  });
});

describe("ImportAccountsPanel", () => {
  afterEach(() => vi.clearAllMocks());

  it("kiểm tệp rồi xác nhận gửi lại cùng tệp", async () => {
    const rows = [
      { rowNumber: 2, email: "sv01@fpt.edu.vn", displayName: "A", role: "STUDENT", outcome: "VALID" as const, reason: null },
      { rowNumber: 3, email: "x@gmail", displayName: "B", role: "STUDENT", outcome: "REJECTED" as const, reason: "INVALID_EMAIL" },
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
    const onClose = vi.fn();
    render(<ImportAccountsPanel onImported={onImported} onClose={onClose} />);
    const file = new File(["workbook"], "tk.xlsx", { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" });

    expect(screen.getByLabelText("Chọn tệp Excel")).toHaveAttribute("accept", ".xlsx");
    fireEvent.change(screen.getByLabelText("Chọn tệp Excel"), { target: { files: [file] } });

    expect(await screen.findByText("Email không hợp lệ")).toBeInTheDocument();
    expect(validateImport).toHaveBeenCalledWith(file);
    fireEvent.click(screen.getByRole("button", { name: "Nhập 1 tài khoản" }));

    await waitFor(() => expect(onImported).toHaveBeenCalledWith(1));
    expect(commitImport).toHaveBeenCalledWith(file);
    expect(onClose).toHaveBeenCalled();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
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
    expect(updateAccount).toHaveBeenCalledWith("acc-1", "Tên mới", "", 3, undefined);
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
    vi.mocked(getAccount).mockResolvedValue(TEACHER_ACCOUNT);
    render(<AccountDetailPage />);

    fireEvent.click(await screen.findByRole("button", { name: "Đổi vai trò" }));
    const role = within(screen.getByRole("dialog")).getByLabelText(/^Vai trò/);
    expect(role).toHaveValue("TEACHER");
    fireEvent.change(role, { target: { value: "ADMIN" } });
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
    vi.mocked(getAccount).mockResolvedValue(TEACHER_ACCOUNT);
    render(<AccountDetailPage />);

    fireEvent.click(await screen.findByRole("button", { name: "Đổi vai trò" }));
    expect(screen.getAllByRole("button", { name: "Đổi vai trò" })[1]).toBeDisabled();
    const role = within(screen.getByRole("dialog")).getByLabelText(/^Vai trò/);
    expect(role).toHaveValue("TEACHER");
    expect(within(role).queryByRole("option", { name: "Sinh viên" })).not.toBeInTheDocument();
    fireEvent.change(role, { target: { value: "ADMIN" } });
    fireEvent.click(screen.getAllByRole("button", { name: "Đổi vai trò" })[1]!);

    expect(await screen.findByRole("dialog", { name: "Không đổi được vai trò" })).toHaveTextContent("lop-1");
    expect(changeRole).toHaveBeenCalledWith("acc-1", "ADMIN", 3);
  });

  it("không cho tự đổi vai trò hay tự vô hiệu hóa", async () => {
    signedInAs("acc-1");
    vi.mocked(getAccount).mockResolvedValue(TEACHER_ACCOUNT);
    render(<AccountDetailPage />);

    expect(await screen.findByRole("button", { name: "Đổi vai trò" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Vô hiệu hóa tài khoản" })).toBeDisabled();
    expect(changeStatus).not.toHaveBeenCalled();
  });

  it("sinh viên: không có nút đổi vai trò; MSSV đã kích hoạt chỉ đọc", async () => {
    signedInAs("admin-1");
    vi.mocked(getAccount).mockResolvedValue({ ...ACCOUNT, studentCode: "SE000001", studentCodeEditable: false });
    render(<AccountDetailPage />);

    expect(await screen.findByText("Không đổi được vai trò của tài khoản sinh viên.")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Đổi vai trò" })).not.toBeInTheDocument();
    expect(screen.getByLabelText(/^Mã số sinh viên/)).toBeDisabled();
    expect(screen.getByLabelText(/^Mã số sinh viên/)).toHaveValue("SE000001");
  });

  it("sửa MSSV gõ sai khi sinh viên chưa kích hoạt, gửi kèm mã đã chuẩn hóa", async () => {
    signedInAs("admin-1");
    vi.mocked(getAccount).mockResolvedValue({ ...ACCOUNT, status: "PENDING", studentCode: "SE000001", studentCodeEditable: true });
    vi.mocked(updateAccount).mockResolvedValue({ ...ACCOUNT, status: "PENDING", studentCode: "SE000009", studentCodeEditable: true, version: 4 });
    render(<AccountDetailPage />);

    fireEvent.change(await screen.findByLabelText(/^Mã số sinh viên/), { target: { value: "se000009" } });
    fireEvent.click(screen.getByRole("button", { name: "Lưu thay đổi" }));

    await waitFor(() => expect(updateAccount).toHaveBeenCalledWith("acc-1", "Sinh viên 01", "", 3, "SE000009"));
  });

  it("sinh viên cũ chưa có mã chỉ thấy Chưa có MSSV, không gán được", async () => {
    signedInAs("admin-1");
    vi.mocked(getAccount).mockResolvedValue({ ...ACCOUNT, studentCode: null, studentCodeEditable: false });
    render(<AccountDetailPage />);

    expect(await screen.findByLabelText(/^Mã số sinh viên/)).toHaveValue("Chưa có MSSV");
    expect(screen.getByLabelText(/^Mã số sinh viên/)).toBeDisabled();
  });

  it("mở Nhật ký đã lọc theo tài khoản", async () => {
    signedInAs("admin-1");
    render(<AccountDetailPage />);

    expect(await screen.findByRole("link", { name: "Mở Nhật ký của tài khoản" })).toHaveAttribute(
      "href",
      "/admin/audit?actorEmail=sv01%40fpt.edu.vn&actorId=acc-1",
    );
  });
});
