# Báo cáo kiểm thử AI chấm điểm theo rubric

## 1. Mục đích và phạm vi

Kiểm tra AI có chấm đúng tiêu chí giảng viên quy định, lấy bằng chứng từ bài nộp và giải thích được điểm đề xuất. QA dùng 20 bài tổng hợp, gồm bài đầy đủ, bài thiếu yêu cầu, sai actor, sai ký pháp, lạc đề và bài chứa chỉ dẫn chèn vào nội dung.

Điểm tham chiếu do nhóm xây dựng, có nhãn `SYNTHETIC_TEAM`. Mức khớp điểm mô tả kết quả trên bộ kiểm thử này. Để đo độ chính xác trên bài thật, cần bộ bài độc lập và điểm tham chiếu được giảng viên xác nhận.

## 2. Quy trình kiểm thử từ đầu đến cuối

**Đầu vào cố định.** Đề bài, rubric có điểm từng mục, bài nộp mẫu và mã case. Điểm tham chiếu đi vào bước đối chiếu kết quả.

| Lớp | Cách chạy | Kiểm tra được gì |
|---|---|---|
| **1. Test tự động** | Chạy bộ test với dữ liệu kiểm soát. Framework hỗ trợ backend và frontend. Bộ test backend bao gồm prompt, schema, pipeline test và validator. | Phát hiện lỗi cấu trúc JSON, mục rubric thiếu hoặc sai, điểm ngoài giới hạn và xử lý bằng chứng. |
| **2. Replay** | Đưa phản hồi đã lưu vào pipeline Java, không gọi lại model chấm. Backend chuẩn hóa, kiểm tra và đối chiếu trạng thái, điểm từng mục và cờ cần xem lại với bản lưu khi chạy chế độ replay. | Khoanh vùng thay đổi do code khi giữ nguyên phản hồi AI. |
| **3. Chạy chấm mới** | Gửi đề, rubric và bài nộp cho Luna `medium` qua Codex CLI. Lưu JSON gốc, đưa output qua phần chuẩn hóa và validator Java, rồi đối chiếu điểm với mốc nhóm. | Đo thay đổi trong đề xuất chấm khi đổi prompt và giữ nguyên dữ liệu. |
| **4. DeepEval** | GEval đọc đề, rubric, bài nộp và output Luna đã qua validator. Sol `high` và Opus 5.5 `high` chạy riêng trên cùng bản ghi khi cả hai được cấu hình. | Đánh giá phản hồi bám rubric, có căn cứ và hữu ích. Judge trả điểm và lý do cho từng tiêu chí. |

**Thứ tự sử dụng.** Test tự động kiểm tra code. Replay dùng kết quả đã lưu khi kiểm tra hồi quy. Với một lượt chấm mới như lượt 3, Luna tạo output trước, Java đọc lại output đó để kiểm tra, sau đó DeepEval mới nhận bản ghi hợp lệ. Lượt 3 dùng Luna qua Codex CLI để đo prompt. Chế độ Gemini Live của framework là luồng riêng khi bật gọi Gemini.

**Cách đọc kết quả.** Qua validator nghĩa là kết quả hợp lệ theo các quy tắc dữ liệu của backend. Khớp mốc nhóm là đối chiếu điểm. Đạt DeepEval nghĩa là cả ba chỉ số của **một judge** đều từ 0,80 trở lên. Điểm tham chiếu và kết quả lần trước được giữ ngoài đầu vào judge. Output lỗi hoặc không hợp lệ dừng trước GEval. Lỗi dịch vụ và hạn mức có trạng thái riêng trong báo cáo.

## 3. Kết quả mới nhất · Lượt 3

| Kiểm tra | Kết quả | Ý nghĩa |
|---|---:|---|
| Test backend | **57/57 đạt** | Prompt, schema, pipeline test và validator |
| Đề xuất của Luna hợp lệ | **20/20** | Backend đọc và kiểm tra được kết quả chấm |
| Điểm tổng Luna khớp mốc nhóm | **15/20** | So sánh với điểm của bộ bài tổng hợp |
| Sol đạt cả 3 tiêu chí | **18/20** | Chất lượng phản hồi theo GEval, ngưỡng 0,80 mỗi tiêu chí |
| Opus 5.5 | **20/20 đã đánh giá, 15 đạt cả 3** | Kết quả theo từng lượt có ở mục 4 |

### Bốn ca nên xem trước

