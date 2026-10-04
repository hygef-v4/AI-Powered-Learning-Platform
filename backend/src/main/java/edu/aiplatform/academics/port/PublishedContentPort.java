package edu.aiplatform.academics.port;

import java.util.List;
import java.util.UUID;

/** U04 khai báo, U05 cài (`C`): module `ACTIVE` của môn, học liệu `ACTIVE` của môn và của lớp. */
public interface PublishedContentPort {
    List<ModuleView> listForClass(UUID classId);
}
