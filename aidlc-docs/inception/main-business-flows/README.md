# Main Business Flows

Authoritative source: `../application-design/business-flows.md`.

This directory contains one nine-page draw.io file, nine individual draw.io files, and nine PNG images ready for insertion into the SRS.

| ID | Business flow | Swimlanes |
|---|---|---|
| BF-01 | Login and Session Management | User, Backend, Redis |
| BF-02 | Subject and Class Setup and Staff Assignment | Administrator, Backend, PostgreSQL |
| BF-03 | Document Upload, AI Summarization, and Lesson Splitting | Instructor or Subject Owner, Backend, Google Drive, RabbitMQ, AI Worker |
| BF-04 | Assignment Authoring, Review, and Publication | Instructor or Subject Owner, Backend, RabbitMQ, AI Worker, Learner |
| BF-05 | Lesson Access and Progress Tracking | Student, Backend, Google Drive, PostgreSQL |
| BF-06 | Group Formation, Work Allocation, and Leader Change | Instructor, Backend, Student |
| BF-07 | Individual or Shared Assignment Submission | Student or Leader, Backend, Google Drive, PostgreSQL |
| BF-08 | Grading Method Selection and Grade Publication | Instructor, Backend, RabbitMQ and AI Worker, Student |
| BF-09 | Payment and Access Grant | Student, Backend, Payment Gateway, PostgreSQL |

Each diagram includes its trigger, end condition, decision branches, and the corresponding textual alternative described in the authoritative source.
