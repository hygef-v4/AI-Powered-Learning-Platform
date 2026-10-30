package edu.aiplatform.subjects.controller;

import edu.aiplatform.subjects.dto.SubjectAdminViews;
import edu.aiplatform.subjects.model.Subject;

public final class SubjectResponses {
    private SubjectResponses() { }

    /** Snapshot vừa commit; contact/count chưa enrichment nên unknown=null, không re-read sau write. */
    public static SubjectAdminViews.Detail detail(Subject subject) {
        return new SubjectAdminViews.Detail(subject.id(), subject.code().value(), subject.name(), subject.status(),
                subject.managerId(), null, null, subject.description(), subject.createdAt(), subject.updatedAt(), subject.version());
    }
}
