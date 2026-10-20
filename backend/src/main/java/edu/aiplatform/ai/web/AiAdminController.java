package edu.aiplatform.ai.web;

import edu.aiplatform.ai.application.AiConnectionService;
import edu.aiplatform.ai.application.AiConnectionService.ConnectionView;
import edu.aiplatform.ai.application.AiConnectionService.SaveCommand;
import edu.aiplatform.ai.application.QuotaPolicyService;
import edu.aiplatform.ai.application.QuotaPolicyService.TeacherQuotaView;
import edu.aiplatform.ai.application.QuotaPolicyService.UsageSummary;
import edu.aiplatform.ai.domain.QuotaPolicy;
import edu.aiplatform.identity.contract.Actor;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** UC 14, 15, 33 phía Admin: API AI của trường, hạn mức chung, mức riêng giảng viên, tổng hợp sử dụng. */
@RestController
@RequestMapping("/api/v1/admin/ai")
public class AiAdminController {

    private final AiConnectionService connections;
    private final QuotaPolicyService quota;

    public AiAdminController(AiConnectionService connections, QuotaPolicyService quota) {
        this.connections = connections;
        this.quota = quota;
    }

    public record StatusRequest(Boolean enabled, Long version) {
    }

    public record PolicyRequest(Long defaultDayTokens, Long globalDayTokens, Long version) {
    }

    public record PolicyView(Long defaultDayTokens, Long globalDayTokens, long version, Instant updatedAt,
                             boolean configured) {

        static PolicyView of(QuotaPolicy p) {
            return new PolicyView(p.defaultDayTokens(), p.globalDayTokens(), p.version(), p.updatedAt(), p.configured());
        }
    }

    public record TeacherQuotaRequest(Long dayTokens) {
    }

    public record TeacherCandidate(UUID accountId, String name, String email) {
    }

    @GetMapping("/connections")
    public List<ConnectionView> connections(@AuthenticationPrincipal Actor actor) {
        return connections.listInstitutional(actor);
    }

    @PutMapping("/connections/{provider}")
    public ConnectionView save(@AuthenticationPrincipal Actor actor, @PathVariable String provider,
                               @RequestBody SaveCommand command) {
        return connections.saveInstitutional(actor, AiWebSupport.provider(provider), command);
    }

    @PostMapping("/connections/{provider}:test")
    public ConnectionView test(@AuthenticationPrincipal Actor actor, @PathVariable String provider) {
        return connections.testInstitutional(actor, AiWebSupport.provider(provider));
    }

    @PutMapping("/connections/{provider}/status")
    public ConnectionView status(@AuthenticationPrincipal Actor actor, @PathVariable String provider,
                                 @RequestBody StatusRequest request) {
        return connections.setInstitutionalEnabled(actor, AiWebSupport.provider(provider),
                Boolean.TRUE.equals(request.enabled()), request.version());
    }

    @DeleteMapping("/connections/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal Actor actor, @PathVariable String provider) {
        connections.revokeInstitutional(actor, AiWebSupport.provider(provider));
    }

    @GetMapping("/quota-policy")
    public PolicyView policy(@AuthenticationPrincipal Actor actor) {
        return PolicyView.of(quota.policy(actor));
    }

    @PutMapping("/quota-policy")
    public PolicyView savePolicy(@AuthenticationPrincipal Actor actor, @RequestBody PolicyRequest request) {
        return PolicyView.of(quota.savePolicy(actor, request.defaultDayTokens(), request.globalDayTokens(),
                request.version()));
    }

    @GetMapping("/teacher-quotas")
    public List<TeacherQuotaView> teacherQuotas(@AuthenticationPrincipal Actor actor) {
        return quota.teacherQuotas(actor);
    }

    @PutMapping("/teacher-quotas/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setTeacherQuota(@AuthenticationPrincipal Actor actor, @PathVariable UUID accountId,
                                @RequestBody TeacherQuotaRequest request) {
        quota.setTeacherQuota(actor, accountId, request.dayTokens());
    }

    @DeleteMapping("/teacher-quotas/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeacherQuota(@AuthenticationPrincipal Actor actor, @PathVariable UUID accountId) {
        quota.deleteTeacherQuota(actor, accountId);
    }

    /** Tìm giảng viên để đặt mức riêng (tối đa 20 kết quả, IAccountLookup). */
    @GetMapping("/teacher-candidates")
    public List<TeacherCandidate> teacherCandidates(@AuthenticationPrincipal Actor actor,
                                                    @RequestParam(defaultValue = "") String q) {
        return quota.teacherCandidates(actor, q).stream()
                .map(c -> new TeacherCandidate(c.accountId(), c.displayName(), c.email())).toList();
    }

    @GetMapping("/usage-summary")
    public UsageSummary usageSummary(@AuthenticationPrincipal Actor actor, @RequestParam(defaultValue = "7") int days) {
        return quota.summary(actor, days);
    }
}
