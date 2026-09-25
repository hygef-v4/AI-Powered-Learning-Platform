# U10 Template, Copy & Simulation - Infrastructure Design

## 1. Ánh xạ

| Thành phần | Chạy ở |
|---|---|
| Template, copy, diff, chính sách thi thử | `backend` |
| Bảng `template_releases`, `assignment_lineage`, `simulation_policies` | `postgres` |

U10 không chạy trong `worker`, không có queue, Redis key, secret hay dịch vụ ngoài.

## 2. Migration

`V20260925_1700__u10_template_copy_simulation.sql`:
- `template_releases` (FK `assignments`), unique `(template_assignment_id)`, index `(subject_id, status)`.
- `assignment_lineage (target_assignment_id PK FK, source_assignment_id FK, kind, source_class_id, target_class_id, actor_id, created_at)`; `REVOKE UPDATE, DELETE ON assignment_lineage FROM app`.
- `simulation_policies (publication_id PK FK publications, max_attempts INT NOT NULL DEFAULT 3 CHECK (max_attempts BETWEEN 1 AND 10), result_policy, answer_release, counts_toward_grade, locked_at)`.

## 3. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| Mọi rule | N/A | Không thêm hạ tầng; dùng chung deploy, health, secret của backend |
