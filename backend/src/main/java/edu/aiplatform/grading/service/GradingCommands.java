package edu.aiplatform.grading.service;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.grading.model.GradingFeedback;
import edu.aiplatform.grading.model.ContributionLevel;
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
    /** Prevents instantiation of this command and input type container. */
    private GradingCommands() {
    }

    /** Rejects grading batches that are empty or exceed the supported item limit. */
    static void validateBatchItems(List<ItemVersion> items) {
        if (items == null || items.isEmpty() || items.size() > 200)
            throw ApiException.badRequest("INVALID_BATCH", "Batch must contain 1–200 items");
    }

    public interface IStrictInput {
        /** Rejects JSON properties that are not declared by a grading input type. */
        @JsonAnySetter
        default void unknown(String key, Object value) {
            throw ApiException.badRequest("INVALID_REQUEST", "Unknown field: " + key);
        }
    }

    public record Save(
            @JsonProperty(required = true) @JsonSetter(nulls = Nulls.FAIL) @PositiveOrZero long version,
            @NotNull Map<UUID, Set<UUID>> checkedItemsByRubric, BigDecimal adjustment,
            @Size(max = 2000) String adjustmentReason, @Size(max = 10000) String feedback,
            @Size(max = 2000) String changeReason, UUID usedProposalId,
            Map<UUID, Map<UUID, BigDecimal>> itemScoresByRubric, GradingFeedback structuredFeedback,
            Boolean clearStructuredFeedback, Map<UUID, Set<UUID>> acknowledgedReviewItems,
            Map<UUID, Map<UUID, BigDecimal>> reviewedItemScores) implements IStrictInput {
        /** Existing callers retain all-or-nothing checklist scoring. */
        public Save(long version, Map<UUID, Set<UUID>> checkedItemsByRubric, BigDecimal adjustment,
                String adjustmentReason, String feedback, String changeReason,
                UUID usedProposalId) {
            this(version, checkedItemsByRubric, adjustment, adjustmentReason, feedback,
                    changeReason, usedProposalId, null, null, null, null, null);
        }

        /** Existing fractional-score callers retain compatibility without structured feedback. */
        public Save(long version, Map<UUID, Set<UUID>> checkedItemsByRubric, BigDecimal adjustment,
                String adjustmentReason, String feedback, String changeReason, UUID usedProposalId,
                Map<UUID, Map<UUID, BigDecimal>> itemScoresByRubric) {
            this(version, checkedItemsByRubric, adjustment, adjustmentReason, feedback,
                    changeReason, usedProposalId, itemScoresByRubric, null, null, null, null);
        }
    }

    public record ItemVersion(
            @JsonProperty("evaluationId") @NotNull UUID evaluationId,
            @JsonProperty(value = "version", required = true) @JsonSetter(nulls = Nulls.FAIL) @PositiveOrZero long version)
            implements IStrictInput {
    }

    public record Batch(@JsonProperty("items") @NotEmpty @Size(max = 200) List<@Valid ItemVersion> items)
            implements IStrictInput {
    }

    public record AiRequest(
            @JsonProperty("source") AiSource source,
            @JsonProperty("provider") AiProvider provider,
            @JsonProperty("requestRef") @NotBlank @Size(max = 200) String requestRef) implements IStrictInput {
    }

    public record AiBatch(
            @JsonProperty("items") @NotEmpty @Size(max = 200) List<@Valid ItemVersion> items,
            @JsonProperty("request") @NotNull @Valid AiRequest request) implements IStrictInput {
    }

    public record MemberSave(
            @JsonProperty(required = true) @JsonSetter(nulls = Nulls.FAIL) @PositiveOrZero long version,
            @NotNull @DecimalMin("0") BigDecimal score, @Size(max = 2000) String reason,
            ContributionLevel contributionLevel)
            implements IStrictInput {
        public MemberSave(long version, BigDecimal score, String reason) {
            this(version, score, reason, null);
        }
    }
}
