ALTER TABLE quizzes ADD COLUMN instructions_document JSONB;
ALTER TABLE quizzes ADD CONSTRAINT ck_quiz_instructions_document
    CHECK (instructions_document IS NULL OR jsonb_typeof(instructions_document) = 'object');
