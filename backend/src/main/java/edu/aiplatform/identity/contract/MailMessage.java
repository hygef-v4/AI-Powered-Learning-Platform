package edu.aiplatform.identity.contract;

public record MailMessage(String to, String subject, String textBody, String htmlBody) {
}
