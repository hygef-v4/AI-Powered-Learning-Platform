-- U13 owns reference materials; storage objects remain private to U03.
CREATE TABLE materials (
    id UUID PRIMARY KEY,
    scope VARCHAR(10) NOT NULL CHECK (scope IN ('SUBJECT','CLASS')),
    subject_id UUID NOT NULL REFERENCES subjects(id),
    class_id UUID REFERENCES course_classes(id),
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('FILE','YOUTUBE_LINK')),
    title VARCHAR(200) NOT NULL CHECK (length(trim(title)) > 0),
    description VARCHAR(2000) NOT NULL DEFAULT '',
    order_no INTEGER NOT NULL CHECK (order_no >= 0),
    status VARCHAR(10) NOT NULL CHECK (status IN ('ACTIVE','ARCHIVED')),
    current_version_no INTEGER NOT NULL CHECK (current_version_no >= 1),
    created_by UUID NOT NULL REFERENCES accounts(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL CHECK (updated_at >= created_at),
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    CHECK ((scope = 'SUBJECT' AND class_id IS NULL) OR (scope = 'CLASS' AND class_id IS NOT NULL))
);
CREATE INDEX ix_materials_subject ON materials(subject_id, scope, status, order_no, id);
CREATE INDEX ix_materials_class ON materials(class_id, status, order_no, id);
CREATE TABLE material_versions (
    material_id UUID NOT NULL REFERENCES materials(id),
    version_no INTEGER NOT NULL CHECK (version_no >= 1),
    file_id TEXT,
    file_name VARCHAR(255),
    mime_type VARCHAR(150),
    byte_size BIGINT CHECK (byte_size > 0 AND byte_size <= 52428800),
    video_url VARCHAR(500),
    video_id VARCHAR(11),
    text_status VARCHAR(12) CHECK (text_status IN ('PENDING','EXTRACTED','NO_TEXT','FAILED')),
    extracted_text TEXT CHECK (length(extracted_text) <= 1000000),
    created_by UUID NOT NULL REFERENCES accounts(id),
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(material_id, version_no),
    CHECK ((file_id IS NOT NULL AND file_name IS NOT NULL AND mime_type IS NOT NULL
        AND byte_size IS NOT NULL AND text_status IS NOT NULL AND video_id IS NULL AND video_url IS NULL)
        OR (file_id IS NULL AND file_name IS NULL AND mime_type IS NULL AND byte_size IS NULL
        AND text_status IS NULL AND extracted_text IS NULL AND video_id IS NOT NULL AND video_url IS NOT NULL)),
    CHECK (video_id IS NULL OR video_id ~ '^[A-Za-z0-9_-]{11}$')
);
CREATE INDEX ix_material_versions_pending ON material_versions(text_status, created_at);
GRANT SELECT, INSERT, UPDATE ON materials, material_versions TO app;
REVOKE DELETE ON materials, material_versions FROM app;
