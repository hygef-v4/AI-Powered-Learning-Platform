package edu.aiplatform.shared.contract;

public record PageRequest(int page, int size) {
    public PageRequest {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("page/size");
    }
}
