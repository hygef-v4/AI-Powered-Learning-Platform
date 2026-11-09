import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, expect, it, vi } from "vitest";
import { apiFetch } from "@/shared/api/client";
import { LearningMaterialPage } from "@/features/materials/pages/LearningMaterialPage";
import type { Material } from "@/features/materials/model/types";

const { context } = vi.hoisted(() => ({ context: { student: false } }));
const material = { id: "m", subjectId: "s", moduleId: "module", title: "Chương 1", status: "ACTIVE", enabled: true,
  canManage: true, version: 3, description: "Nội dung chính",
  descriptionDocument: { blocks: [{ type: "UL", align: "LEFT", indent: 0, runs: [{ text: "Nội dung chính", bold: true }] }] },
} as Material;
vi.mock("@/shared/api/client", async (original) => ({ ...(await original<typeof import("@/shared/api/client")>()), apiFetch: vi.fn() }));
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));
vi.mock("@/features/materials/hooks/useMaterialResource", () => ({ useMaterialResource: () => ({ data: material, reload: vi.fn() }) }));
vi.mock("@/features/materials/components/MaterialPreview", () => ({ MaterialPreview: () => null }));
vi.mock("@/features/materials/components/ModulePlace", () => ({
  useModuleScope: () => context.student ? { kind: "student", classId: "c" } : { kind: "subject", subjectId: "s" },
  useModulePlace: () => ({}), ModuleNeighbours: () => null,
}));

beforeEach(() => { context.student = false; vi.mocked(apiFetch).mockReset(); });

it("renders formatted description and persists visibility with its current version", async () => {
  vi.mocked(apiFetch).mockResolvedValue({ ...material, enabled: false, version: 4 });
  const { container } = render(<LearningMaterialPage id="m" />);
  expect(container.querySelector("ul li strong")).toHaveTextContent("Nội dung chính");
  fireEvent.click(screen.getByRole("switch", { name: "Bật học liệu" }));
  await waitFor(() => expect(apiFetch).toHaveBeenCalledWith("/api/v1/materials/m", expect.objectContaining({ method: "PATCH" })));
  expect(JSON.parse(String(vi.mocked(apiFetch).mock.calls[0]![1]!.body))).toMatchObject({ enabled: false, version: 3 });
  await waitFor(() => expect(screen.getByRole("switch", { name: "Bật học liệu" })).not.toBeChecked());
});

it("does not expose management controls to a student", () => {
  context.student = true;
  render(<LearningMaterialPage id="m" classId="c" />);
  expect(screen.queryByRole("switch")).not.toBeInTheDocument();
  expect(screen.queryByTestId("material-edit-open")).not.toBeInTheDocument();
});
