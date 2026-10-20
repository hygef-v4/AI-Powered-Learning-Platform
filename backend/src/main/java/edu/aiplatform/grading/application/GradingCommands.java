package edu.aiplatform.grading.application;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.shared.error.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GradingCommands {
    private GradingCommands() {
    }

    public interface IStrictInput {
        @JsonAnySetter
        default void unknown(String key, Object value) {
            throw ApiException.badRequest("INVALID_REQUEST", "Unknown field: " + key);
        }
    }

    public record Save(@JsonProperty(required = true) @JsonSetter(nulls = Nulls.FAIL) @PositiveOrZero long version,
            @NotNull Map<UUID, Set<UUID>> checkedItemsByRubric, BigDecimal adjustment,
            @Size(max = 2000) String adjustmentReason, @Size(max = 10000) String feedback,
            @Size(max = 2000) String changeReason, UUID usedProposalId) implements IStrictInput {
    }

    public record ItemVersion(@NotNull UUID evaluationId,
            @JsonProperty(required = true) @JsonSetter(nulls = Nulls.FAIL) @PositiveOrZero long version)
            implements IStrictInput {
    }

    public record Batch(@NotEmpty @Size(max = 200) List<@Valid ItemVersion> items) implements IStrictInput {
    }

    public record AiRequest(@NotNull AiSource source, AiProvider provider, @NotBlank @Size(max = 200) String requestRef)
            implements IStrictInput {
    }

    public record AiBatch(@NotEmpty @Size(max = 200) List<@Valid ItemVersion> items, @NotNull @Valid AiRequest request)
            implements IStrictInput {
    }

    public record MemberSave(
            @JsonProperty(required = true) @JsonSetter(nulls = Nulls.FAIL) @PositiveOrZero long version,
            @NotNull @DecimalMin("0") BigDecimal score, @Size(max = 2000) String reason) implements IStrictInput {
    }
}
