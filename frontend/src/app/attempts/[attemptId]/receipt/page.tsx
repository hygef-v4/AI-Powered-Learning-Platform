import { ReceiptPage } from "@/features/assignmentwork/ReceiptPage";

// U08: biên nhận sau khi nộp.
export default async function Page({ params }: { params: Promise<{ attemptId: string }> }) {
  const { attemptId } = await params;
  return <ReceiptPage attemptId={attemptId} />;
}
