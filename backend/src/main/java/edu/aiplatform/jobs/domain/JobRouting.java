package edu.aiplatform.jobs.domain;

import edu.aiplatform.jobs.port.JobQueue;
import edu.aiplatform.jobs.port.JobTypes;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Mỗi `jobType` thuộc đúng một trong 7 queue (BR-U03-63) và số luồng xử lý của từng queue (P11).
 * Thêm loại việc nền: thêm hằng vào {@link JobTypes} và một dòng ở đây; không tạo queue mới.
 */
public final class JobRouting {

    public record Route(JobQueue queue, int priority) {}

    private static final Map<String, Route> ROUTES = Map.of(
            JobTypes.OTP_DELIVERY, new Route(JobQueue.EMAIL, 9),
            JobTypes.EMAIL_SEND, new Route(JobQueue.EMAIL, 1),
            JobTypes.GROUP_DOC_CREATE, new Route(JobQueue.TRIGGERED, 0),
            JobTypes.LESSON_SCAN, new Route(JobQueue.GEMINI, 0),
            JobTypes.AI_TASK, new Route(JobQueue.GEMINI, 0),
            JobTypes.YOUTUBE_CAPTION, new Route(JobQueue.YOUTUBE, 0),
            JobTypes.CODE_RUN, new Route(JobQueue.CODE, 0),
            JobTypes.DRIVE_CLEANUP, new Route(JobQueue.DRIVE, 0),
            JobTypes.PAYOS_CHECK, new Route(JobQueue.PAYOS, 0));

    private static final Map<JobQueue, Integer> CONCURRENCY = new EnumMap<>(Map.of(
            JobQueue.TRIGGERED, 2,
            JobQueue.EMAIL, 1,
            JobQueue.GEMINI, 4,
            JobQueue.YOUTUBE, 1,
            JobQueue.CODE, 2,
            JobQueue.DRIVE, 1,
            JobQueue.PAYOS, 1));

    /** `jobs.email` là priority queue: OTP trước email thông báo. */
    public static final int EMAIL_MAX_PRIORITY = 10;

    private JobRouting() {}

    public static boolean isKnown(String jobType) {
        return jobType != null && ROUTES.containsKey(jobType);
    }

    public static Route routeOf(String jobType) {
        Route route = ROUTES.get(jobType);
        if (route == null) {
            throw new IllegalArgumentException("Unknown jobType: " + jobType);
        }
        return route;
    }

    public static List<String> jobTypesOf(JobQueue queue) {
        return ROUTES.entrySet().stream()
                .filter(e -> e.getValue().queue() == queue)
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    public static int concurrencyOf(JobQueue queue) {
        return CONCURRENCY.get(queue);
    }
}
