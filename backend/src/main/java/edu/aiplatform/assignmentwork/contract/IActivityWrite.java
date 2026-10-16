package edu.aiplatform.assignmentwork.contract;

import edu.aiplatform.identity.contract.Actor;
import java.util.List;

/** Ghi sự kiện rời trang của lượt Quiz (U11) và tài liệu nhóm (U12) sau khi unit đó đã kiểm quyền. */
public interface IActivityWrite {

    void record(Actor actor, WorkRef workRef, List<ActivityEventInput> events);
}
