import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AssignmentSettingsPanel, readSettings } from "@/features/assignments/components/AssignmentSettingsPanel";
import type { AssignmentDetail } from "@/features/assignments/model/types";

const detail = {
  id: "a", classId: null, subjectId: "s", scope: "CLASS", title: "Bài", instructions: "",
  gradingMode: "GRADED", submissionMode: "INDIVIDUAL", status: "DRAFT", skeleton: { blocks: [] }, parts: [],
  requiredDiagrams: {}, referenceMaterialIds: [], diagramEnabled: true, activityMonitoringEnabled: false,
  activityNotice: null, groupSupported: true, permissions: { canEditContent: true, canManage: true },
} as unknown as AssignmentDetail;

afterEach(cleanup);

describe("assignment settings type", () => {
  it("shows one Type choice instead of separate Format and Mode", () => {
    render(<AssignmentSettingsPanel detail={detail} value={readSettings(detail)} onChange={vi.fn()} readOnly={false} />);
    expect(screen.getByRole("radiogroup", { name: "Loại" })).toBeInTheDocument();
    expect(screen.queryByRole("radiogroup", { name: "Hình thức" })).toBeNull();
    expect(screen.queryByRole("radiogroup", { name: "Chế độ" })).toBeNull();
    expect(screen.getByRole("radio", { name: "Cá nhân" })).toBeChecked();
  });

  it.each([
    ["Nhóm", { submissionMode: "GROUP", gradingMode: "GRADED" }],
    ["Luyện tập", { submissionMode: "INDIVIDUAL", gradingMode: "PRACTICE" }],
  ])("switches to %s", (label, expected) => {
    const onChange = vi.fn();
    render(<AssignmentSettingsPanel detail={detail} value={readSettings(detail)} onChange={onChange} readOnly={false} />);
    fireEvent.click(screen.getByRole("radio", { name: label }));
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining(expected));
  });

  it("switches a group assignment back to individual graded", () => {
    const group = { ...detail, submissionMode: "GROUP" } as AssignmentDetail;
    const onChange = vi.fn();
    render(<AssignmentSettingsPanel detail={group} value={readSettings(group)} onChange={onChange} readOnly={false} />);
    expect(screen.getByRole("radio", { name: "Nhóm" })).toBeChecked();
    fireEvent.click(screen.getByRole("radio", { name: "Cá nhân" }));
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ submissionMode: "INDIVIDUAL", gradingMode: "GRADED" }));
  });

  it("disables Group when the class cannot use group assignments", () => {
    const noGroups = { ...detail, groupSupported: false } as AssignmentDetail;
    render(<AssignmentSettingsPanel detail={noGroups} value={readSettings(noGroups)} onChange={vi.fn()} readOnly={false} />);
    expect(screen.getByRole("radio", { name: "Nhóm" })).toBeDisabled();
  });
});
