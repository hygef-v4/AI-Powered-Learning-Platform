package edu.aiplatform.materials.application;

import edu.aiplatform.materials.domain.Material;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public final class MaterialCommands {
    private MaterialCommands() { }
    public record Source(String fileRef, @Size(max=500) String videoUrl) { }
    public record Create(@NotNull Material.Kind kind,@NotBlank @Size(max=200) String title,
                         @Size(max=2000) String description,@Min(0) int orderNo,@NotNull @Valid Source source,
                         @NotNull UUID moduleId) { }
    /** {@code moduleId} khác module hiện tại thì chuyển học liệu sang module đó; bỏ trống là giữ nguyên. */
    public record Update(@NotBlank @Size(max=200) String title,@Size(max=2000) String description,
                         @Min(0) int orderNo,@NotNull @Min(0) Long version,UUID moduleId) { }
    public record ModuleInput(@NotBlank @Size(max=200) String title) { }
    public record ModuleUpdate(@NotBlank @Size(max=200) String title,@NotNull @Min(0) Long version) { }
    public record QuizAttach(@NotNull UUID quizId) { }
    public record Replace(@NotNull @Min(0) Long version,@NotNull @Valid Source source) { }
    public record Expected(@NotNull @Min(0) Long version) { }
    public record Position(@NotNull UUID id,@NotNull @Min(0) Long version) { }
    public record Reorder(@NotEmpty @Size(max=500) List<@NotNull @Valid Position> items) { }
}
