import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import * as XLSX from "xlsx";
import { GroupImportDialog } from "@/features/groups/components/GroupImportDialog";
import type { GroupSetView, Person } from "@/features/groups/model/types";

function createFile(rows: unknown[][]) {
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, XLSX.utils.aoa_to_sheet(rows), "Nhóm");
  const bytes = XLSX.write(workbook, { type: "array", bookType: "xlsx" }) as ArrayBuffer;
  const file = new File([bytes], "nhom.xlsx");
  Object.defineProperty(file, "arrayBuffer", { value: async () => bytes });
  return file;
}

const people: Person[] = [
  { accountId: "a", name: "An", email: "an@fpt.edu.vn", studentCode: "SE180001" },
  { accountId: "b", name: "Bình", email: "binh@fpt.edu.vn", studentCode: "SE180002" },
  { accountId: "e", name: "Em", email: "em@fpt.edu.vn", studentCode: "SE180005" },
];

const SET: GroupSetView = {
  classId: "c1",
  classStatus: "OPEN",
  version: 7,
  groups: [{
    id: "g1",
    name: "Nhóm 1",
    leaderId: "e",
    members: [{ accountId: "e", name: "Em", email: "em@fpt.edu.vn", active: true, leader: true }],
    used: false,
  }],
  ungrouped: people.slice(0, 2),
  pendingRequests: [],
};

describe("GroupImportDialog", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("previews new groups, needs a leader where the file marks none, then saves the whole set", async () => {
    const bodies: unknown[] = [];
    vi.stubGlobal("fetch", vi.fn((_input: RequestInfo | URL, init?: RequestInit) => {
      bodies.push(JSON.parse(String(init?.body)));
      return Promise.resolve(new Response(JSON.stringify({ ...SET, version: 8 }), { status: 200, headers: { "Content-Type": "application/json" } }));
    }));
    const onSaved = vi.fn();
    render(<GroupImportDialog classId="c1" set={SET} people={people} sizes={{ minSize: 2, maxSize: 5 }}
      onClose={vi.fn()} onSaved={onSaved} />);

    fireEvent.change(screen.getByTestId("group-import-input"), { target: { files: [createFile([
      ["MSSV", "Họ tên", "Tên nhóm", "Trưởng nhóm"],
      ["SE180001", "An", "Nhóm Mới", ""],
      ["SE180002", "Bình", "Nhóm Mới", ""],
      ["SE180005", "Em", "Nhóm Khác", "x"],
    ])] } });

    expect(await screen.findByText(/SE180005 đã có nhóm/)).toBeInTheDocument();
    expect(screen.getByText("File chưa đánh dấu trưởng nhóm.")).toBeInTheDocument();
    const save = screen.getByTestId("group-import-save");
    expect(save).toBeDisabled();

    fireEvent.change(screen.getByLabelText("Trưởng nhóm của Nhóm Mới"), { target: { value: "b" } });
    expect(save).toBeEnabled();
    fireEvent.click(save);

    await waitFor(() => expect(onSaved).toHaveBeenCalled());
    expect(bodies[0]).toEqual({
      version: 7,
      groups: [
        { key: "g1", id: "g1", name: "Nhóm 1", leaderId: "e", memberIds: ["e"] },
        { key: "import-1", id: null, name: "Nhóm Mới", leaderId: "b", memberIds: ["a", "b"] },
      ],
    });
  });

  it("shows the reading error for a file without the needed columns", async () => {
    render(<GroupImportDialog classId="c1" set={SET} people={people} onClose={vi.fn()} onSaved={vi.fn()} />);
    fireEvent.change(screen.getByTestId("group-import-input"), { target: { files: [createFile([["MSSV"], ["SE180001"]])] } });
    expect(await screen.findByRole("alert")).toHaveTextContent("Tên nhóm");
  });
});
