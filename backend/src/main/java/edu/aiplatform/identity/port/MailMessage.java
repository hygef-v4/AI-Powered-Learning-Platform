package edu.aiplatform.identity.port;

public record MailMessage(String to, String subject, String htmlBody, String textBody) {}
