package edu.aiplatform.subjects.dto;

import edu.aiplatform.subjects.contract.SubjectClassSummary;
import edu.aiplatform.subjects.contract.SubjectStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** DTO đọc cho Admin; không mở rộng SubjectRef dùng chung hoặc lộ entity persistence. */
public final class SubjectAdminViews {
    private SubjectAdminViews() { }

    public record Manager(UUID accountId, String displayName, String email) { }

    /** managerId khác null nhưng manager=null nghĩa là chưa tra được, không phải chưa phân công. */
    public record ListItem(UUID id, String code, String name, SubjectStatus status, UUID managerId,
                           Manager manager, Integer classCount) { }

    public record Page(List<ListItem> items, int page, int size, long totalElements,
                       Map<SubjectStatus, Long> statusCounts) {
        public Page {
            items = List.copyOf(items);
            statusCounts = Map.copyOf(statusCounts);
        }
    }

    public record Detail(UUID id, String code, String name, SubjectStatus status, UUID managerId,
                         Manager manager, Integer classCount, String description,
                         Instant createdAt, Instant updatedAt, long version) { }

    public enum Availability { AVAILABLE, UNAVAILABLE }

    /** AVAILABLE + [] là đã xác minh không có lớp; UNAVAILABLE + null là không kiểm được. */
    public record Classes(Availability availability, List<SubjectClassSummary> items) {
        public Classes {
            java.util.Objects.requireNonNull(availability, "availability");
            items = items == null ? null : List.copyOf(items);
            if ((availability == Availability.AVAILABLE) != (items != null)) {
                throw new IllegalArgumentException("Availability phải khớp dữ liệu lớp");
            }
        }
    }
}
