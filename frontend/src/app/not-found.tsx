import { ErrorPage } from "./ErrorPage";

export default function NotFound() {
  return (
    <ErrorPage
      code="404"
      title="Không tìm thấy trang"
      description="Đường dẫn này không tồn tại hoặc đã được thay đổi. Hãy kiểm tra lại liên kết hoặc quay về trang chủ."
    />
  );
}
