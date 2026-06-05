package top.egon.skillopt;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Configures workspace paths and validation thresholds for one optimization flow.
 */
public record SkillOptFlowOptions(Path workspace, double minValidationImprovement, int maxSkillBytes) {

    public SkillOptFlowOptions {
        Objects.requireNonNull(workspace, "workspace must not be null");
        if (minValidationImprovement < 0.0) {
            throw new IllegalArgumentException("minValidationImprovement must not be negative");
        }
        if (maxSkillBytes <= 0) {
            throw new IllegalArgumentException("maxSkillBytes must be positive");
        }
    }
}