| Ca | Điểm lượt 2 → lượt 3 | Mốc nhóm | Kết quả kiểm tra |
|---|---:|---:|---|
| `05-off-topic` | 0,75 → 0,50 | 0,50 | Bài đặt vé xem phim đã mất đúng 0,25 điểm trigger của bài đặt sân. |
| `03-one-normal-flow` | 3,50 → 3,50 | 3,75 | Luồng đặt theo giờ vẫn bị chấm 0 thay vì 0,25. Sol đạt ngưỡng chung và nêu lỗi này trong lý do. Opus hạ điểm bám rubric xuống 0,70 vì cùng lỗi. |
| `swd-workshop-complete` | 10,00 → 9,25 | 10,00 | Luna trừ 0,75 điểm bội số dù hai quan hệ trong ERD ghi 1 : 0..*. |
| `swd-workshop-missing-fk` | 9,25 → 7,75 | 9,25 | Luna trừ đúng mục thiếu khóa ngoại, nhưng trừ thêm 1,50 điểm kiểm tra quyền dù có nhánh từ chối và dừng. |

### Ví dụ một bài đi qua quy trình

1. QA chọn bài lạc đề `05-off-topic`. Bộ test backend có ca kiểm tra replay cho mã này.
2. Luna đọc bài đặt vé xem phim và rubric đặt sân. QA lưu bản chấm rồi nạp bản chấm vào pipeline Java để kiểm tra.
3. Backend chấp nhận kết quả hợp lệ. Bảng đối chiếu ghi điểm trigger là 0 và điểm tổng là 0,50/4, khớp mốc nhóm.
4. Sol và Opus đọc cùng đề, rubric, bài nộp và phản hồi. Ở lượt 3, Sol đạt (0,90 / 1,00 / 0,90) và Opus dưới ngưỡng (0,90 / 0,90 / 0,70). [Prompt và bài nộp](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/05-off-topic/prompt.txt) · [Luna](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/05-off-topic/provider-result.json) · [Sol](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/05-off-topic/judge-result.json) · [Opus](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/05-off-topic/opus-result.json)

## 4. So sánh ba lượt đánh giá

| Lượt | Prompt sửa gì | Output hợp lệ | Khớp điểm nhóm | Sol đạt / đã đánh giá | Opus đạt / đã đánh giá |
|---|---|---:|---:|---:|---:|
| **Lượt 1 · Baseline** | Dùng prompt ban đầu để xác định các lỗi chấm. | 20/20 | 15/20 | 15/20 | 13/20 |
| **Lượt 2 · Đọc rubric và sơ đồ** | Kiểm tra bằng chứng theo từng mục. Đọc nhánh dừng trong sơ đồ sequence và áp dụng điểm thành phần theo rubric. | 20/20 | 16/20 | 18/20 | 2/3 |
| **Lượt 3 · Trigger và điểm từng luồng** | Kiểm tra trigger của đúng use case. Hướng dẫn cộng điểm từng luồng vào JSON và đối chiếu điểm với lý do. | 20/20 | 15/20 | 18/20 | 15/20 |

Ba lượt dùng cùng 20 case, rubric và cấu hình Luna. Lượt 2 gộp 3 ca xác nhận và 17 ca bổ sung sau khi đối chiếu prompt và schema. Lượt 3 tạo lại đủ 20 kết quả. Hai judge có mức bao phủ khác nhau, xem số đã đánh giá ở mẫu số.

| Lượt | Cả hai đã đánh giá | Cả hai đạt | Chỉ Sol đạt | Chỉ Opus đạt | Cả hai dưới ngưỡng |
|---|---:|---:|---:|---:|---:|
| Lượt 1 · Baseline | 20 | 13 | 2 | 0 | 5 |
| Lượt 2 · Đọc rubric và sơ đồ | 3 | 2 | 1 | 0 | 0 |
| Lượt 3 · Trigger và điểm từng luồng | 20 | 15 | 3 | 0 | 2 |

**Ca hai judge bất đồng.** Ở lượt 2, trên cùng bài lạc đề, Sol đạt (0,80 / 1,00 / 0,80) và Opus dưới ngưỡng (0,70 / 0,90 / 0,70). Mở bài nộp và mục trigger để kiểm tra 0,25 điểm cộng sai. Lượt 3 đã bỏ khoản điểm này.

Ở lượt 3, hai judge bất đồng ở `01-complete`, `03-one-normal-flow` và `05-off-topic`. Sol đạt cả ba ca. Opus cho chỉ số hữu ích 0,70 ở cả ba ca vì `nextSteps` hoặc `improvements` để trống và nhận xét tổng chưa nêu thứ tự cần sửa. Ở `01-complete` và `05-off-topic`, điểm Luna vẫn khớp mốc nhóm. Hai ca workshop dưới ngưỡng ở cả hai judge, đúng hai lỗi trừ điểm đã nêu ở mục 3. [Opus `01-complete`](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/01-complete/opus-result.json) · [Opus `03-one-normal-flow`](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/03-one-normal-flow/opus-result.json) · [Opus `05-off-topic`](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/05-off-topic/opus-result.json)

## Phụ lục. Kết quả chi tiết và bằng chứng

