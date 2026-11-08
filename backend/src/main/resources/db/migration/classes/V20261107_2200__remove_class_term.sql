-- Semester is no longer part of the class domain or API (D-U04-04).
-- Preserve class identities, versions, enrollments and academic references.
ALTER TABLE course_classes DROP COLUMN term;
