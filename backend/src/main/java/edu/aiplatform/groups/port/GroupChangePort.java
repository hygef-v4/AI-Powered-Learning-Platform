package edu.aiplatform.groups.port;

import java.util.UUID;

/** U12 khai báo, U14 cài (`C`); gọi trong transaction lưu nhóm. */
public interface GroupChangePort {
    void onGroupCreated(UUID groupId);
    void onMemberRemoved(UUID groupId, UUID studentId);
    boolean hasGroupWork(UUID groupId);
}
