# Unit / Story / UC map — 2026-10-14

> AS-BUILT sync 2026-10-30; 66 UC/15 unit ownership retained and verified against source. Dated planning estimates below remain estimates, not actual LOC or runtime acceptance.

66 active UC; 50 MVP stories + 1 extension; 15 logical units

| Iter | Unit | Tên | Primary UC |
|---|---|---|---|
| 1 | U01 | Account & Access | 01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11, 12 |
| 1 | U02 | Audit | 13 |
| 1 | U03 | Job and Settings | 14, 15 |
| 1 | U04 | Subject Administration | 16, 17, 18, 19 |
| 1 | U05 | Class & Enrollment | 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30 |
| 2 | U06 | AI Connections | 31, 32, 33 |
| 2 | U07 | Assignment Authoring | 34, 35, 36 |
| 2 | U08 | Assignment Work | 37, 38, 39, 40, 41 |
| 2 | U09 | Assignment Grading | 42, 43, 44, 45 |
| 2 | U10 | Quiz Authoring | 46, 47, 48 |
| 3 | U11 | Quiz Practice | 49, 50, 51, 52 |
| 3 | U12 | Group Assignment | 53, 54, 55, 56, 57 |
| 3 | U13 | Reference Materials | 58, 59, 60, 61, 62 |
| 3 | U14 | Announcements & Notifications | 63, 64, 65 |
| 3 | U15 | Admin Dashboard | 66 |

## Story ownership

| Unit | MVP stories | Extension stories |
|---|---|---|
| U01 | US-IAM-001, US-IAM-002, US-IAM-003, US-IAM-004, US-IAM-005, US-IAM-006, US-IAM-007 |  |
| U02 | US-AUD-001 |  |
| U03 | US-SET-001, US-OPS-001 |  |
| U04 | US-CAT-001 |  |
| U05 | US-CAT-002, US-CAT-003, US-LRN-001 |  |
| U06 | US-AIC-001, US-AIC-002, US-AIC-003, US-AIC-004, US-AIG-001, US-AIG-002, US-AIG-003 |  |
| U07 | US-ASM-001, US-ASM-004, US-ASM-008, US-ASM-009, US-ASM-010, US-QBK-001 |  |
| U08 | US-ASM-003, US-ASM-012 |  |
| U09 | US-GRD-002, US-GRD-003, US-GRD-004, US-GRD-005, US-INT-001, US-RPT-001, US-RPT-003 |  |
| U10 | US-QBK-002, US-ASM-006 |  |
| U11 | US-GRD-001 |  |
| U12 | US-GRP-001, US-GRP-002, US-GRP-003, US-GRP-004, US-GRP-005, US-GRP-006 |  |
| U13 | US-CNT-001, US-CNT-002 | US-CNT-005 |
| U14 | US-CNT-004, US-NTF-001 |  |
| U15 | US-RPT-002 |  |

Catalog source: [use-cases.md](../requirements/use-cases.md). Unit, iteration, phụ thuộc và phân công: [unit-of-work.md](unit-of-work.md).

## Story trải nhiều unit

| Story | Unit sở hữu | Unit hỗ trợ |
|---|---|---|
| US-CAT-001 | U04 (môn) | U05 (tạo lớp, giao giảng viên) |
| US-ASM-001 | U07 (duyệt, phát hành Assignment) | U10 (phát hành Quiz) |
| US-ASM-003 | U08 (làm, nộp Assignment cá nhân) | U11 (làm Quiz) |
| US-AIG-001, US-AIG-002 | U06 (gọi AI) | U07, U10 (nhận bản nháp) |
| US-RPT-001 | U09 (theo dõi nộp bài) | U14 (nhắc hạn), U05 (bật phân bố điểm) |
| US-INT-001 | U09 (giảng viên xem) | U08 (dữ liệu hoạt động), U11, U12 (ghi sự kiện) |

## Truy vết chức năng đã gộp

| UC hiện hành | Requirement | Stories liên quan | Primary UC owner | Owner thực thi hỗ trợ |
|---|---|---|---|---|
| 14 View Settings / 15 Edit Setting | FR-033, FR-034, FR-038, FR-039 | US-SET-001, US-AIC-003, US-OPS-001 | U03 | U06 quota; U03 backup/activity retention settings |
| 14 View Settings / 15 Edit Setting (API chung) | FR-012, FR-021, FR-037 | US-AIG-003 | U03 | U06 kết nối và credentials được bảo vệ |
| 66 View Admin Dashboard | FR-024, FR-038 | US-RPT-002, US-OPS-001 | U15 | U03 backup metadata |
| 42 View Student Submissions | FR-008, FR-019, FR-039 | US-RPT-001, US-INT-001 | U09 | U08 bài nộp và hoạt động; U12 bài nhóm |

Story owner không buộc bằng primary UC owner: US-OPS-001 thuộc U03, US-AIC-003 thuộc U06, US-RPT-001 và US-INT-001 thuộc U09. Đây là truy vết chéo, không tạo primary owner thứ hai cho UC.
