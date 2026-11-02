package edu.aiplatform.ai.service;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.model.AiConnection;
import edu.aiplatform.ai.model.AiConnection.Status;
import edu.aiplatform.ai.model.AiSourcePreference;
import edu.aiplatform.ai.repository.AiConnectionRepository;
import edu.aiplatform.ai.repository.AiSourcePreferenceRepository;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nguồn AI người dùng chọn được (popup Chọn nguồn AI, BR-U06-27): API của trường kèm quota còn lại (chỉ giảng viên)
 * và API cá nhân theo từng provider đã lưu; nguồn không dùng được kèm lý do. Nguồn mặc định của người dùng
 * (BR-U06-28) được đánh dấu {@code preferred}; không có mặc định thì không chọn sẵn nguồn nào.
 */
@Service
public class AiSourceService {

    private final AiConnectionRepository connections;
    private final AiSourcePreferenceRepository preferences;
    private final QuotaReservationService quota;
    private final AiAccess access;
    private final Clock clock;

    public AiSourceService(AiConnectionRepository connections, AiSourcePreferenceRepository preferences,
                           QuotaReservationService quota, AiAccess access, Clock clock) {
        this.connections = connections;
        this.preferences = preferences;
        this.quota = quota;
        this.access = access;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SourceOption> sources(Actor actor) {
        access.require(actor, Actions.AI_PERSONAL);
        Optional<AiSourcePreference> preferred = preference(actor);
        List<SourceOption> options = new ArrayList<>();
        // Student, Admin không dùng API của trường (BR-U06-11): popup chỉ có API cá nhân.
        if (access.allowed(actor, Actions.AI_SHARED_USE)) {
            options.add(shared(actor, preferred.filter(p -> p.source() == AiSource.SHARED).isPresent()));
        }
        for (AiProvider provider : AiProvider.values()) {
            boolean mine = preferred.filter(p -> p.source() == AiSource.PERSONAL && p.provider() == provider).isPresent();
            Optional<AiConnection> personal = connections.findPersonal(actor.accountId(), provider)
                    .filter(c -> c.status() == Status.ACTIVE);
            options.add(personal.map(c -> new SourceOption(AiSource.PERSONAL, provider, c.model(), c.keyHint(), true, null,
                            null, "SELF", mine))
                    .orElseGet(() -> new SourceOption(AiSource.PERSONAL, provider, null, null, false,
                            "Chưa lưu khóa API cho nhà cung cấp này", null, "SELF", mine)));
        }
        return options;
    }

    /** Nguồn mặc định; API chung mà người dùng không còn quyền dùng thì coi như chưa đặt. */
    @Transactional(readOnly = true)
    public Optional<AiSourcePreference> preference(Actor actor) {
        access.require(actor, Actions.AI_PERSONAL);
        return preferences.find(actor.accountId())
                .filter(p -> p.source() != AiSource.SHARED || access.allowed(actor, Actions.AI_SHARED_USE));
    }

    /**
     * Đặt nguồn mặc định (BR-U06-28); {@code source} rỗng là hỏi mỗi lần. Không đòi nguồn đang dùng được: lúc dùng
     * popup báo lý do và không tự đổi sang nguồn khác (BR-U06-10).
     */
    @Transactional
    public Optional<AiSourcePreference> savePreference(Actor actor, AiSource source, AiProvider provider) {
        access.require(actor, Actions.AI_PERSONAL);
        if (source == null) {
            preferences.delete(actor.accountId());
            return Optional.empty();
        }
        if (source == AiSource.SHARED) {
            access.require(actor, Actions.AI_SHARED_USE);
        } else if (provider == null) {
            throw ApiException.badRequest("AI_PREFERENCE_INVALID", "Chọn nhà cung cấp cho API cá nhân");
        }
        AiSourcePreference preference = new AiSourcePreference(source, source == AiSource.SHARED ? null : provider);
        preferences.save(actor.accountId(), preference, clock.instant());
        return Optional.of(preference);
    }

    private SourceOption shared(Actor actor, boolean preferred) {
        Optional<AiConnection> connection = Arrays.stream(AiProvider.values()).map(connections::findInstitutional)
                .flatMap(Optional::stream).filter(c -> c.status() == Status.ACTIVE).findFirst();
        if (connection.isEmpty()) {
            return new SourceOption(AiSource.SHARED, null, null, null, false, "API của trường chưa được bật", null,
                    "SCHOOL", preferred);
        }
        QuotaReservationService.Remaining remaining = quota.remaining(actor.accountId());
        if (remaining == null) {
            return new SourceOption(AiSource.SHARED, connection.get().provider(), connection.get().model(), null, false,
                    "API của trường chưa được cấu hình hạn mức", null, "SCHOOL", preferred);
        }
        Quota q = new Quota(remaining.used(), remaining.limit(), remaining.left(), remaining.resetAt());
        String reason = remaining.limit() == 0 ? "Bạn không được dùng API của trường"
                : remaining.left() <= 0 ? "Đã hết hạn mức hôm nay" : null;
        return new SourceOption(AiSource.SHARED, connection.get().provider(), connection.get().model(), null,
                reason == null, reason, q, "SCHOOL", preferred);
    }

    /**
     * {@code costBearer}: {@code SCHOOL} (trường chịu) hoặc {@code SELF} (người dùng chịu chi phí trên key).
     * {@code preferred}: nguồn mặc định của người dùng.
     */
    public record SourceOption(AiSource source, AiProvider provider, String model, String keyHint, boolean available,
                               String reason, Quota quota, String costBearer, boolean preferred) {
    }

    public record Quota(long used, long limit, long left, ZonedDateTime resetAt) {
    }
}