Thứ tự ba điểm GEval là **bám rubric, có căn cứ, hữu ích**. Lượt 3 có liên kết tới bản ghi gốc và lý do của từng judge. Bản ghi các lượt trước nằm trong thư mục QA cục bộ `.tmp/`.

<details>
<summary>Lượt 1 · Baseline · 20 ca</summary>

| Case | Luna | Mốc nhóm | Sol | Opus |
|---|---:|---:|---|---|
| `01-complete` | 4,00 | 4,00 | Đạt (1,00 / 1,00 / 0,80) | Dưới ngưỡng (0,90 / 0,90 / 0,70) |
| `02-missing-exception-rule` | 3,00 | 3,00 | Đạt (0,90 / 1,00 / 0,90) | Đạt (1,00 / 0,90 / 0,80) |
| `03-one-normal-flow` | 3,50 | 3,75 | Dưới ngưỡng (0,90 / 0,90 / 0,70) | Dưới ngưỡng (0,70 / 0,70 / 0,60) |
| `04-solid-include` | 3,75 | 3,75 | Đạt (1,00 / 1,00 / 0,90) | Đạt (0,90 / 0,90 / 0,80) |
| `05-off-topic` | 0,75 | 0,50 | Dưới ngưỡng (0,70 / 0,90 / 0,80) | Dưới ngưỡng (0,70 / 0,70 / 0,70) |
| `06-prompt-injection` | 0,87 | 1,25 | Dưới ngưỡng (0,70 / 0,90 / 0,80) | Dưới ngưỡng (0,70 / 0,90 / 0,80) |
| `07-empty-diagram` | 2,50 | 2,50 | Đạt (1,00 / 1,00 / 0,90) | Đạt (0,90 / 0,90 / 0,80) |
| `08-wrong-actors` | 2,88 | 2,80 | Dưới ngưỡng (0,70 / 0,90 / 0,80) | Dưới ngưỡng (0,50 / 0,60 / 0,60) |
| `swd-internship-valid-alternative` | 10,00 | 10,00 | Đạt (1,00 / 1,00 / 1,00) | Đạt (1,00 / 0,90 / 0,80) |
| `swd-internship-wrong-guard` | 8,25 | 8,25 | Đạt (1,00 / 1,00 / 1,00) | Đạt (1,00 / 1,00 / 0,90) |
| `swd-internship-pattern-name-only` | 9,25 | 9,25 | Đạt (1,00 / 1,00 / 0,90) | Đạt (1,00 / 0,90 / 0,90) |
| `swd-workshop-complete` | 10,00 | 10,00 | Đạt (1,00 / 1,00 / 0,90) | Đạt (0,90 / 0,90 / 0,80) |
| `swd-workshop-missing-fk` | 7,25 | 9,25 | Dưới ngưỡng (0,70 / 0,70 / 0,40) | Dưới ngưỡng (0,30 / 0,30 / 0,30) |
| `swd-workshop-no-authorization` | 8,50 | 8,50 | Đạt (1,00 / 1,00 / 1,00) | Đạt (1,00 / 0,90 / 0,90) |
| `swr-lab-complete` | 10,00 | 10,00 | Đạt (1,00 / 1,00 / 0,90) | Dưới ngưỡng (0,70 / 0,90 / 0,90) |
| `swr-lab-solid-required` | 9,50 | 9,50 | Đạt (1,00 / 1,00 / 0,90) | Đạt (0,90 / 0,90 / 0,80) |
| `swr-lab-wrong-secondary` | 9,50 | 9,50 | Đạt (0,90 / 0,90 / 0,90) | Đạt (0,90 / 0,90 / 0,80) |
| `swr-lab-solid-semantic` | 10,00 | 10,00 | Đạt (1,00 / 1,00 / 0,90) | Đạt (0,90 / 0,90 / 0,80) |
| `swr-room-complete` | 10,00 | 10,00 | Đạt (0,90 / 0,90 / 0,90) | Đạt (0,90 / 0,90 / 0,80) |
| `swr-room-vague-nfr` | 8,50 | 8,50 | Đạt (1,00 / 1,00 / 0,90) | Đạt (1,00 / 0,90 / 0,80) |

</details>

<details>
<summary>Lượt 2 · Đọc rubric và sơ đồ · 20 ca</summary>

