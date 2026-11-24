import { describe, expect, it } from "vitest";
import { describeOperation, describeReason, listDetailEntries } from "@/features/audit/model/details";
import type { AuditLogItem } from "@/features/audit/model/types";

const DENIED: AuditLogItem = {
  id: "a1",
  occurredAt: "2026-11-24T16:17:00Z",
  actorId: "u1",
  actorEmail: "giangvien1@demo.alapedu.online",
  action: "ACCESS_DENIED",
  objectType: "COURSE_CLASS",
  objectId: "6f1c2a7d-11e0-4b9b-8f3a-6c2d0a5e1e81",
  objectLabel: "SWD392 · SE1801",
  result: "DENIED",
  reason: "ROLE_DENIED",
  details: { audience: "manager", action: "CLASS_VIEW", actorRole: "TEACHER", subjectId: "s1" },
};

describe("chi tiết nhật ký", () => {
  it("nêu thao tác bị chặn và vai trò trong lý do", () => {
    expect(describeOperation(DENIED)).toBe("Xem lớp");
    expect(describeReason(DENIED)).toBe("Vai trò Giảng viên không được dùng chức năng này");
  });

  it("mã lý do quen thành câu, mã lạ để trống cho bảng tự rút gọn", () => {
    expect(describeReason({ ...DENIED, reason: "OUT_OF_CLASS_SCOPE", details: {} }))
      .toBe("Không phụ trách hoặc không học lớp này");
    expect(describeReason({ ...DENIED, reason: "SOMETHING_NEW", details: {} })).toBeNull();
    expect(describeOperation({ ...DENIED, details: {} })).toBeNull();
  });

  it("dòng chi tiết có mã đối tượng, thao tác, vai trò, màn, mã lý do và khóa còn lại", () => {
    expect(listDetailEntries(DENIED)).toEqual([
      { label: "Mã đối tượng", value: DENIED.objectId },
      { label: "Thao tác bị chặn", value: "Xem lớp" },
      { label: "Vai trò lúc đó", value: "Giảng viên" },
      { label: "Màn đang dùng", value: "Quản lý môn" },
      { label: "Mã lý do", value: "ROLE_DENIED" },
      { label: "subjectId", value: "s1" },
    ]);
  });
});
