package edu.aiplatform.jobs.port;

/** Routing key trên exchange `platform.events`. */
public final class EventTypes {
    public static final String ENROLLMENT_ACTIVATED = "enrollment.activated";           // U04
    public static final String CLASS_ANNOUNCEMENT_POSTED = "class.announcement-posted"; // U05
    public static final String PAYMENT_PAID = "payment.paid";                           // U07
    public static final String ASSIGNMENT_OPENED = "assignment.opened";                 // U08
    public static final String GROUP_MEMBERSHIP_CHANGED = "group.membership-changed";   // U12
    public static final String GROUP_LEADER_CHANGED = "group.leader-changed";           // U12
    public static final String GROUP_LEADER_REQUESTED = "group.leader-requested";       // U12
    public static final String GROUP_LEADER_REQUEST_REJECTED = "group.leader-request-rejected"; // U12
    public static final String GROUP_SUBMITTED = "group.submitted";                     // U14
    public static final String GRADE_PUBLISHED = "grade.published";                     // U15

    private EventTypes() {}
}