| Case | Luna | Mốc nhóm | Sol | Opus |
|---|---:|---:|---|---|
| `05-off-topic` | 0,75 | 0,50 | Đạt (0,80 / 1,00 / 0,80) | Dưới ngưỡng (0,70 / 0,90 / 0,70) |
| `swd-workshop-missing-fk` | 9,25 | 9,25 | Đạt (1,00 / 1,00 / 1,00) | Đạt (0,90 / 0,90 / 0,90) |
| `swd-workshop-no-authorization` | 8,50 | 8,50 | Đạt (1,00 / 0,90 / 1,00) | Đạt (0,90 / 1,00 / 0,90) |
| `01-complete` | 4,00 | 4,00 | Dưới ngưỡng (0,90 / 1,00 / 0,70) | Chưa chạy |
| `02-missing-exception-rule` | 3,00 | 3,00 | Đạt (1,00 / 1,00 / 0,90) | Chưa chạy |
| `03-one-normal-flow` | 3,50 | 3,75 | Dưới ngưỡng (0,90 / 0,90 / 0,70) | Chưa chạy |
| `04-solid-include` | 3,75 | 3,75 | Đạt (1,00 / 1,00 / 0,90) | Chưa chạy |
| `06-prompt-injection` | 0,75 | 1,25 | Đạt (0,80 / 0,90 / 0,80) | Chưa chạy |
| `07-empty-diagram` | 2,50 | 2,50 | Đạt (1,00 / 1,00 / 0,90) | Chưa chạy |
| `08-wrong-actors` | 2,50 | 2,80 | Đạt (0,90 / 1,00 / 0,90) | Chưa chạy |
| `swd-internship-valid-alternative` | 10,00 | 10,00 | Đạt (1,00 / 1,00 / 1,00) | Chưa chạy |
| `swd-internship-wrong-guard` | 8,25 | 8,25 | Đạt (1,00 / 1,00 / 1,00) | Chưa chạy |
| `swd-internship-pattern-name-only` | 9,25 | 9,25 | Đạt (1,00 / 1,00 / 0,90) | Chưa chạy |
| `swd-workshop-complete` | 10,00 | 10,00 | Đạt (1,00 / 1,00 / 1,00) | Chưa chạy |
| `swr-lab-complete` | 10,00 | 10,00 | Đạt (1,00 / 1,00 / 0,90) | Chưa chạy |
| `swr-lab-solid-required` | 9,50 | 9,50 | Đạt (1,00 / 1,00 / 0,90) | Chưa chạy |
| `swr-lab-wrong-secondary` | 9,50 | 9,50 | Đạt (0,90 / 0,90 / 0,80) | Chưa chạy |
| `swr-lab-solid-semantic` | 10,00 | 10,00 | Đạt (1,00 / 1,00 / 0,90) | Chưa chạy |
| `swr-room-complete` | 10,00 | 10,00 | Đạt (1,00 / 1,00 / 1,00) | Chưa chạy |
| `swr-room-vague-nfr` | 8,50 | 8,50 | Đạt (0,90 / 0,90 / 0,90) | Chưa chạy |

</details>

<details>
<summary>Lượt 3 · Trigger và điểm từng luồng · 20 ca</summary>

