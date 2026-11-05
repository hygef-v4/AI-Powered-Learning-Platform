"use client";

import type { ReactNode } from "react";
import { ButtonLink, PageHeader } from "@/shared/ui";
import { McpClientSetup } from "../components/SetupGuides";
import { MCP_TOOL_GROUPS } from "../model/mcpGuide";

const CONTENTS = [
  ["tong-quan", "MCP dùng để làm gì?"],
  ["ket-noi", "Kết nối ứng dụng"],
  ["su-dung", "Cách sử dụng"],
  ["cong-cu", "Danh sách tool"],
  ["xu-ly-loi", "Khi gặp lỗi"],
  ["tham-khao", "Tài liệu chính thức"],
] as const;

export function McpGuidePage() {
  return <div className="flex min-w-0 flex-col gap-8">
    <PageHeader title="Tài liệu MCP" backHref="/ai/connection?tab=mcp"
      subtitle="Kết nối agent của bạn với Nền tảng học tập."
      actions={<ButtonLink href="/ai/connection?tab=mcp" size="sm">Quản lý kết nối MCP</ButtonLink>} />
    <div className="grid min-w-0 gap-8 lg:grid-cols-[200px_minmax(0,1fr)] lg:gap-12">
      <nav aria-label="Mục lục tài liệu MCP" className="self-start rounded-xl border border-line bg-surface p-4 lg:sticky lg:top-24">
        <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-muted">Trong tài liệu này</p>
        <ol className="grid gap-1 sm:grid-cols-2 lg:grid-cols-1">
          {CONTENTS.map(([id, label], index) => <li key={id}>
            <a href={`#${id}`} className="flex min-w-0 gap-3 rounded-lg px-2 py-2 text-[13px] text-body transition-colors hover:bg-sunken hover:text-ink focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus">
              <span className="text-faint" aria-hidden="true">{index + 1}</span>{label}
            </a>
          </li>)}
        </ol>
      </nav>
      <article aria-label="Hướng dẫn MCP" className="min-w-0 space-y-9 text-[15px] leading-7 text-body">
        <GuideSection id="tong-quan" title="MCP dùng để làm gì?">
          <p>MCP cho phép bạn trò chuyện trong Codex, Claude Code, OpenCode hoặc Antigravity để làm việc với dữ liệu được phép trên hệ thống. Agent dùng mô hình của chính ứng dụng đó; token MCP không phải khóa API AI và không sử dụng khóa API bạn đã lưu ở đây.</p>
          <p>Bạn nêu yêu cầu và trao đổi với agent ở ứng dụng bên ngoài. Khi tạo đề hoặc chấm bài, agent trả liên kết mở đúng bản nháp/đề xuất trên nền tảng để bạn xem và quyết định.</p>
          <dl className="grid gap-4 rounded-xl border border-line bg-surface p-5 sm:grid-cols-2">
            <div><dt className="font-semibold text-ink">Chỉ đọc</dt><dd className="mt-1 text-sm">Xem dữ liệu mà vai trò cho phép. Không tạo nháp hay lưu đề xuất.</dd></div>
            <div><dt className="font-semibold text-ink">Đọc và ghi</dt><dd className="mt-1 text-sm">Có thêm các tool ghi được vai trò cho phép, như tạo nháp hoặc lưu góp ý. Không tự chốt/công bố điểm.</dd></div>
          </dl>
          <ul className="list-disc space-y-2 pl-5">
            <li>Giảng viên: làm việc trong lớp được phân công. Chủ nhiệm môn: thêm quyền trên nội dung môn phụ trách; chấm bài vẫn cần quyền dạy lớp.</li>
            <li>Sinh viên: xin góp ý cho bài Practice đã nộp của chính mình, không tự chấm điểm chính thức hoặc dùng AI cho bài nhóm.</li>
            <li>Admin không có quyền kết nối MCP. Danh sách tool thực tế phụ thuộc cả vai trò và quyền token, được trả qua get_my_capabilities.</li>
          </ul>
        </GuideSection>

        <GuideSection id="ket-noi" title="Kết nối ứng dụng">
          <p>Trong Quản lý kết nối MCP, bấm <strong>Kết nối agent mới</strong>, đặt nhãn dễ nhớ và chọn quyền phù hợp. Sau đó chọn ứng dụng bên dưới. Cách nhanh nhất là sao chép prompt tiếng Anh và nhờ agent thiết lập; bạn cũng có thể mở file cấu hình để ghép thủ công.</p>
          <div className="min-w-0 rounded-xl border border-line bg-surface p-4 sm:p-5"><McpClientSetup /></div>
          <p className="text-sm text-muted">Hướng dẫn không bắt buộc dùng terminal. Với Codex, cấu hình cá nhân dùng chung cho các giao diện hỗ trợ cùng cấu hình; với Claude Code, đây là cấu hình của Claude Code, không phải file cấu hình Claude Desktop.</p>
          <p className="text-sm">Giữ token trong cấu hình cá nhân. Trang tài liệu chỉ hiển thị chỗ giữ chỗ; không dán token vào cuộc trò chuyện, ảnh chụp, URL hay Git. Nếu lộ token, thu hồi kết nối đó và tạo token mới.</p>
          <p className="text-sm">Để ngắt kết nối, tắt hoặc xóa server nen-tang-hoc-tap trong ứng dụng agent. Nếu không còn sử dụng, quay lại Quản lý kết nối MCP và thu hồi token; các kết nối dùng token đó sẽ không xác thực được nữa.</p>
        </GuideSection>

        <GuideSection id="su-dung" title="Cách sử dụng">
          <div className="space-y-5">
            <div>
              <h3 className="font-semibold text-ink">1. Tạo hoặc chỉnh đề Assignment</h3>
              <ol className="mt-2 list-decimal space-y-2 pl-5">
                <li>Nêu rõ lớp/môn, chủ đề, số câu, độ khó, tổng điểm, cá nhân/nhóm và Practice/Graded. Gửi ảnh hoặc nội dung nguồn nếu cần; yêu cầu agent hỏi lại phần còn thiếu.</li>
                <li>Agent đọc quyền, hướng dẫn ra đề trên server và schema hiện hành; tìm đúng lớp/môn trước khi tạo. Chỉnh đề đã có thì phải đọc version mới nhất.</li>
                <li>Agent tạo DRAFT cùng đề xuất nội dung/rubric và trả liên kết review. Bản nháp vừa tạo có thể chưa có nội dung đã áp dụng.</li>
                <li>Mở liên kết, xem đề và rubric, rồi bấm <strong>Áp dụng đề xuất</strong>. Chỉnh sửa và lưu trên giao diện; việc giao/xếp lịch bài vẫn do bạn quyết định.</li>
              </ol>
            </div>
            <div>
              <h3 className="font-semibold text-ink">2. Đề xuất chấm bài theo rubric</h3>
              <ol className="mt-2 list-decimal space-y-2 pl-5">
                <li>Cho agent biết lớp và bài cần chấm. Agent liệt kê bài nộp để bạn chọn một bài hoặc một nhóm bài.</li>
                <li>Agent đọc hướng dẫn chấm, đề, rubric và toàn bộ snapshot bài nộp. Nội dung dài phải đọc đủ các trang cùng fingerprint trước khi ghi đề xuất.</li>
                <li>Yêu cầu điểm từng mục, bằng chứng, lý do và cờ cần giảng viên xem lại khi thiếu thông tin. Không suy đoán phần sơ đồ/ảnh không đọc được.</li>
                <li>Mở liên kết review ở trang chấm bài, kiểm tra từng mục và giải thích. Áp dụng vào bản nháp rồi lưu, chốt, công bố trên hệ thống theo quyền; MCP không thực hiện quyết định chính thức.</li>
              </ol>
              <p className="mt-3 rounded-lg border border-line bg-sunken p-4 text-sm">MCP không bảo đảm AI chấm đúng rubric. Hệ thống kiểm tra quyền, snapshot, cấu trúc và việc đọc đủ ngữ cảnh; điều đó không chứng minh model hiểu đúng bài. Giảng viên cần kiểm tra điểm lẻ, bằng chứng và các mục được đánh dấu cần review trước khi chốt.</p>
            </div>
            <div>
              <h3 className="font-semibold text-ink">3. Quiz, ngân hàng câu hỏi và luyện tập</h3>
              <p className="mt-2">Quiz dùng đề xuất câu hỏi riêng; ngân hàng câu hỏi dành cho Chủ nhiệm môn và vẫn cần duyệt. Sinh viên có thể xin góp ý riêng sau khi nộp bài Practice; góp ý không sửa bài và không tạo điểm vào sổ điểm.</p>
            </div>
          </div>
          <details className="rounded-xl border border-line bg-surface p-4 text-sm">
            <summary className="cursor-pointer font-semibold text-ink">Prompt mẫu để bắt đầu (tiếng Anh)</summary>
            <div className="mt-4 space-y-4">
              <div><p className="font-semibold">Tạo đề</p><pre className="mt-2 whitespace-pre-wrap break-words rounded-lg bg-sunken p-3 font-mono text-xs leading-6">{`Create a graded individual assignment for class SE1933 on software architecture: 3 UML questions, 10 points total, medium difficulty. Confirm the exact class, ask about any missing requirements, and draft questions with a matching rubric. Use the server's AUTHOR_ASSIGNMENT workflow. Return the review link; do not publish.`}</pre></div>
              <div><p className="font-semibold">Chấm bài</p><pre className="mt-2 whitespace-pre-wrap break-words rounded-lg bg-sunken p-3 font-mono text-xs leading-6">{`List the submitted work for the class and assignment I specify. Use GRADE_WITH_RUBRIC, read every current context page, and propose per-item scores with evidence and explanations. Flag uncertainty for teacher review. Save proposals only and return review links; do not save, finalize or publish official grades.`}</pre></div>
            </div>
          </details>
          <p className="text-sm text-muted">Không cần tải skill hoặc hook về máy. Giảng viên dùng hướng dẫn trên server qua read_agent_workflow; skill/hook cục bộ chỉ là lựa chọn bổ sung, không cấp thêm quyền.</p>
        </GuideSection>

        <GuideSection id="cong-cu" title="Danh sách tool">
          <p>Tool là thao tác agent gọi trên hệ thống. <strong>Chỉ đọc</strong> không tạo bản ghi nghiệp vụ; <strong>Ghi nháp/góp ý</strong> cần token Đọc và ghi. Đây là danh mục theo chức năng; quyền hiện hành trong get_my_capabilities và kiểm tra từng đối tượng mới quyết định tool nào dùng được.</p>
          <div className="space-y-8">
            {MCP_TOOL_GROUPS.map((group) => <section key={group.title} aria-label={group.title}>
              <h3 className="font-semibold text-ink">{group.title}</h3>
              <p className="mt-1 text-sm text-muted">{group.audience}</p>
              <dl className="mt-3 divide-y divide-line rounded-xl border border-line bg-surface px-4 sm:px-5">
                {group.tools.map((tool) => <div key={tool.name} className="grid min-w-0 gap-2 py-4 xl:grid-cols-[minmax(0,0.9fr)_minmax(0,1.4fr)] xl:gap-5">
                  <dt className="min-w-0"><code className="break-words font-mono text-[13px] font-semibold text-ink">{tool.name}</code><span className="mt-1 block text-xs text-muted">{tool.write ? "Ghi nháp/góp ý" : "Chỉ đọc"}</span></dt>
                  <dd className="min-w-0 text-sm leading-6">{tool.description}</dd>
                </div>)}
              </dl>
            </section>)}
          </div>
        </GuideSection>

        <GuideSection id="xu-ly-loi" title="Khi gặp lỗi">
          <dl className="space-y-4 text-sm">
            <div><dt className="font-semibold text-ink">401 hoặc không xác thực được</dt><dd>Kiểm tra token còn hạn/chưa thu hồi và header Authorization đúng. Không dùng token giữ chỗ; tạo token mới nếu đã mất token cũ.</dd></div>
            <div><dt className="font-semibold text-ink">Không thấy tool hoặc bị từ chối quyền</dt><dd>Đọc lại capabilities; token Chỉ đọc không có tool ghi. Vai trò Chủ nhiệm môn không tự có quyền chấm mọi lớp. Sau khi đổi quyền hoặc cập nhật server, tải lại danh sách MCP/tools trong ứng dụng.</dd></div>
            <div><dt className="font-semibold text-ink">Không truy cập được địa chỉ server</dt><dd>localhost là máy chạy agent. Agent ở container, máy khác hoặc dịch vụ từ xa phải có địa chỉ nền tảng mà nó truy cập được; không mặc định dùng localhost của trình duyệt. Với kết nối qua mạng, dùng địa chỉ HTTPS của nền tảng.</dd></div>
            <div><dt className="font-semibold text-ink">Context chưa đọc đủ hoặc không còn hiện hành</dt><dd>Với MCP_CONTEXT_READ_REQUIRED, đọc lại từ trang đầu; với VERSION_CONFLICT, SUBMISSION_CHANGED hoặc đề xuất stale, tải dữ liệu mới và chấm lại. Không đổi fingerprint/version để lách kiểm tra. Điểm đã chốt/công bố không thể sửa bằng MCP.</dd></div>
            <div><dt className="font-semibold text-ink">Hết thời gian chờ khi tạo nháp hoặc lưu đề xuất</dt><dd>Kiểm tra xem thao tác đã thành công chưa. Tool hỗ trợ requestRef phải thử lại với cùng requestRef và nội dung không đổi; không tự tạo yêu cầu mới. Tool không có requestRef cần kiểm tra đề xuất trên giao diện trước khi ghi tiếp.</dd></div>
            <div><dt className="font-semibold text-ink">Tạo xong nhưng bài vẫn trống</dt><dd>Mở đúng reviewUrl agent trả về và áp dụng đề xuất. DRAFT/proposal không đồng nghĩa nội dung đã được áp dụng hoặc bài đã giao.</dd></div>
          </dl>
        </GuideSection>

        <GuideSection id="tham-khao" title="Tài liệu chính thức">
          <p className="text-sm">Menu và nơi lưu cấu hình có thể thay đổi theo phiên bản. Với Antigravity, ưu tiên mở cấu hình từ chính ứng dụng để xác nhận đúng file.</p>
          <ul className="list-disc space-y-2 pl-5 text-sm">
            {[
              ["Codex: kết nối MCP", "https://developers.openai.com/codex/mcp"],
              ["Claude Code: MCP và phạm vi cấu hình", "https://code.claude.com/docs/en/mcp"],
              ["OpenCode: MCP servers và xác thực", "https://opencode.ai/docs/mcp-servers/"],
              ["Antigravity: thiết lập MCP trong ứng dụng", "https://codelabs.developers.google.com/getting-started-google-antigravity#6"],
            ].map(([label, href]) => <li key={href}><a href={href} target="_blank" rel="noreferrer" className="text-primary underline decoration-primary/40 underline-offset-4 hover:decoration-primary focus-visible:outline-2 focus-visible:outline-focus">{label}</a></li>)}
          </ul>
        </GuideSection>
      </article>
    </div>
  </div>;
}

function GuideSection({ id, title, children }: { id: string; title: string; children: ReactNode }) {
  return <section id={id} aria-labelledby={`${id}-title`} className="min-w-0 scroll-mt-24 border-t border-line pt-7 first:border-0 first:pt-0">
    <h2 id={`${id}-title`} className="mb-4 text-xl font-semibold leading-7 text-ink">{title}</h2>
    <div className="space-y-4">{children}</div>
  </section>;
}
