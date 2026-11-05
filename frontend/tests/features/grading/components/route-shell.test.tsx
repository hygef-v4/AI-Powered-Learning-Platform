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
  GradingWorkspacePage: ({ evaluationId, proposalId, status, late }: { evaluationId: string; proposalId?: string; status?: string; late?: string }) => (
    <div data-testid="grading-workspace" data-evaluation-id={evaluationId} data-proposal-id={proposalId} data-status={status} data-late={late} />
  ),
}));

afterEach(cleanup);
describe("teaching evaluation route composition", () => {
  it("forwards scalar navigation filters and ignores duplicate query parameters", async () => {
    const page = await EvaluationPage({ params: Promise.resolve({ evaluationId: "e" }), searchParams: Promise.resolve({ status: "PENDING", late: "true" }) });
    const view = render(page);
    expect(screen.getByTestId("grading-workspace")).toHaveAttribute("data-status", "PENDING");
    expect(screen.getByTestId("grading-workspace")).toHaveAttribute("data-late", "true");
    view.unmount();
    render(await EvaluationPage({ params: Promise.resolve({ evaluationId: "e" }), searchParams: Promise.resolve({ status: ["PENDING", "DRAFT"], late: ["true", "false"] }) }));
    expect(screen.getByTestId("grading-workspace")).not.toHaveAttribute("data-status");
    expect(screen.getByTestId("grading-workspace")).not.toHaveAttribute("data-late");
  });
  it("uses one parent shell with both authorized roles and preserves the evaluation ID", async () => {
    const page = await EvaluationPage({ params: Promise.resolve({ evaluationId: "evaluation-123" }), searchParams: Promise.resolve({ proposalId: "proposal-1" }) });
    render(<TeachingLayout>{page}</TeachingLayout>);
    expect(screen.getAllByTestId("authenticated-shell")).toHaveLength(1);
    expect(screen.getByTestId("authenticated-shell")).toHaveAttribute("data-roles", "TEACHER,SUBJECT_MANAGER");
    expect(screen.getByTestId("grading-workspace")).toHaveAttribute("data-evaluation-id", "evaluation-123");
    expect(screen.getByTestId("grading-workspace")).toHaveAttribute("data-proposal-id", "proposal-1");
  });
});
