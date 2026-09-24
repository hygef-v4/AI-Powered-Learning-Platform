# U10 Template, Copy & Simulation - Tech Stack Decisions

| Hạng mục | Chọn | Lý do |
|---|---|---|
| Diff văn bản | `java-diff-utils` (`io.github.java-diff-utils`) | Diff theo dòng, nhẹ |
| Diff thành phần | So khớp theo `bankItemId`/`stableKey` hoặc hash `inlineDefinition`, rồi so thứ tự và điểm | Không cần thư viện |
| Lưu | PostgreSQL + JPA | Như các unit trước |
| Frontend | Next.js + Tailwind, component tự viết | Như các unit trước |
