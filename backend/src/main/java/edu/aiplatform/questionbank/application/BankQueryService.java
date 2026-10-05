package edu.aiplatform.questionbank.application;

import edu.aiplatform.questionbank.port.*;
import edu.aiplatform.shared.contract.Page;
import edu.aiplatform.shared.contract.PageRequest;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U06: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 */
@Service
public class BankQueryService implements BankQueryPort {

    @Override
    public Optional<QuestionVersion> getVersion(UUID questionId) {
        return Optional.empty();
    }

    @Override
    public Page<QuestionVersion> search(BankScope scope, BankFilter filter, PageRequest page) {
        return new Page<>(List.of(), 0, page.page(), page.size());
    }

    @Override
    public List<QuestionVersion> pickRandom(BankScope scope, BankFilter filter, int n, Set<UUID> excludeIds) {
        return List.of();
    }

    @Override
    public QuestionStudentView getStudentView(UUID questionId) {
        throw new UnsupportedOperationException("Chưa cài: U06");
    }
}
