package edu.aiplatform.ai.contract;

/** Một nguồn AI dùng được cho một lượt gọi: API chung ({@code provider} rỗng) hoặc API cá nhân của một provider. */
public record AiSourceChoice(AiSource source, AiProvider provider) {
}
