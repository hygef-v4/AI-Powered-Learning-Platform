package edu.aiplatform.ai.service;

import edu.aiplatform.ai.model.McpCredential;
import edu.aiplatform.ai.model.McpCredential.Access;
import edu.aiplatform.ai.repository.McpCredentialRepository;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Credential MCP cho agent của người dùng (F4, F5; BR-U06-30, 31): token 256 bit hiện một lần, lưu SHA-256 và 4 ký tự
 * cuối; hạn 7, 30 hoặc 90 ngày; tối đa 5 còn hiệu lực. Xác thực mỗi lời gọi dùng role và trạng thái tài khoản hiện
 * hành; đổi mật khẩu không thu hồi credential.
 */
@Service
public class McpCredentialService {

    public static final int MAX_ACTIVE = 5;
    static final Set<Integer> DAYS = Set.of(7, 30, 90);
    static final Set<Role> AGENT_ROLES = EnumSet.of(Role.STUDENT, Role.TEACHER, Role.SUBJECT_MANAGER);
    static final String PREFIX = "mcp_";

    private final McpCredentialRepository repository;
    private final IAccountLookup accounts;
    private final AiAccess access;
    private final IAudit audit;
    private final Clock clock;
    private final KeyCipher cipher;
    private final SecureRandom random = new SecureRandom();

    public McpCredentialService(McpCredentialRepository repository, IAccountLookup accounts, AiAccess access, IAudit audit,
                                Clock clock, KeyCipher cipher) {
        this.repository = repository;
        this.accounts = accounts;
        this.access = access;
        this.audit = audit;
        this.clock = clock;
        this.cipher = cipher;
    }

    @Transactional(readOnly = true)
    public List<CredentialView> list(Actor actor) {
        access.require(actor, Actions.MCP_SELF);
        Instant now = clock.instant();
        Set<UUID> revealable = repository.revealableIds(actor.accountId());
        return repository.listOwn(actor.accountId()).stream().map(c -> view(c, now, revealable.contains(c.id()))).toList();
    }

    @Transactional
    public Created create(Actor actor, String label, Access accessLevel, Integer days) {
        access.require(actor, Actions.MCP_SELF);
        String name = validLabel(label);
        validAccess(accessLevel);
        int validity = days == null ? 30 : days;
        if (!DAYS.contains(validity)) {
            throw ApiException.badRequest("MCP_EXPIRY_INVALID", "Hạn dùng là 7, 30 hoặc 90 ngày");
        }
        Instant now = clock.instant();
        if (repository.countActive(actor.accountId(), now) >= MAX_ACTIVE) {
            throw ApiException.conflict("MCP_LIMIT_REACHED", "Bạn đã có 5 agent còn hiệu lực; hãy thu hồi bớt");
        }
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        String token = PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        McpCredential credential = new McpCredential(UUID.randomUUID(), actor.accountId(), name, accessLevel, hash(token),
                token.substring(token.length() - 4), now.plus(Duration.ofDays(validity)), null, null, now);
        repository.insert(credential);
        boolean revealable = storeSecret(credential.id(), token);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("label", name);
        details.put("access", accessLevel.name());
        details.put("days", validity);
        details.put("tokenHint", credential.tokenHint());
        record(actor.accountId(), "MCP_CREDENTIAL_CREATED", credential.id(), details);
        return new Created(view(credential, now, revealable), token);
    }

    /** Sửa nhãn và quyền của agent còn hiệu lực; token, hạn dùng giữ nguyên. */
    @Transactional
    public CredentialView update(Actor actor, UUID id, String label, Access accessLevel) {
        access.require(actor, Actions.MCP_SELF);
        String name = validLabel(label);
        validAccess(accessLevel);
        Instant now = clock.instant();
        McpCredential credential = repository.findOwn(actor.accountId(), id)
                .filter(c -> c.status(now) == McpCredential.Status.ACTIVE).orElseThrow(AiAccess::notFound);
        repository.update(id, name, accessLevel);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("label", name);
        details.put("access", accessLevel.name());
        details.put("tokenHint", credential.tokenHint());
        record(actor.accountId(), "MCP_CREDENTIAL_UPDATED", id, details);
        McpCredential next = new McpCredential(id, credential.accountId(), name, accessLevel, credential.tokenHash(),
                credential.tokenHint(), credential.expiresAt(), credential.revokedAt(), credential.lastUsedAt(),
                credential.createdAt());
        return view(next, now, repository.findSecret(id).isPresent());
    }

