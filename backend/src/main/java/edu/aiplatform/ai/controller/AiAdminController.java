package edu.aiplatform.ai.controller;

import edu.aiplatform.ai.service.AiConnectionService;
import edu.aiplatform.ai.service.AiConnectionService.ConnectionView;
import edu.aiplatform.ai.service.AiConnectionService.SaveCommand;
import edu.aiplatform.ai.service.QuotaPolicyService;
import edu.aiplatform.ai.service.QuotaPolicyService.TeacherQuotaView;
import edu.aiplatform.ai.service.QuotaPolicyService.UsageSummary;
import edu.aiplatform.ai.model.QuotaPolicy;
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

    private final AiConnectionService aiConnectionService;
    private final QuotaPolicyService quotaPolicyService;

    public AiAdminController(AiConnectionService aiConnectionService, QuotaPolicyService quotaPolicyService) {
        this.aiConnectionService = aiConnectionService;
        this.quotaPolicyService = quotaPolicyService;
    }

    public record StatusRequest(Boolean enabled, Long version) {
    }

    /** Khóa không được in ra log: {@code toString} chỉ có provider, model và id. */
    public record TestRequest(String provider, String apiKey, String model, UUID id) {

        @Override
        public String toString() {
            return "TestRequest[provider=" + provider + ", model=" + model + ", id=" + id + "]";
        }
    }

    /** Khóa không được in ra log: {@code toString} chỉ có provider và model. */
    public record AddRequest(String provider, String apiKey, String model) {

        @Override
        public String toString() {
            return "AddRequest[provider=" + provider + ", model=" + model + "]";
        }
    }

    public record PolicyRequest(Long defaultDayTokens, Long globalDayTokens, Long version) {
    }

    public record PolicyView(Long defaultDayTokens, Long globalDayTokens, long version, Instant updatedAt,
                             boolean configured) {

        static PolicyView mapPolicyView(QuotaPolicy p) {
            return new PolicyView(p.defaultDayTokens(), p.globalDayTokens(), p.version(), p.updatedAt(), p.hasLimits());
        }
    }

    public record TeacherQuotaRequest(Long dayTokens) {
    }

    public record TeacherCandidate(UUID accountId, String name, String email) {
    }

    @GetMapping("/connections")
    public List<ConnectionView> listConnections(@AuthenticationPrincipal Actor actor) {
        return aiConnectionService.listInstitutional(actor);
    }

    /** Thêm một khóa chung; một provider có thể có nhiều khóa. */
    @PostMapping("/connections")
    public ConnectionView addConnection(@AuthenticationPrincipal Actor actor, @RequestBody AddRequest request) {
        return aiConnectionService.addInstitutional(actor, AiWebSupport.parseProvider(request.provider()),
                new SaveCommand(request.apiKey(), request.model(), null));
    }

    /** Kiểm tra trong popup trước khi lưu; {@code id} có khi đang sửa khóa đã lưu và để trống ô khóa. */
    @PostMapping("/connections:test")
    public AiConnectionService.TestResult testDraft(@AuthenticationPrincipal Actor actor, @RequestBody TestRequest request) {
        return aiConnectionService.testInstitutionalDraft(actor, AiWebSupport.parseProvider(request.provider()), request.id(),
                new SaveCommand(request.apiKey(), request.model(), null));
    }

    @PutMapping("/connections/{id}")
    public ConnectionView saveConnection(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                               @RequestBody SaveCommand command) {
        return aiConnectionService.replaceInstitutional(actor, id, command);
    }

    @PostMapping("/connections/{id}:test")
    public ConnectionView test(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return aiConnectionService.testInstitutional(actor, id);
    }

    @PutMapping("/connections/{id}/status")
    public ConnectionView updateConnectionStatus(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                 @RequestBody StatusRequest request) {
        return aiConnectionService.setInstitutionalEnabled(actor, id, Boolean.TRUE.equals(request.enabled()), request.version());
    }

    @DeleteMapping("/connections/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        aiConnectionService.revokeInstitutional(actor, id);
    }

    @GetMapping("/quota-policy")
    public PolicyView loadPolicy(@AuthenticationPrincipal Actor actor) {
        return PolicyView.mapPolicyView(quotaPolicyService.loadPolicy(actor));
    }

    @PutMapping("/quota-policy")
    public PolicyView savePolicy(@AuthenticationPrincipal Actor actor, @RequestBody PolicyRequest request) {
        return PolicyView.mapPolicyView(quotaPolicyService.savePolicy(actor, request.defaultDayTokens(), request.globalDayTokens(),
                request.version()));
    }

    @GetMapping("/teacher-quotas")
    public List<TeacherQuotaView> listTeacherQuotas(@AuthenticationPrincipal Actor actor) {
        return quotaPolicyService.listTeacherQuotas(actor);
    }

    @PutMapping("/teacher-quotas/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setTeacherQuota(@AuthenticationPrincipal Actor actor, @PathVariable UUID accountId,
                                @RequestBody TeacherQuotaRequest request) {
        quotaPolicyService.setTeacherQuota(actor, accountId, request.dayTokens());
    }

    @DeleteMapping("/teacher-quotas/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeacherQuota(@AuthenticationPrincipal Actor actor, @PathVariable UUID accountId) {
        quotaPolicyService.deleteTeacherQuota(actor, accountId);
    }

    /** Tìm giảng viên để đặt mức riêng (tối đa 20 kết quả, IAccountLookup). */
    @GetMapping("/teacher-candidates")
    public List<TeacherCandidate> listTeacherCandidates(@AuthenticationPrincipal Actor actor,
                                                    @RequestParam(defaultValue = "") String q) {
        return quotaPolicyService.listTeacherCandidates(actor, q).stream()
                .map(c -> new TeacherCandidate(c.accountId(), c.displayName(), c.email())).toList();
    }

    @GetMapping("/usage-summary")
    public UsageSummary loadUsageSummary(@AuthenticationPrincipal Actor actor, @RequestParam(defaultValue = "7") int days) {
        return quotaPolicyService.summarizeUsage(actor, days);
    }
}
