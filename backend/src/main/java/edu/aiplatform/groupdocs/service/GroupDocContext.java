package edu.aiplatform.groupdocs.service;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.groupdocs.model.GroupDocument;
import edu.aiplatform.groups.model.ClassGroup;
import java.util.UUID;

/** Tài liệu nhóm kèm nhóm, lớp và vai trò của người gọi đã kiểm quyền (BR-U12-31). */
public record GroupDocContext(GroupDocument document, ClassGroup group, ClassRef classRef, UUID actorId, boolean member,
                              boolean leader, boolean teacher) {
}
