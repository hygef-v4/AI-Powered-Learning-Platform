# U14 — Phê duyệt kế hoạch triển khai UC63–65

Ngày 2026-10-21, branch `announcements-and-notifications`.

Kế hoạch: [u14-announcements-and-notifications-code-generation-plan.md](u14-announcements-and-notifications-code-generation-plan.md), gồm11 bước và quyết định C01–C08.

## Question 1 — Triển khai theo kế hoạch cụ thể

Phê duyệt triển khai đầy đủ F1–F9/UC63–65 theo kế hoạch, gồm các thay đổi giới hạn sau?

- U01: thêm action quyền và API cursor `listActiveAdmins` cùng provider/stub/test để không bỏ sót Admin thứ21 trở đi; không đổi login/quyền cũ.
- Frontend: dùng local UI-reference đã có, thêm react-markdown + rehype-sanitize cho markdown an toàn; áp riêng cấu hình nonce SEC-004 đã kiểm từ U13, không merge toàn bộ U13.
- U03: dùng contracts/job routing hiện có; queue event và retry topology U14 riêng, không sửa job dispatcher của U03.
- U07/U08/U12: giữ contract/provider owner, cài consumer U14 và ghi rõ gate nhắc hạn/group/SSE nhiều instance khi provider thật chưa có. Không hứa SMTP exactly-once qua crash window của IMail.
- Không tự commit/push, không gửi email thật, không thay stage/UC acceptance khi gate chưa qua.

A) Duyệt kế hoạch và C01–C08; triển khai đủ11 bước, dừng ở review code trước khi chuyển unit/stage. (Đề xuất)

B) Yêu cầu sửa kế hoạch; ghi nội dung muốn điều chỉnh.

E) Phương án khác; mô tả sau Answer.

[Answer]: a
