import { render, screen, cleanup } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import type { ReactNode } from "react";
import TeachingLayout from "@/app/teaching/layout";
import EvaluationPage from "@/app/teaching/evaluations/[evaluationId]/page";

vi.mock("@/features/identity/shell/components/AuthenticatedLayout", () => ({
  AuthenticatedLayout: ({ roles, children }: { roles: string[]; children: ReactNode }) => (
    <div data-testid="authenticated-shell" data-roles={roles.join(",")}>{children}</div>
  ),
}));
vi.mock("@/features/grading", () => ({
  GradingWorkspacePage: ({ evaluationId }: { evaluationId: string }) => (
    <div data-testid="grading-workspace" data-evaluation-id={evaluationId} />
  ),
}));

afterEach(cleanup);
describe("teaching evaluation route composition", () => {
  it("uses one parent shell with both authorized roles and preserves the evaluation ID", async () => {
    const page = await EvaluationPage({ params: Promise.resolve({ evaluationId: "evaluation-123" }) });
    render(<TeachingLayout>{page}</TeachingLayout>);
    expect(screen.getAllByTestId("authenticated-shell")).toHaveLength(1);
    expect(screen.getByTestId("authenticated-shell")).toHaveAttribute("data-roles", "TEACHER,SUBJECT_MANAGER");
    expect(screen.getByTestId("grading-workspace")).toHaveAttribute("data-evaluation-id", "evaluation-123");
  });
});
