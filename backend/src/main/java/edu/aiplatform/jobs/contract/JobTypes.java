package edu.aiplatform.jobs.contract;

/** Loại việc nền và unit cài handler (domain-entities U03 mục 5). */
public final class JobTypes {

    /** U01, queue {@code jobs.email} (ưu tiên cao). */
    public static final String OTP_DELIVERY = "OTP_DELIVERY";
    /** U14, queue {@code jobs.email}. */
    public static final String EMAIL_SEND = "EMAIL_SEND";
    /** U06, queue {@code jobs.ai}. */
    public static final String AI_TASK = "AI_TASK";
    /** U12, queue {@code jobs.triggered}. */
    public static final String GROUP_DOC_CREATE = "GROUP_DOC_CREATE";
    /** U13, queue {@code jobs.triggered}. */
    public static final String MATERIAL_TEXT_EXTRACT = "MATERIAL_TEXT_EXTRACT";
    /** U03, queue {@code jobs.drive}. */
    public static final String DRIVE_CLEANUP = "DRIVE_CLEANUP";
    /** U03, queue {@code jobs.backup}. */
    public static final String BACKUP_RUN = "BACKUP_RUN";

    private JobTypes() {
    }
}
