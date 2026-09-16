# Main Business Flows

Authoritative source: `../application-design/business-flows.md` (trigger, end condition, Mermaid diagram and text alternative for every flow). The Draw.io diagrams here contain the same steps, decision branches and outcomes.

This directory contains one nine-page draw.io file, nine individual draw.io files, and nine PNG images ready for insertion into the SRS. The PNG files in `exports/` are rendered from the individual draw.io files with the diagrams.net viewer, so they match what draw.io displays.

| ID | Business flow | Swimlanes | End outcomes |
|---|---|---|---|
| BF-01 | Login and Session Management | User, Backend, Redis | Login failed; Logged out; Session expired |
| BF-02 | Subject and Class Setup and Staff Assignment | Administrator, Backend, PostgreSQL | Setup completed |
| BF-03 | Document Upload, AI Summarization, and Lesson Splitting | Instructor or Subject Manager, Backend, Google Drive, RabbitMQ and AI Worker, PostgreSQL | Upload rejected; Processing failed; Lessons published |
| BF-04 | Assignment Authoring, Review, and Publication | Instructor or Subject Manager, Backend, RabbitMQ and AI Worker, PostgreSQL, Student | Assignment published |
| BF-05 | Lesson Access and Progress Tracking | Student, Backend, PostgreSQL, Google Drive | Access denied; Progress saved |
| BF-06 | Group Formation, Work Allocation, and Leader Change | Instructor, Backend, PostgreSQL, Student | Leader unchanged; Leader change decided |
| BF-07 | Individual or Shared Assignment Submission | Student or Leader, Backend, Google Drive, PostgreSQL | Submission rejected; Submission recorded |
| BF-08 | Grading Method Selection and Grade Publication | Instructor, Backend, RabbitMQ and AI Worker, PostgreSQL, Student | Grade received |
| BF-09 | Payment and Access Grant | Student, Backend, Payment Gateway, PostgreSQL | Webhook rejected, status unchanged; Result shown |

## Diagram conventions

- Each page shows only the Horizontal Pool 1 swimlane named after the business flow; trigger, end condition and text alternative live in the authoritative source.
- Every End shape states its business outcome, and every decision branch is labelled.
- Connectors have fixed exit and entry points and never pass through another shape; loop-backs run along the top or bottom edge of a lane.
- Terminology follows the requirements: Subject Manager, staff assignment, Student.
- A PostgreSQL lane appears whenever the flow persists a business result; Redis holds only temporary session state; RabbitMQ and AI Worker share one lane.