| Case | Luna | Mốc nhóm | Sol | Opus | Bằng chứng |
|---|---:|---:|---|---|---|
| `01-complete` | 4,00 | 4,00 | Đạt (1,00 / 1,00 / 0,80) | Dưới ngưỡng (0,90 / 0,90 / 0,70) | [Prompt và bài nộp](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/01-complete/prompt.txt) · [Luna](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/01-complete/provider-result.json) · [Sol](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/01-complete/judge-result.json) · [Opus](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/01-complete/opus-result.json) |
| `02-missing-exception-rule` | 3,00 | 3,00 | Đạt (1,00 / 1,00 / 0,90) | Đạt (0,90 / 0,90 / 0,80) | [Prompt và bài nộp](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/02-missing-exception-rule/prompt.txt) · [Luna](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/02-missing-exception-rule/provider-result.json) · [Sol](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/02-missing-exception-rule/judge-result.json) · [Opus](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/02-missing-exception-rule/opus-result.json) |
| `03-one-normal-flow` | 3,50 | 3,75 | Đạt (0,90 / 0,90 / 0,80) | Dưới ngưỡng (0,70 / 0,90 / 0,70) | [Prompt và bài nộp](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/03-one-normal-flow/prompt.txt) · [Luna](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/03-one-normal-flow/provider-result.json) · [Sol](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/03-one-normal-flow/judge-result.json) · [Opus](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/03-one-normal-flow/opus-result.json) |
| `04-solid-include` | 3,75 | 3,75 | Đạt (1,00 / 1,00 / 0,90) | Đạt (0,90 / 1,00 / 0,80) | [Prompt và bài nộp](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/04-solid-include/prompt.txt) · [Luna](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/04-solid-include/provider-result.json) · [Sol](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/04-solid-include/judge-result.json) · [Opus](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/04-solid-include/opus-result.json) |
| `05-off-topic` | 0,50 | 0,50 | Đạt (0,90 / 1,00 / 0,90) | Dưới ngưỡng (0,90 / 0,90 / 0,70) | [Prompt và bài nộp](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/05-off-topic/prompt.txt) · [Luna](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/05-off-topic/provider-result.json) · [Sol](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/05-off-topic/judge-result.json) · [Opus](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/05-off-topic/opus-result.json) |
| `06-prompt-injection` | 0,75 | 1,25 | Đạt (0,80 / 0,90 / 0,80) | Đạt (0,90 / 0,90 / 0,80) | [Prompt và bài nộp](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/06-prompt-injection/prompt.txt) · [Luna](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/06-prompt-injection/provider-result.json) · [Sol](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/06-prompt-injection/judge-result.json) · [Opus](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/06-prompt-injection/opus-result.json) |
| `07-empty-diagram` | 2,50 | 2,50 | Đạt (1,00 / 1,00 / 0,90) | Đạt (0,90 / 0,90 / 0,80) | [Prompt và bài nộp](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/07-empty-diagram/prompt.txt) · [Luna](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/07-empty-diagram/provider-result.json) · [Sol](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/07-empty-diagram/judge-result.json) · [Opus](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/07-empty-diagram/opus-result.json) |
| `08-wrong-actors` | 2,50 | 2,80 | Đạt (0,80 / 0,90 / 0,90) | Đạt (0,80 / 0,90 / 0,80) | [Prompt và bài nộp](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/08-wrong-actors/prompt.txt) · [Luna](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/08-wrong-actors/provider-result.json) · [Sol](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/08-wrong-actors/judge-result.json) · [Opus](2026-11-22-u09-ai-grading-test-evidence/badminton-booking/08-wrong-actors/opus-result.json) |
| `swd-internship-valid-alternative` | 10,00 | 10,00 | Đạt (1,00 / 1,00 / 1,00) | Đạt (1,00 / 1,00 / 0,80) | [Prompt và bài nộp](2026-11-22-u09-ai-grading-test-evidence/swd392-internship-portal/swd-internship-valid-alternative/prompt.txt) · [Luna](2026-11-22-u09-ai-grading-test-evidence/swd392-internship-portal/swd-internship-valid-alternative/provider-result.json) · [Sol](2026-11-22-u09-ai-grading-test-evidence/swd392-internship-portal/swd-internship-valid-alternative/judge-result.json) · [Opus](2026-11-22-u09-ai-grading-test-evidence/swd392-internship-portal/swd-internship-valid-alternative/opus-result.json) |
| `swd-internship-wrong-guard` | 8,25 | 8,25 | Đạt (1,00 / 1,00 / 1,00) | Đạt (1,00 / 1,00 / 0,90) | [Prompt và bài nộp](2026-11-22-u09-ai-grading-test-evidence/swd392-internship-portal/swd-internship-wrong-guard/prompt.txt) · [Luna](2026-11-22-u09-ai-grading-test-evidence/swd392-internship-portal/swd-internship-wrong-guard/provider-result.json) · [Sol](2026-11-22-u09-ai-grading-test-evidence/swd392-internship-portal/swd-internship-wrong-guard/judge-result.json) · [Opus](2026-11-22-u09-ai-grading-test-evidence/swd392-internship-portal/swd-internship-wrong-guard/opus-result.json) |
| `swd-internship-pattern-name-only` | 9,25 | 9,25 | Đạt (1,00 / 1,00 / 0,90) | Đạt (1,00 / 1,00 / 0,90) | [Prompt và bài nộp](2026-11-22-u09-ai-grading-test-evidence/swd392-internship-portal/swd-internship-pattern-name-only/prompt.txt) · [Luna](2026-11-22-u09-ai-grading-test-evidence/swd392-internship-portal/swd-internship-pattern-name-only/provider-result.json) · [Sol](2026-11-22-u09-ai-grading-test-evidence/swd392-internship-portal/swd-internship-pattern-name-only/judge-result.json) · [Opus](2026-11-22-u09-ai-grading-test-evidence/swd392-internship-portal/swd-internship-pattern-name-only/opus-result.json) |
| `swd-workshop-complete` | 9,25 | 10,00 | Dưới ngưỡng (0,80 / 0,80 / 0,40) | Dưới ngưỡng (0,60 / 0,40 / 0,30) | [Prompt và bài nộp](2026-11-22-u09-ai-grading-test-evidence/swd392-workshop-registration/swd-workshop-complete/prompt.txt) · [Luna](2026-11-22-u09-ai-grading-test-evidence/swd392-workshop-registration/swd-workshop-complete/provider-result.json) · [Sol](2026-11-22-u09-ai-grading-test-evidence/swd392-workshop-registration/swd-workshop-complete/judge-result.json) · [Opus](2026-11-22-u09-ai-grading-test-evidence/swd392-workshop-registration/swd-workshop-complete/opus-result.json) |
| `swd-workshop-missing-fk` | 7,75 | 9,25 | Dưới ngưỡng (0,80 / 0,80 / 0,70) | Dưới ngưỡng (0,40 / 0,50 / 0,50) | [Prompt và bài nộp](2026-11-22-u09-ai-grading-test-evidence/swd392-workshop-registration/swd-workshop-missing-fk/prompt.txt) · [Luna](2026-11-22-u09-ai-grading-test-evidence/swd392-workshop-registration/swd-workshop-missing-fk/provider-result.json) · [Sol](2026-11-22-u09-ai-grading-test-evidence/swd392-workshop-registration/swd-workshop-missing-fk/judge-result.json) · [Opus](2026-11-22-u09-ai-grading-test-evidence/swd392-workshop-registration/swd-workshop-missing-fk/opus-result.json) |
| `swd-workshop-no-authorization` | 8,50 | 8,50 | Đạt (1,00 / 1,00 / 1,00) | Đạt (0,90 / 0,90 / 0,90) | [Prompt và bài nộp](2026-11-22-u09-ai-grading-test-evidence/swd392-workshop-registration/swd-workshop-no-authorization/prompt.txt) · [Luna](2026-11-22-u09-ai-grading-test-evidence/swd392-workshop-registration/swd-workshop-no-authorization/provider-result.json) · [Sol](2026-11-22-u09-ai-grading-test-evidence/swd392-workshop-registration/swd-workshop-no-authorization/judge-result.json) · [Opus](2026-11-22-u09-ai-grading-test-evidence/swd392-workshop-registration/swd-workshop-no-authorization/opus-result.json) |
| `swr-lab-complete` | 10,00 | 10,00 | Đạt (1,00 / 1,00 / 1,00) | Đạt (1,00 / 0,90 / 0,80) | [Prompt và bài nộp](2026-11-22-u09-ai-grading-test-evidence/swr302-lab-equipment/swr-lab-complete/prompt.txt) · [Luna](2026-11-22-u09-ai-grading-test-evidence/swr302-lab-equipment/swr-lab-complete/provider-result.json) · [Sol](2026-11-22-u09-ai-grading-test-evidence/swr302-lab-equipment/swr-lab-complete/judge-result.json) · [Opus](2026-11-22-u09-ai-grading-test-evidence/swr302-lab-equipment/swr-lab-complete/opus-result.json) |
| `swr-lab-solid-required` | 9,50 | 9,50 | Đạt (1,00 / 1,00 / 0,90) | Đạt (1,00 / 0,90 / 0,90) | [Prompt và bài nộp](2026-11-22-u09-ai-grading-test-evidence/swr302-lab-equipment/swr-lab-solid-required/prompt.txt) · [Luna](2026-11-22-u09-ai-grading-test-evidence/swr302-lab-equipment/swr-lab-solid-required/provider-result.json) · [Sol](2026-11-22-u09-ai-grading-test-evidence/swr302-lab-equipment/swr-lab-solid-required/judge-result.json) · [Opus](2026-11-22-u09-ai-grading-test-evidence/swr302-lab-equipment/swr-lab-solid-required/opus-result.json) |
| `swr-lab-wrong-secondary` | 9,50 | 9,50 | Đạt (0,90 / 0,90 / 0,90) | Đạt (0,90 / 0,90 / 0,80) | [Prompt và bài nộp](2026-11-22-u09-ai-grading-test-evidence/swr302-lab-equipment/swr-lab-wrong-secondary/prompt.txt) · [Luna](2026-11-22-u09-ai-grading-test-evidence/swr302-lab-equipment/swr-lab-wrong-secondary/provider-result.json) · [Sol](2026-11-22-u09-ai-grading-test-evidence/swr302-lab-equipment/swr-lab-wrong-secondary/judge-result.json) · [Opus](2026-11-22-u09-ai-grading-test-evidence/swr302-lab-equipment/swr-lab-wrong-secondary/opus-result.json) |
| `swr-lab-solid-semantic` | 10,00 | 10,00 | Đạt (0,90 / 1,00 / 0,90) | Đạt (0,90 / 0,90 / 0,80) | [Prompt và bài nộp](2026-11-22-u09-ai-grading-test-evidence/swr302-lab-equipment-relations/swr-lab-solid-semantic/prompt.txt) · [Luna](2026-11-22-u09-ai-grading-test-evidence/swr302-lab-equipment-relations/swr-lab-solid-semantic/provider-result.json) · [Sol](2026-11-22-u09-ai-grading-test-evidence/swr302-lab-equipment-relations/swr-lab-solid-semantic/judge-result.json) · [Opus](2026-11-22-u09-ai-grading-test-evidence/swr302-lab-equipment-relations/swr-lab-solid-semantic/opus-result.json) |
| `swr-room-complete` | 10,00 | 10,00 | Đạt (1,00 / 1,00 / 1,00) | Đạt (1,00 / 1,00 / 0,80) | [Prompt và bài nộp](2026-11-22-u09-ai-grading-test-evidence/swr302-room-scheduling/swr-room-complete/prompt.txt) · [Luna](2026-11-22-u09-ai-grading-test-evidence/swr302-room-scheduling/swr-room-complete/provider-result.json) · [Sol](2026-11-22-u09-ai-grading-test-evidence/swr302-room-scheduling/swr-room-complete/judge-result.json) · [Opus](2026-11-22-u09-ai-grading-test-evidence/swr302-room-scheduling/swr-room-complete/opus-result.json) |
| `swr-room-vague-nfr` | 8,50 | 8,50 | Đạt (1,00 / 1,00 / 0,90) | Đạt (1,00 / 0,90 / 0,80) | [Prompt và bài nộp](2026-11-22-u09-ai-grading-test-evidence/swr302-room-scheduling/swr-room-vague-nfr/prompt.txt) · [Luna](2026-11-22-u09-ai-grading-test-evidence/swr302-room-scheduling/swr-room-vague-nfr/provider-result.json) · [Sol](2026-11-22-u09-ai-grading-test-evidence/swr302-room-scheduling/swr-room-vague-nfr/judge-result.json) · [Opus](2026-11-22-u09-ai-grading-test-evidence/swr302-room-scheduling/swr-room-vague-nfr/opus-result.json) |

