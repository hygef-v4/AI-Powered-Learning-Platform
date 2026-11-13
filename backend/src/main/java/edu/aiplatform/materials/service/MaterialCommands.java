package edu.aiplatform.materials.service;

import edu.aiplatform.materials.model.Material;
import edu.aiplatform.shared.content.FormattedText;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public final class MaterialCommands {
    private MaterialCommands() {}

    public record Source(String fileRef, @Size(max = 500) String videoUrl) {}

    public record Create(
            @NotNull Material.Kind kind,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @Min(0) int orderNo,
            @NotNull @Valid Source source,
            @NotNull UUID moduleId,
            Boolean enabled,
            FormattedText descriptionDocument) {
        public Create(Material.Kind kind, String title, String description, int orderNo, Source source, UUID moduleId) {
            this(kind, title, description, orderNo, source, moduleId, null, null);
        }
    }

    /** {@code moduleId} khác module hiện tại thì chuyển học liệu sang module đó; bỏ trống là giữ nguyên. */
    public record Update(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 2000) String description,
            @Min(0) int orderNo,
            @NotNull @Min(0) Long version,
            UUID moduleId,
            Boolean enabled,
            FormattedText descriptionDocument) {
        public Update(String title, String description, int orderNo, Long version, UUID moduleId) {
            this(title, description, orderNo, version, moduleId, null, null);
        }
    }

    public record ModuleInput(@NotBlank @Size(max = 200) String title) {}

    public record ModuleUpdate(
            @NotBlank @Size(max = 200) String title, @NotNull @Min(0) Long version) {}

    public record QuizAttach(@NotNull UUID quizId) {}

    public record Replace(@NotNull @Min(0) Long version, @NotNull @Valid Source source) {}

    public record Expected(@NotNull @Min(0) Long version) {}

    public record Position(@NotNull UUID id, @NotNull @Min(0) Long version) {}

    public record Reorder(@NotEmpty @Size(max = 500) List<@NotNull @Valid Position> items) {}
}
