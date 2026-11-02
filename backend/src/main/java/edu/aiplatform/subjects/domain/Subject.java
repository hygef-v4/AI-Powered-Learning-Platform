package edu.aiplatform.subjects.domain;

import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.SubjectStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Trạng thái bất biến của môn. Version là version public, bắt đầu từ 1.
 * Service phải kiểm quyền, tài khoản chủ nhiệm và lớp chưa lưu trữ trước khi gọi các thao tác bên dưới.
 * Domain không gọi U01/U05 hoặc tự ghi audit; repository phải kiểm version khi ghi.
 */
public record Subject(UUID id, SubjectCode code, String name, String description, SubjectStatus status,
                      UUID managerId, Instant createdAt, Instant updatedAt, long version) {

    public Subject {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
        name = validateName(name);
        validateDescription(description);
        if (version < 1) {
            throw new IllegalArgumentException("Version môn phải từ 1");
        }
    }

    public static Subject create(UUID id, String code, String name, String description, UUID managerId, Instant now) {
        return new Subject(id, new SubjectCode(code), name, description, SubjectStatus.ACTIVE,
                managerId, now, now, 1);
    }

    /** Kiểm cả trước no-op: request cũ không được coi là thành công chỉ vì giá trị đang giống nhau. */
    public void requireVersion(long expectedVersion) {
        if (expectedVersion != version) {
            throw ApiException.conflict("SUBJECT_VERSION_CONFLICT", "Dữ liệu môn đã thay đổi. Hãy tải lại trước khi sửa");
        }
    }

    public Subject updateInformation(String name, String description, Instant now) {
        String normalizedName = validateName(name);
        validateDescription(description);
        if (this.name.equals(normalizedName) && Objects.equals(this.description, description)) {
            return this;
        }
        return changed(normalizedName, description, status, managerId, now);
    }

    /** managerId=null nghĩa là bỏ phân công. Service kiểm SUBJECT_MANAGER/ACTIVE trước khi gán. */
    public Subject changeManager(UUID managerId, Instant now) {
        return Objects.equals(this.managerId, managerId) ? this : changed(name, description, status, managerId, now);
    }

    /**
     * Chỉ gọi sau khi service đã khóa môn và xác minh không còn lớp chưa lưu trữ (BR-U04-04).
     * Lưu trữ gỡ luôn Chủ nhiệm môn (BR-U04-11); mở lại không tự gán lại.
     */
    public Subject archive(Instant now) {
        return status == SubjectStatus.ARCHIVED ? this : changed(name, description, SubjectStatus.ARCHIVED, null, now);
    }

    public Subject reopen(Instant now) {
        return status == SubjectStatus.ACTIVE ? this : changed(name, description, SubjectStatus.ACTIVE, managerId, now);
    }

    private Subject changed(String name, String description, SubjectStatus status, UUID managerId, Instant now) {
        return new Subject(id, code, name, description, status, managerId, createdAt,
                Objects.requireNonNull(now, "now"), Math.addExact(version, 1));
    }

    private static String validateName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isBlank() || normalized.length() > 200) {
            throw ApiException.badRequest("SUBJECT_NAME_INVALID", "Tên môn là bắt buộc và tối đa 200 ký tự");
        }
        return normalized;
    }

    private static void validateDescription(String description) {
        if (description != null && description.length() > 2000) {
            throw ApiException.badRequest("SUBJECT_DESCRIPTION_INVALID", "Mô tả môn tối đa 2.000 ký tự");
        }
    }
}
