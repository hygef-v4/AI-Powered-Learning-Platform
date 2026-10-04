package edu.aiplatform.assessments.port;

import java.util.UUID;

/** U08 khai báo, U12 cài: nhóm của lớp hợp lệ trước khi phát hành bài nhóm (BR-U12-21). */
public interface GroupReadinessPort {
    GroupReadiness check(UUID classId);
}