    /** Token rõ của agent còn hiệu lực, cho chủ agent xem lại trong popup Sửa; mỗi lần xem ghi audit. */
    @Transactional
    public String reveal(Actor actor, UUID id) {
        access.require(actor, Actions.MCP_SELF);
        Instant now = clock.instant();
        McpCredential credential = repository.findOwn(actor.accountId(), id)
                .filter(c -> c.status(now) == McpCredential.Status.ACTIVE).orElseThrow(AiAccess::notFound);
        String token = cipher.decrypt(repository.findSecret(id).orElseThrow(AiAccess::notFound));
        record(actor.accountId(), "MCP_TOKEN_REVEALED", id, Map.of("tokenHint", credential.tokenHint()));
        return token;
    }

    /** Lưu bản mã token; chưa cấu hình khóa mã hóa thì bỏ qua (agent vẫn dùng được, chỉ không xem lại token). */
    private boolean storeSecret(UUID id, String token) {
        try {
            repository.saveSecret(id, cipher.encrypt(token));
            return true;
        } catch (ApiException e) {
            return false;
        }
    }

    private static String validLabel(String label) {
        String name = label == null ? "" : label.trim();
        if (name.isEmpty() || name.length() > 60) {
            throw ApiException.badRequest("MCP_LABEL_INVALID", "Nhãn cần 1–60 ký tự");
        }
        return name;
    }

    private static void validAccess(Access accessLevel) {
        if (accessLevel == null) {
            throw ApiException.badRequest("MCP_ACCESS_INVALID", "Cần chọn quyền cho agent");
        }
    }

    @Transactional
    public void revoke(Actor actor, UUID id) {
        access.require(actor, Actions.MCP_SELF);
        McpCredential credential = repository.findOwn(actor.accountId(), id).orElseThrow(AiAccess::notFound);
        if (repository.revoke(id, clock.instant()) == 1) {
            record(actor.accountId(), "MCP_REVOKED", credential.id(), Map.of("tokenHint", credential.tokenHint()));
        }
    }

    /**
     * Xác thực bearer token của agent: băm rồi tìm, so sánh thời gian hằng (NFR-U06-31); credential còn hiệu lực;
     * tài khoản còn {@code ACTIVE} với role được dùng MCP (BR-U06-31). Sai thì rỗng, không nói lý do.
     */
    @Transactional
    public Optional<McpPrincipal> authenticate(String token) {
        if (token == null || !token.startsWith(PREFIX) || token.length() > 200) {
            return Optional.empty();
        }
        String hash = hash(token);
        Instant now = clock.instant();
        Optional<McpCredential> found = repository.findByHash(hash)
                .filter(c -> MessageDigest.isEqual(c.tokenHash().getBytes(StandardCharsets.US_ASCII),
                        hash.getBytes(StandardCharsets.US_ASCII)))
                .filter(c -> c.status(now) == McpCredential.Status.ACTIVE);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Optional<AccountContact> contact;
        try {
            contact = accounts.getContact(found.get().accountId());
        } catch (RuntimeException e) {
            return Optional.empty();
        }
        if (contact.isEmpty() || contact.get().status() != AccountStatus.ACTIVE || !AGENT_ROLES.contains(contact.get().role())) {
            return Optional.empty();
        }
        repository.touch(found.get().id(), now);
        return Optional.of(new McpPrincipal(found.get(), new Actor(contact.get().accountId(), contact.get().role())));
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void record(UUID actorId, String action, UUID credentialId, Map<String, Object> details) {
        audit.record(new AuditEvent(UUID.randomUUID(), actorId, action, "MCP_CREDENTIAL", credentialId.toString(),
                AuditResult.SUCCESS, null, details, clock.instant()));
    }

    static CredentialView view(McpCredential c, Instant now, boolean revealable) {
        return new CredentialView(c.id(), c.label(), c.access(), c.tokenHint(), c.createdAt(), c.expiresAt(),
                c.lastUsedAt(), c.status(now), revealable);
    }

    /** {@code revealable}: có bản mã token, chủ agent xem lại được khi còn hiệu lực. */
    public record CredentialView(UUID id, String label, Access access, String tokenHint, Instant createdAt,
                                 Instant expiresAt, Instant lastUsedAt, McpCredential.Status status,
                                 boolean revealable) {
    }

    /** Token rõ trả lúc tạo; sau đó chỉ xem lại qua {@link #reveal}. */
    public record Created(CredentialView credential, String token) {

        @Override
        public String toString() {
            return "Created[credential=" + credential + "]";
        }
    }

    /** Người gọi MCP: actor dựng từ tài khoản của credential, không nhận từ tham số (P5). */
    public record McpPrincipal(McpCredential credential, Actor actor) {
    }
}
