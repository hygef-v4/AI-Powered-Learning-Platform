# U03 File & Artifact - Câu hỏi NFR Requirements

## Câu N1 - Backend đăng nhập Google Drive

A) Service account, thêm vào Shared Drive.

B) OAuth bằng tài khoản Google của một thành viên.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: X - "dựa vào .env", làm rõ: "có key gì đó ở env sau đó upload lên theo cái key đấy" → JSON key của service account đặt trong `.env` (tương đương A)

## Câu N2 - Giới hạn upload đồng thời

A) Tối đa 5 cùng lúc, ghi file tạm ra đĩa.

B) Tối đa 10 cùng lúc.

C) Không giới hạn.

X) Khác (mô tả sau thẻ [Answer]: bên dưới)

[Answer]: A