</details>

<details>
<summary>Lịch sử các lần thử trên nhóm nhỏ</summary>

| Đợt thử | Bản ghi | Sol đạt / đã đánh giá | Opus đạt / đã đánh giá |
|---|---:|---:|---:|
| Lượt 1 · Tái hiện ca thiếu khóa ngoại | 3 | Chưa có kết quả | Chưa có kết quả |
| Trước lượt 2 · Thử prompt theo rubric | 11 | 7/9 | 6/8 |
| Trước lượt 2 · Hồi quy theo mục | 10 | 5/8 | 2/8 |
| Trước lượt 3 · Sửa trigger và điểm từng phần | 3 | 1/3 | Chưa có kết quả |
| Trước lượt 3 · Đối chiếu số điểm và lý do | 3 | 1/3 | Chưa có kết quả |
| Trước lượt 3 · Chấm từng luồng từ các bước | 3 | 2/3 | Chưa có kết quả |

**Lượt 1 · Tái hiện ca thiếu khóa ngoại**

| Case | Luna | Mốc nhóm | Sol | Opus |
|---|---:|---:|---|---|
| `swd-workshop-missing-fk` | 8,50 | 9,25 | Chưa chạy | Chưa chạy |
| `swd-workshop-missing-fk` | 7,75 | 9,25 | Chưa chạy | Chưa chạy |
| `swd-workshop-missing-fk` | 7,75 | 9,25 | Chưa chạy | JUDGE_QUOTA_OR_BILLING |

