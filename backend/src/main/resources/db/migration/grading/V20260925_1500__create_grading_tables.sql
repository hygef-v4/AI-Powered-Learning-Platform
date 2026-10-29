-- U09 requires the real U07 (1300) and U08 (1400) migrations first.
CREATE TABLE evaluations (
 id UUID PRIMARY KEY,
 assignment_id UUID NOT NULL REFERENCES assignments(id),
 class_id UUID NOT NULL REFERENCES course_classes(id),
 attempt_id UUID UNIQUE REFERENCES assignment_attempts(id),
 group_document_id UUID,
 account_id UUID REFERENCES accounts(id),
 kind VARCHAR(20) NOT NULL CHECK (kind IN ('ATTEMPT','GROUP_DOCUMENT','MEMBER')),
 status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','DRAFT','FINALIZED','PUBLISHED','SUPERSEDED')),
 submission_hash TEXT NOT NULL,
 max_score NUMERIC(6,2) NOT NULL CHECK (max_score >= 0),
 score NUMERIC(6,2) CHECK (score >= 0 AND score <= max_score),
 grading_document JSONB NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 CHECK ((kind='ATTEMPT' AND attempt_id IS NOT NULL AND account_id IS NOT NULL AND group_document_id IS NULL)
 OR (kind='GROUP_DOCUMENT' AND group_document_id IS NOT NULL AND attempt_id IS NULL AND account_id IS NULL)
 OR (kind='MEMBER' AND group_document_id IS NOT NULL AND attempt_id IS NULL AND account_id IS NOT NULL)),
 CHECK (status NOT IN ('DRAFT','FINALIZED','PUBLISHED') OR score IS NOT NULL)
);
CREATE UNIQUE INDEX uq_group_evaluation ON evaluations(group_document_id) WHERE kind='GROUP_DOCUMENT';
CREATE UNIQUE INDEX uq_member_evaluation ON evaluations(group_document_id,account_id) WHERE kind='MEMBER';
CREATE INDEX ix_evaluation_assignment_class_state ON evaluations(assignment_id,class_id,status);
CREATE INDEX ix_evaluation_class_account ON evaluations(class_id,account_id);

CREATE TABLE grading_proposals (
 id UUID PRIMARY KEY,
 evaluation_id UUID NOT NULL REFERENCES evaluations(id),
 version_no BIGINT NOT NULL CHECK (version_no > 0),
 request_ref VARCHAR(500) NOT NULL UNIQUE,
 status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','READY','FAILED','STALE','REJECTED')),
 proposal_document JSONB NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 UNIQUE(evaluation_id,version_no)
);
CREATE INDEX ix_proposal_evaluation_version ON grading_proposals(evaluation_id,version_no);
GRANT SELECT, INSERT, UPDATE ON evaluations,grading_proposals TO app;
REVOKE DELETE ON evaluations,grading_proposals FROM app;
