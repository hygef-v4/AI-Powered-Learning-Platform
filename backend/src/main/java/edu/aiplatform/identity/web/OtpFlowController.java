package edu.aiplatform.identity.web;

import edu.aiplatform.identity.application.ActivationService;
import edu.aiplatform.identity.application.AuthService;
import edu.aiplatform.identity.application.OtpService;
import edu.aiplatform.identity.application.PasswordResetService;
import edu.aiplatform.identity.application.SessionInfo;
import edu.aiplatform.identity.domain.OtpPurpose;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC 01 Activate Account và UC 04 Reset Password: ba bước email → OTP → mật khẩu mới (BR-U01-28).
 * Endpoint public, không cần CSRF token ({@code /api/v1/auth/**}). Không log body (có OTP, mật khẩu).
 */
@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class OtpFlowController {

    /** Cùng một câu cho mọi trường hợp (BR-U01-12, MSG01). */
    public static final String NEUTRAL_MESSAGE =
            "Nếu email hợp lệ, mã gồm 6 chữ số sẽ được gửi. Mã có hiệu lực 10 phút.";

    private final OtpService otp;
    private final ActivationService activation;
    private final PasswordResetService passwordReset;
    private final AuthCookies cookies;

    public OtpFlowController(OtpService otp, ActivationService activation, PasswordResetService passwordReset,
                             AuthCookies cookies) {
        this.otp = otp;
        this.activation = activation;
        this.passwordReset = passwordReset;
        this.cookies = cookies;
    }

    public record EmailRequest(@NotNull @Size(max = 320) String email) {
    }

    public record VerifyRequest(@NotNull @Size(max = 320) String email, @NotNull @Size(max = 16) String otp) {
    }

    public record CompleteRequest(@NotNull @Size(max = 128) String otpTicket,
                                  @NotNull @Size(max = 1024) String newPassword) {
    }

    public record TicketResponse(String otpTicket) {
    }

    @PostMapping("/activation/request")
    public ResponseEntity<Map<String, String>> requestActivation(@RequestBody @Validated EmailRequest request,
                                                                 HttpServletRequest http) {
        otp.request(request.email(), OtpPurpose.ACTIVATION, http.getRemoteAddr());
        return accepted();
    }

    @PostMapping("/activation/verify")
    public TicketResponse verifyActivation(@RequestBody @Validated VerifyRequest request) {
        return new TicketResponse(otp.verify(request.email(), OtpPurpose.ACTIVATION, request.otp()));
    }

    /** Kích hoạt xong thì tự đăng nhập (BR-U01-13): trả phiên và đặt cookie như đăng nhập. */
    @PostMapping("/activation/complete")
    public ResponseEntity<SessionInfo> completeActivation(@RequestBody @Validated CompleteRequest request) {
        AuthService.LoginResult result = activation.complete(request.otpTicket(), request.newPassword());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.access(result.accessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookies.refresh(result.refreshToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookies.clearLegacyRefresh().toString())
                .body(result.info());
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Map<String, String>> requestPasswordReset(@RequestBody @Validated EmailRequest request,
                                                                    HttpServletRequest http) {
        otp.request(request.email(), OtpPurpose.PASSWORD_RESET, http.getRemoteAddr());
        return accepted();
    }

    @PostMapping("/password-reset/verify")
    public TicketResponse verifyPasswordReset(@RequestBody @Validated VerifyRequest request) {
        return new TicketResponse(otp.verify(request.email(), OtpPurpose.PASSWORD_RESET, request.otp()));
    }

    /** Đặt lại xong không tự đăng nhập; mọi phiên cũ đã bị xóa (BR-U01-34). */
    @PostMapping("/password-reset/complete")
    public ResponseEntity<Void> completePasswordReset(@RequestBody @Validated CompleteRequest request) {
        passwordReset.complete(request.otpTicket(), request.newPassword());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clearAccess().toString())
                .header(HttpHeaders.SET_COOKIE, cookies.clearRefresh().toString())
                .header(HttpHeaders.SET_COOKIE, cookies.clearLegacyRefresh().toString())
                .build();
    }

    private static ResponseEntity<Map<String, String>> accepted() {
        return ResponseEntity.accepted().body(Map.of("message", NEUTRAL_MESSAGE));
    }
}
