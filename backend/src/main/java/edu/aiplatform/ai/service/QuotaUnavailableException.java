package edu.aiplatform.ai.service;

import edu.aiplatform.shared.error.ApiException;
import org.springframework.http.HttpStatus;

/** Expected quota rejection before reservation writes; callers may try another source. */
final class QuotaUnavailableException extends ApiException {
    QuotaUnavailableException(String code, String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, code, message);
    }
}
