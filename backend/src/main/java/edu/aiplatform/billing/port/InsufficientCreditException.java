package edu.aiplatform.billing.port;

public class InsufficientCreditException extends RuntimeException {
    public InsufficientCreditException(String message) { super(message); }
}
