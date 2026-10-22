package edu.aiplatform.ai.application;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.domain.AiConnection;
import edu.aiplatform.ai.domain.AiConnection.Status;
import edu.aiplatform.ai.domain.AiConnectionRepository;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nguồn AI người dùng chọn được (popup Chọn nguồn AI, BR-U06-27): API của trường kèm quota còn lại (chỉ giảng viên)
 * và API cá nhân theo từng provider đã lưu; nguồn không dùng được kèm lý do. Không chọn sẵn nguồn nào.
 */
@Service
public class AiSourceService {

    private final AiConnectionRepository connections;
    private final QuotaReservationService quota;
    private final AiAccess access;

    public AiSourceService(AiConnectionRepository connections, QuotaReservationService quota, AiAccess access) {
        this.connections = connections;
        this.quota = quota;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<SourceOption> sources(Actor actor) {
        access.require(actor, Actions.AI_PERSONAL);
        List<SourceOption> options = new ArrayList<>();
        options.add(shared(actor));
        for (AiProvider provider : AiProvider.values()) {
            Optional<AiConnection> personal = connections.findPersonal(actor.accountId(), provider)
                    .filter(c -> c.status() == Status.ACTIVE);
            options.add(personal.map(c -> new SourceOption(AiSource.PERSONAL, provider, c.model(), c.keyHint(), true, null,
                            null, "SELF"))
                    .orElseGet(() -> new SourceOption(AiSource.PERSONAL, provider, null, null, false,
                            "Chưa lưu khóa API cho nhà cung cấp này", null, "SELF")));
        }
        return options;
    }

    private SourceOption shared(Actor actor) {
        if (!access.allowed(actor, Actions.AI_SHARED_USE)) {
            return new SourceOption(AiSource.SHARED, null, null, null, false,
                    "Chỉ giảng viên dùng API của trường", null, "SCHOOL");
        }
        Optional<AiConnection> connection = Arrays.stream(AiProvider.values()).map(connections::findInstitutional)
                .flatMap(Optional::stream).filter(c -> c.status() == Status.ACTIVE).findFirst();
        if (connection.isEmpty()) {
            return new SourceOption(AiSource.SHARED, null, null, null, false, "API của trường chưa được bật", null,
                    "SCHOOL");
        }
        QuotaReservationService.Remaining remaining = quota.remaining(actor.accountId());
        if (remaining == null) {
            return new SourceOption(AiSource.SHARED, connection.get().provider(), connection.get().model(), null, false,
                    "API của trường chưa được cấu hình hạn mức", null, "SCHOOL");
        }
        Quota q = new Quota(remaining.used(), remaining.limit(), remaining.left(), remaining.resetAt());
        String reason = remaining.limit() == 0 ? "Bạn không được dùng API của trường"
                : remaining.left() <= 0 ? "Đã hết hạn mức hôm nay" : null;
        return new SourceOption(AiSource.SHARED, connection.get().provider(), connection.get().model(), null,
                reason == null, reason, q, "SCHOOL");
    }

    /** {@code costBearer}: {@code SCHOOL} (trường chịu) hoặc {@code SELF} (người dùng chịu chi phí trên key). */
    public record SourceOption(AiSource source, AiProvider provider, String model, String keyHint, boolean available,
                               String reason, Quota quota, String costBearer) {
    }

    public record Quota(long used, long limit, long left, ZonedDateTime resetAt) {
    }
}
