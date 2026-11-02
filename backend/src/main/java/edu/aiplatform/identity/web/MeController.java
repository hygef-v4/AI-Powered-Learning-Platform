package edu.aiplatform.identity.web;

import edu.aiplatform.identity.application.PasswordChangeService;
import edu.aiplatform.identity.application.ProfileService;
import edu.aiplatform.identity.contract.Actor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC 05 Change Password, UC 06 View / UC 07 Update Profile Information. Luôn là tài khoản của phiên, không nhận ID
 * (BR-U01-54). Request đổi trạng thái cần header CSRF.
 */
@RestController
@RequestMapping("/api/v1/me")
@Validated
public class MeController {

    private final ProfileService profiles;
    private final PasswordChangeService passwords;

    public MeController(ProfileService profiles, PasswordChangeService passwords) {
        this.profiles = profiles;
        this.passwords = passwords;
    }

    /** Chỉ nhận tên và số điện thoại; field khác bị từ chối 400 trước khi ghi (BR-U01-52). */
    public static class UpdateProfileRequest extends StrictRequest {
        @NotNull
        @Size(max = 300)
        public String displayName;
        @Size(max = 32)
        public String phoneNumber;
    }

    public static class ChangePasswordRequest extends StrictRequest {
        @NotNull
        @Size(max = 1024)
        public String currentPassword;
        @NotNull
        @Size(max = 1024)
        public String newPassword;
    }

    @GetMapping("/profile")
    public ProfileService.ProfileView profile(@AuthenticationPrincipal Actor actor) {
        return profiles.get(actor);
    }

    @PatchMapping("/profile")
    public ProfileService.ProfileView updateProfile(@AuthenticationPrincipal Actor actor,
                                                    @RequestBody @Validated UpdateProfileRequest request) {
        request.rejectUnknownFields();
        return profiles.update(actor, request.displayName, request.phoneNumber);
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Actor actor,
                                               @RequestBody @Validated ChangePasswordRequest request,
                                               HttpServletRequest http) {
        request.rejectUnknownFields();
        String sid = (String) http.getAttribute(JwtAuthFilter.SID_ATTRIBUTE);
        passwords.change(actor, sid, request.currentPassword, request.newPassword);
        return ResponseEntity.noContent().build();
    }
}
