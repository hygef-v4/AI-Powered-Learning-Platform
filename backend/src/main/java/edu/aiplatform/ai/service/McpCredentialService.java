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
 * hành; đổi mật khẩu không thu hồi credential. Token rõ chỉ trả một lần khi tạo, server không lưu bản có thể giải mã.
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
    private final SecureRandom random = new SecureRandom();

    public McpCredentialService(McpCredentialRepository repository, IAccountLookup accounts, AiAccess access, IAudit audit,
                                Clock clock) {
        this.repository = repository;
        this.accounts = accounts;
        this.access = access;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CredentialView> listCredentials(Actor actor) {
        access.require(actor, Actions.MCP_SELF);
        Instant now = clock.instant();
        return repository.listOwn(actor.accountId()).stream().map(c -> view(c, now)).toList();
    }

    @Transactional
    public Created createCredential(Actor actor, String label, Access accessLevel, Integer days) {
        access.require(actor, Actions.MCP_SELF);
        String name = requireValidLabel(label);
        requireValidAccess(accessLevel);
        int validity = days == null ? 30 : days;
        if (!DAYS.contains(validity)) {
            throw ApiException.createBadRequest("MCP_EXPIRY_INVALID", "Hạn dùng là 7, 30 hoặc 90 ngày");
        }
        repository.lockCredentialAccount(actor.accountId());
        Instant now = clock.instant();
        if (repository.countActive(actor.accountId(), now) >= MAX_ACTIVE) {
            throw ApiException.createConflict("MCP_LIMIT_REACHED", "Bạn đã có 5 agent còn hiệu lực; hãy thu hồi bớt");
        }
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        String token = PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        McpCredential credential = new McpCredential(UUID.randomUUID(), actor.accountId(), name, accessLevel, hash(token),
                token.substring(token.length() - 4), now.plus(Duration.ofDays(validity)), null, null, now);
        repository.insert(credential);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("label", name);
        details.put("access", accessLevel.name());
        details.put("days", validity);
        details.put("tokenHint", credential.tokenHint());
        record(actor.accountId(), "MCP_CREDENTIAL_CREATED", credential.id(), details);
        return new Created(view(credential, now), token);
    }

    /**
     * Sửa nhãn và quyền của agent còn hiệu lực; token, hạn dùng giữ nguyên. Chỉ được hạ quyền: nâng một token đang
     * dùng lên đọc-ghi sẽ trao quyền ghi cho cả bản token có thể đã lộ, nên muốn quyền ghi thì tạo agent mới.
     */
    @Transactional
    public CredentialView updateCredential(Actor actor, UUID id, String label, Access accessLevel) {
        access.require(actor, Actions.MCP_SELF);
        String name = requireValidLabel(label);
        requireValidAccess(accessLevel);
        Instant now = clock.instant();
        McpCredential credential = repository.findOwn(actor.accountId(), id)
                .filter(c -> c.resolveStatus(now) == McpCredential.Status.ACTIVE).orElseThrow(AiAccess::createNotFoundError);
        if (credential.access() == Access.READ_ONLY && accessLevel == Access.READ_WRITE) {
            throw ApiException.createBadRequest("MCP_ACCESS_UPGRADE_NOT_ALLOWED",
                    "Không nâng được agent chỉ đọc lên đọc và ghi; hãy tạo agent mới với quyền đọc và ghi");
        }
        repository.updateCredential(id, name, accessLevel);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("label", name);
        details.put("access", accessLevel.name());
        details.put("tokenHint", credential.tokenHint());
        record(actor.accountId(), "MCP_CREDENTIAL_UPDATED", id, details);
        McpCredential next = new McpCredential(id, credential.accountId(), name, accessLevel, credential.tokenHash(),
                credential.tokenHint(), credential.expiresAt(), credential.revokedAt(), credential.lastUsedAt(),
                credential.createdAt());
        return view(next, now);
    }

    private static String requireValidLabel(String label) {
        String name = label == null ? "" : label.trim();
        if (name.isEmpty() || name.length() > 60) {
            throw ApiException.createBadRequest("MCP_LABEL_INVALID", "Nhãn cần 1–60 ký tự");
        }
        return name;
    }

    private static void requireValidAccess(Access accessLevel) {
        if (accessLevel == null) {
            throw ApiException.createBadRequest("MCP_ACCESS_INVALID", "Cần chọn quyền cho agent");
        }
    }

    @Transactional
    public void revoke(Actor actor, UUID id) {
        access.require(actor, Actions.MCP_SELF);
        McpCredential credential = repository.findOwn(actor.accountId(), id).orElseThrow(AiAccess::createNotFoundError);
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
                .filter(c -> c.resolveStatus(now) == McpCredential.Status.ACTIVE);
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
        // A minute is precise enough for "last used"; writing it on every call costs one UPDATE per tool call.
        Instant lastUsedAt = found.get().lastUsedAt();
        if (lastUsedAt == null || lastUsedAt.isBefore(now.minus(Duration.ofMinutes(1)))) {
            repository.touch(found.get().id(), now);
        }
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

    static CredentialView view(McpCredential c, Instant now) {
        return new CredentialView(c.id(), c.label(), c.access(), c.tokenHint(), c.createdAt(), c.expiresAt(),
                c.lastUsedAt(), c.resolveStatus(now));
    }

    public record CredentialView(UUID id, String label, Access access, String tokenHint, Instant createdAt,
                                 Instant expiresAt, Instant lastUsedAt, McpCredential.Status status) {
    }

    /** Token rõ chỉ trả lúc tạo; mất token thì tạo agent mới. */
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
