package edu.aiplatform.assignments.service;

import edu.aiplatform.jobs.contract.IScheduledScanner;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Mỗi phút xóa ảnh trang nhập đề còn sót quá 24 giờ (BR-U07-91). */
@Component
public class ImportPageCleanupScanner implements IScheduledScanner {

    private final AssignmentImportService assignmentImportService;

    public ImportPageCleanupScanner(AssignmentImportService assignmentImportService) {
        this.assignmentImportService = assignmentImportService;
    }

    @Override
    public String name() {
        return "assignment-import-pages";
    }

    @Override
    public void scan(Instant now) {
        assignmentImportService.deleteExpiredPages(now);
    }
}
