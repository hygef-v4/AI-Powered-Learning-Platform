-- Legacy course_classes.show_grade_distribution is dormant; no settings are inherited.
CREATE TABLE grade_distribution_visibility (
 class_id UUID NOT NULL REFERENCES course_classes(id),
 assignment_id UUID NOT NULL REFERENCES assignments(id),
 enabled BOOLEAN NOT NULL DEFAULT FALSE,
 version BIGINT NOT NULL CHECK (version > 0),
 PRIMARY KEY (class_id, assignment_id)
);
