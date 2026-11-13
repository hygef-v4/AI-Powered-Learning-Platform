-- U13: học liệu chia theo module của môn. Mỗi học liệu (của môn hay của lớp) bắt buộc thuộc một module của môn;
-- Quiz của môn hay của lớp được gắn vào module qua bảng nối (một Quiz nằm ở nhiều nhất một module).
CREATE TABLE material_modules (
    id UUID PRIMARY KEY,
    subject_id UUID NOT NULL REFERENCES subjects(id),
    title VARCHAR(200) NOT NULL CHECK (length(trim(title)) > 0),
    order_no INTEGER NOT NULL CHECK (order_no >= 0),
    created_by UUID NOT NULL REFERENCES accounts(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL CHECK (updated_at >= created_at),
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0)
);
CREATE INDEX ix_material_modules_subject ON material_modules(subject_id, order_no, id);

ALTER TABLE materials ADD COLUMN module_id UUID REFERENCES material_modules(id);

-- Học liệu có từ trước được gom vào "Module 1" của môn.
INSERT INTO material_modules (id, subject_id, title, order_no, created_by, created_at, updated_at, version)
SELECT gen_random_uuid(), m.subject_id, 'Module 1', 0,
       (array_agg(m.created_by ORDER BY m.created_at, m.id))[1], min(m.created_at), min(m.created_at), 0
FROM materials m
GROUP BY m.subject_id;

UPDATE materials m SET module_id = mm.id FROM material_modules mm WHERE mm.subject_id = m.subject_id;

ALTER TABLE materials ALTER COLUMN module_id SET NOT NULL;
CREATE INDEX ix_materials_module ON materials(module_id, status, order_no, id);

CREATE TABLE material_module_quizzes (
    module_id UUID NOT NULL REFERENCES material_modules(id) ON DELETE CASCADE,
    quiz_id UUID NOT NULL REFERENCES quizzes(id),
    order_no INTEGER NOT NULL CHECK (order_no >= 0),
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (module_id, quiz_id),
    CONSTRAINT uq_material_module_quizzes_quiz UNIQUE (quiz_id)
);

do $$ begin
    if exists (select 1 from pg_roles where rolname = 'app') then
        grant select, insert, update, delete on material_modules, material_module_quizzes to app;
    end if;
end $$;
