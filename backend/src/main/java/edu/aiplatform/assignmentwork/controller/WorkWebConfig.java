package edu.aiplatform.assignmentwork.controller;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Giới hạn số request lưu nháp chạy cùng lúc (TC-PERF-004, KAN-244); chỉ đường dẫn lưu nội dung lượt làm. */
@Configuration
class WorkWebConfig implements WebMvcConfigurer {

    private final SaveConcurrencyLimiter saveLimiter;

    WorkWebConfig(SaveConcurrencyLimiter saveLimiter) {
        this.saveLimiter = saveLimiter;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(saveLimiter).addPathPatterns("/api/v1/attempts/*/content");
    }
}
