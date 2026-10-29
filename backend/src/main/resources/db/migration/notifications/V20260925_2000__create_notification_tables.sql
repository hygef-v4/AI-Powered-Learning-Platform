-- U14: notification retention is the only direct DELETE permission.
CREATE TABLE announcements (
 id uuid PRIMARY KEY, class_id uuid NOT NULL REFERENCES course_classes(id),
 title varchar(200) NOT NULL, body varchar(5000) NOT NULL, important boolean NOT NULL,
 send_email boolean NOT NULL, status varchar(16) NOT NULL CHECK (status IN ('ACTIVE','DELETED')),
 created_by uuid NOT NULL, created_at timestamptz NOT NULL, updated_by uuid NOT NULL,
 updated_at timestamptz NOT NULL, deleted_by uuid, deleted_at timestamptz, version bigint NOT NULL DEFAULT 0,
 CHECK (length(trim(title)) > 0 AND length(trim(body)) > 0), CHECK (NOT send_email OR important)
);
CREATE INDEX announcements_class_feed ON announcements(class_id,status,created_at DESC,id);
CREATE TABLE notifications (
 id uuid PRIMARY KEY, recipient_id uuid NOT NULL, type varchar(40) NOT NULL, source_event_id uuid NOT NULL,
 title varchar(200) NOT NULL, summary varchar(500) NOT NULL, link varchar(500) NOT NULL,
 read_at timestamptz, created_at timestamptz NOT NULL,
 UNIQUE(source_event_id,recipient_id,type)
);
CREATE INDEX notifications_recipient_feed ON notifications(recipient_id,read_at,created_at DESC,id);
CREATE INDEX notifications_retention ON notifications(created_at);
CREATE TABLE email_deliveries (
 id uuid PRIMARY KEY, notification_id uuid NOT NULL UNIQUE REFERENCES notifications(id) ON DELETE CASCADE,
 recipient_id uuid NOT NULL, type varchar(40) NOT NULL,
 status varchar(16) NOT NULL CHECK(status IN ('PENDING','SENT','FAILED','SKIPPED','DEFERRED')),
 priority integer NOT NULL, scheduled_for timestamptz NOT NULL, attempts integer NOT NULL DEFAULT 0,
 last_error varchar(100), created_at timestamptz NOT NULL, sent_at timestamptz, expires_at timestamptz,
 CHECK(attempts >= 0)
);
CREATE INDEX email_delivery_pending ON email_deliveries(status,scheduled_for,priority DESC,id);
CREATE TABLE notification_preferences (
 account_id uuid NOT NULL, type varchar(40) NOT NULL, email_enabled boolean NOT NULL,
 PRIMARY KEY(account_id,type)
);
CREATE TABLE deadline_reminders (
 assignment_id uuid NOT NULL, class_id uuid NOT NULL, closes_at timestamptz NOT NULL,
 status varchar(16) NOT NULL CHECK(status IN ('SENT','SKIPPED')), processed_at timestamptz NOT NULL,
 PRIMARY KEY(assignment_id,class_id,closes_at)
);
GRANT SELECT, INSERT, UPDATE ON announcements,notifications,email_deliveries,notification_preferences,deadline_reminders TO app;
REVOKE DELETE ON announcements,email_deliveries,notification_preferences,deadline_reminders FROM app;
GRANT DELETE ON notifications TO app;
