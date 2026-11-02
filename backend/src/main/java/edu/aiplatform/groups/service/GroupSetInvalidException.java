package edu.aiplatform.groups.service;

import edu.aiplatform.groups.model.GroupSetRules.Issue;
import edu.aiplatform.shared.error.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;

/** Bộ nhóm có chỗ mâu thuẫn: từ chối cả bộ và trả từng lỗi theo nhóm (BR-U12-04). */
public class GroupSetInvalidException extends ApiException {

    private final transient List<Issue> issues;

    public GroupSetInvalidException(List<Issue> issues) {
        super(HttpStatus.BAD_REQUEST, "GROUP_SET_INVALID", "Bộ nhóm có lỗi, chưa lưu gì");
        this.issues = List.copyOf(issues);
    }

    public List<Issue> issues() {
        return issues;
    }
}
