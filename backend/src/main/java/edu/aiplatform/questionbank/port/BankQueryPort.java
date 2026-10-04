package edu.aiplatform.questionbank.port;

import edu.aiplatform.shared.contract.Page;
import edu.aiplatform.shared.contract.PageRequest;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** U06 cung cấp cho U08, U09, U11, U13, U15. */
public interface BankQueryPort {
    Optional<QuestionVersion> getVersion(UUID questionId);
    Page<QuestionVersion> search(BankScope scope, BankFilter filter, PageRequest page);
    List<QuestionVersion> pickRandom(BankScope scope, BankFilter filter, int n, Set<UUID> excludeIds);
    QuestionStudentView getStudentView(UUID questionId);
}
