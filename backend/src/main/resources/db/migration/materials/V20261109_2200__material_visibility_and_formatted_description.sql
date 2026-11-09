ALTER TABLE materials ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE materials ADD COLUMN description_document JSONB;
ALTER TABLE materials ADD CONSTRAINT ck_material_description_document
    CHECK (description_document IS NULL OR jsonb_typeof(description_document) = 'object');
CREATE INDEX ix_material_versions_file ON material_versions(file_id) WHERE file_id IS NOT NULL;
