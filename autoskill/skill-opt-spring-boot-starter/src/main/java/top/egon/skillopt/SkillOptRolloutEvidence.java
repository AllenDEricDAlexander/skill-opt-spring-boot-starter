package top.egon.skillopt;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Records the target model output, score, tool calls, error, and timing for one rollout.
 */
public record SkillOptRolloutEvidence(String caseId, String input, Path skillFile, String skillHash, String output,
        double score, boolean passed, List<SkillOptToolCall> toolCalls, String error, long durationMillis) {

    public SkillOptRolloutEvidence {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("case id must not be blank");
        }
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(skillFile, "skillFile must not be null");
        skillHash = skillHash == null ? "" : skillHash;
        output = output == null ? "" : output;
        if (score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("score must be between 0 and 1");
        }
        toolCalls = List.copyOf(Objects.requireNonNull(toolCalls, "toolCalls must not be null"));
        error = error == null ? "" : error;
        if (durationMillis < 0L) {
            throw new IllegalArgumentException("durationMillis must not be negative");
        }
    }

    /**
     * Creates successful rollout evidence for a completed model task.
     */
    public static SkillOptRolloutEvidence success(String caseId, String input, Path skillFile, String output,
            double score, List<SkillOptToolCall> toolCalls) {
        return new SkillOptRolloutEvidence(caseId, input, skillFile, SkillOptHash.sha256(skillFile), output, score,
                score > 0.0, toolCalls, "", 0L);
    }

    /**
     * Creates failed rollout evidence while keeping the flow traceable.
     */
    public static SkillOptRolloutEvidence failure(String caseId, String input, Path skillFile, Exception ex,
            long durationMillis) {
        String error = ex == null ? "" : ex.getClass().getSimpleName() + ": " + ex.getMessage();
        return new SkillOptRolloutEvidence(caseId, input, skillFile, SkillOptHash.sha256(skillFile), "", 0.0, false,
                List.of(), error, durationMillis);
    }

    /**
     * Returns the same evidence with measured duration filled by the orchestrator.
     */
    public SkillOptRolloutEvidence withDurationMillis(long durationMillis) {
        return new SkillOptRolloutEvidence(caseId, input, skillFile, skillHash, output, score, passed, toolCalls, error,
                durationMillis);
    }
}
