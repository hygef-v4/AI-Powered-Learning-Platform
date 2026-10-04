package edu.aiplatform.billing.port;

import edu.aiplatform.shared.contract.Page;
import edu.aiplatform.shared.contract.PageRequest;
import java.util.UUID;

/** U07 khai báo, U13 cài (`C`). */
public interface CreditUsagePort {
    Page<CreditUsage> listUsage(UUID accountId, PageRequest page);
}
