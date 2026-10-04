package edu.aiplatform.jobs.port;

/** Loại việc nền và queue của chúng. */
public final class JobTypes {
    public static final String OTP_DELIVERY = "OTP_DELIVERY";         // U01, jobs.email
    public static final String EMAIL_SEND = "EMAIL_SEND";             // U16, jobs.email
    public static final String DRIVE_CLEANUP = "DRIVE_CLEANUP";       // U03, jobs.drive
    public static final String LESSON_SCAN = "LESSON_SCAN";           // U05, jobs.gemini
    public static final String YOUTUBE_CAPTION = "YOUTUBE_CAPTION";   // U05, jobs.youtube
    public static final String PAYOS_CHECK = "PAYOS_CHECK";           // U07, jobs.payos
    public static final String GROUP_DOC_CREATE = "GROUP_DOC_CREATE"; // U14, jobs.triggered
    public static final String AI_TASK = "AI_TASK";                   // U13, jobs.gemini
    public static final String CODE_RUN = "CODE_RUN";                 // U13, jobs.code

    private JobTypes() {}
}
