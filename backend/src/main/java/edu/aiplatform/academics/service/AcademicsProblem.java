package edu.aiplatform.academics.service;

import edu.aiplatform.shared.error.ApiException;
import java.util.Map;
import org.springframework.http.HttpStatus;

public class AcademicsProblem extends ApiException {
    private final Map<String,Object> properties;
    public AcademicsProblem(String code, String message, Map<String,Object> properties) {
        super(HttpStatus.CONFLICT,code,message);
        this.properties = Map.copyOf(properties);
    }
    public Map<String,Object> properties() { return properties; }
}
