package edu.aiplatform.shared.security;

import edu.aiplatform.shared.contract.ActorRef;
import edu.aiplatform.shared.web.ApiException;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Người đang gọi API: principal là {@link ActorRef} do filter JWT của U01 đặt vào SecurityContext. */
public final class CurrentActor {

    private CurrentActor() {}

    public static Optional<ActorRef> get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof ActorRef actor) {
            return Optional.of(actor);
        }
        return Optional.empty();
    }

    public static ActorRef require() {
        return get().orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Vui lòng đăng nhập."));
    }
}
