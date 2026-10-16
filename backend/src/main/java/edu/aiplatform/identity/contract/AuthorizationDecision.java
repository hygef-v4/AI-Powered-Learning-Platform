package edu.aiplatform.identity.contract;

public record AuthorizationDecision(boolean allowed, String reason) {

    public static AuthorizationDecision allow() {
        return new AuthorizationDecision(true, null);
    }

    public static AuthorizationDecision deny(String reason) {
        return new AuthorizationDecision(false, reason);
    }
}