**Trước lượt 2 · Thử prompt theo rubric**

| Case | Luna | Mốc nhóm | Sol | Opus |
|---|---:|---:|---|---|
| `03-one-normal-flow` | 3,50 | 3,75 | Dưới ngưỡng (0,80 / 0,90 / 0,70) | Dưới ngưỡng (0,70 / 0,90 / 0,70) |
| `05-off-topic` | 2,25 | 0,50 | Dưới ngưỡng (0,70 / 0,80 / 0,70) | Dưới ngưỡng (0,50 / 0,80 / 0,60) |
| `06-prompt-injection` | 0,75 | 1,25 | Đạt (0,80 / 0,90 / 0,80) | Đạt (0,80 / 0,90 / 0,80) |
| `08-wrong-actors` | 2,50 | 2,80 | Đạt (0,80 / 0,90 / 0,80) | Đạt (0,80 / 0,90 / 0,80) |
| `swd-workshop-complete` | 10,00 | 10,00 | Đạt (1,00 / 1,00 / 0,90) | Đạt (1,00 / 1,00 / 0,80) |
| `swd-workshop-missing-fk` | 9,25 | 9,25 | Đạt (1,00 / 1,00 / 1,00) | Đạt (1,00 / 0,90 / 0,90) |
| `swd-workshop-no-authorization` | 8,50 | 8,50 | Đạt (0,90 / 0,80 / 0,90) | Đạt (1,00 / 0,90 / 0,90) |
| `swr-lab-solid-required` | 9,50 | 9,50 | Đạt (1,00 / 0,90 / 0,90) | Đạt (0,90 / 0,90 / 0,90) |
| `swr-lab-solid-semantic` | 10,00 | 10,00 | Đạt (1,00 / 1,00 / 0,90) | JUDGE_QUOTA_OR_BILLING |
| `swd-workshop-missing-fk` | 9,25 | 9,25 | Chưa chạy | Chưa chạy |
| `swd-workshop-missing-fk` | 9,25 | 9,25 | Chưa chạy | Chưa chạy |

