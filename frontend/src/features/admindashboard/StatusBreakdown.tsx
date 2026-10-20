import {Section} from "@/shared/ui/Section";
import {TableFrame, Th, Td} from "@/shared/ui/Table";
import type {Role} from "@/shared/auth/roles";
import {formatCount} from "./CountCard";
import type {AdminDashboardView, Section as DataSection} from "./types";

const roles: Record<Role, string> = {ADMIN: "Quản trị viên", SUBJECT_MANAGER: "Chủ nhiệm môn", TEACHER: "Giảng viên", STUDENT: "Sinh viên"};

function CountsTable<T extends string>({title, counts, labels}: {title: string; counts: DataSection<Record<T, number>>; labels: Record<T, string>}) {
  return <div><h3 className="mb-2 font-semibold">{title}</h3>
    {counts.availability === "UNAVAILABLE" ? <p>Không khả dụng</p> : <TableFrame>
      <thead><tr><Th>Trạng thái</Th><Th>Số lượng</Th></tr></thead>
      <tbody>{(Object.keys(labels) as T[]).map(status => <tr key={status}><Td>{labels[status]}</Td><Td>{formatCount(counts.data[status])}</Td></tr>)}</tbody>
    </TableFrame>}
  </div>;
}

export function StatusBreakdown({value}: {value: AdminDashboardView}) {
  return <Section title="Chi tiết theo trạng thái" description="Tính theo yêu cầu · không chứa dữ liệu cá nhân">
    <div><h3 className="mb-2 font-semibold">Tài khoản theo vai trò</h3>
      {value.accounts.availability === "UNAVAILABLE" ? <p>Không khả dụng</p> : <TableFrame>
        <thead><tr><Th>Vai trò</Th><Th>Hoạt động</Th><Th>Chờ kích hoạt</Th><Th>Vô hiệu hóa</Th></tr></thead>
        <tbody>{(Object.keys(roles) as Role[]).map(role => {
          const counts = value.accounts.data!.byRoleAndStatus[role];
          return <tr key={role}><Td>{roles[role]}</Td><Td>{formatCount(counts.ACTIVE)}</Td><Td>{formatCount(counts.PENDING)}</Td><Td>{formatCount(counts.DISABLED)}</Td></tr>;
        })}</tbody>
      </TableFrame>}
    </div>
    <div className="grid gap-4 md:grid-cols-2">
      <CountsTable title="Môn học" counts={value.subjects} labels={{ACTIVE: "Hoạt động", ARCHIVED: "Lưu trữ"}}/>
      <CountsTable title="Lớp học" counts={value.classes} labels={{DRAFT: "Nháp", OPEN: "Đang mở", ARCHIVED: "Lưu trữ"}}/>
    </div>
  </Section>;
}
