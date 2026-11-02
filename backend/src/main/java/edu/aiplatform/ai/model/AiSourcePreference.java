package edu.aiplatform.ai.model;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;

/** Nguồn AI mặc định của một tài khoản (FB-U06-01); {@code provider} chỉ có với API cá nhân. */
public record AiSourcePreference(AiSource source, AiProvider provider) {
}
