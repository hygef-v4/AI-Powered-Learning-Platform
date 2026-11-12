package edu.aiplatform.assignments.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AssignmentRequestsTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void applyBodyCarriesTheImportPlacement() throws Exception {
        // Thử trên trình duyệt 2026-11-11: thiếu trường này thì "Chèn vào cuối" thành "Thay toàn bộ".
        var append = json.readValue("{\"version\":3,\"replaceTitle\":true,\"placement\":\"APPEND\"}",
                AssignmentRequests.Apply.class).command();
        assertThat(append.placement()).isEqualTo("APPEND");
        assertThat(append.version()).isEqualTo(3);
        assertThat(append.replaceTitle()).isTrue();

        var plain = json.readValue("{\"version\":3}", AssignmentRequests.Apply.class).command();
        assertThat(plain.placement()).isNull();
        assertThat(plain.replaceInstructions()).isFalse();
    }
}
