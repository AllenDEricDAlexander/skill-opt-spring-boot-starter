package top.egon.skillopt.record;

import java.nio.file.Path;
import java.util.List;

/**
 * Summarizes one skill optimization round and its validation decision.
 */
public record SkillOptimizationResult(String skillName, boolean accepted, double baselineScore,
    double candidateScore, Path candidateSkillFile, Path bestSkillFile, SkillReflection reflection,
    List<SkillOptRolloutEvidence> rollouts) {
}