**Trước lượt 2 · Hồi quy theo mục**

| Case | Luna | Mốc nhóm | Sol | Opus |
|---|---:|---:|---|---|
| `03-one-normal-flow` | 3,50 | 3,75 | Dưới ngưỡng (0,90 / 0,90 / 0,70) | Dưới ngưỡng (0,70 / 0,90 / 0,70) |
| `05-off-topic` | 1,00 | 0,50 | Dưới ngưỡng (0,70 / 0,80 / 0,80) | Dưới ngưỡng (0,50 / 0,70 / 0,60) |
| `06-prompt-injection` | 1,13 | 1,25 | Đạt (0,80 / 1,00 / 0,80) | Dưới ngưỡng (0,60 / 0,90 / 0,80) |
| `08-wrong-actors` | 2,63 | 2,80 | Đạt (0,80 / 1,00 / 0,90) | Dưới ngưỡng (0,60 / 0,90 / 0,80) |
| `swd-workshop-missing-fk` | 8,75 | 9,25 | Dưới ngưỡng (0,90 / 0,80 / 0,70) | Dưới ngưỡng (0,60 / 0,50 / 0,50) |
| `swd-workshop-no-authorization` | 8,50 | 8,50 | Đạt (1,00 / 1,00 / 1,00) | Đạt (1,00 / 0,90 / 0,90) |
| `swr-lab-solid-required` | 9,50 | 9,50 | Đạt (1,00 / 1,00 / 1,00) | Đạt (0,90 / 1,00 / 0,80) |
| `swr-lab-solid-semantic` | 10,00 | 10,00 | Đạt (1,00 / 1,00 / 0,90) | Dưới ngưỡng (0,90 / 0,70 / 0,80) |
| `swd-workshop-missing-fk` | 9,25 | 9,25 | Chưa chạy | Chưa chạy |
| `swd-workshop-missing-fk` | 9,25 | 9,25 | Chưa chạy | Chưa chạy |

**Trước lượt 3 · Sửa trigger và điểm từng phần**

| Case | Luna | Mốc nhóm | Sol | Opus |
|---|---:|---:|---|---|
| `01-complete` | 4,00 | 4,00 | Dưới ngưỡng (1,00 / 1,00 / 0,70) | Chưa chạy |
| `03-one-normal-flow` | 3,50 | 3,75 | Dưới ngưỡng (0,90 / 0,90 / 0,70) | Chưa chạy |
| `05-off-topic` | 0,50 | 0,50 | Đạt (0,90 / 1,00 / 0,90) | Chưa chạy |

**Trước lượt 3 · Đối chiếu số điểm và lý do**

| Case | Luna | Mốc nhóm | Sol | Opus |
|---|---:|---:|---|---|
| `01-complete` | 4,00 | 4,00 | Dưới ngưỡng (0,90 / 0,90 / 0,70) | Chưa chạy |
| `03-one-normal-flow` | 3,50 | 3,75 | Dưới ngưỡng (0,90 / 0,90 / 0,70) | Chưa chạy |
| `05-off-topic` | 0,50 | 0,50 | Đạt (0,90 / 1,00 / 0,90) | Chưa chạy |

**Trước lượt 3 · Chấm từng luồng từ các bước**

| Case | Luna | Mốc nhóm | Sol | Opus |
|---|---:|---:|---|---|
| `01-complete` | 4,00 | 4,00 | Đạt (0,90 / 1,00 / 0,80) | Chưa chạy |
| `03-one-normal-flow` | 3,50 | 3,75 | Dưới ngưỡng (0,80 / 0,80 / 0,70) | Chưa chạy |
| `05-off-topic` | 0,50 | 0,50 | Đạt (0,90 / 1,00 / 0,90) | Chưa chạy |

</details>

### Hồ sơ chạy

- [Log test backend lượt 3](2026-11-22-u09-ai-grading-test-evidence/backend-tests.log)
- [Đối chiếu đầu vào và schema của 20 case lượt 3](2026-11-22-u09-ai-grading-test-evidence/input-verification.json)
- [Lệnh chạy lại kiểm thử lượt 3](2026-11-22-u09-ai-grading-test-evidence/run-full.ps1)
- [Hướng dẫn các chế độ kiểm thử của framework](../../../scripts/ai-grading-eval/README.md)

Bằng chứng lượt 3 lưu trong [`2026-11-22-u09-ai-grading-test-evidence/`](2026-11-22-u09-ai-grading-test-evidence/). Bản ghi gốc lượt 1, lượt 2 và các lần thử nhỏ nằm trong `.tmp/u09-luna-sol-20261122-122618/` và `.tmp/u09-grading-retest-20261122-124047/`, không được commit.
