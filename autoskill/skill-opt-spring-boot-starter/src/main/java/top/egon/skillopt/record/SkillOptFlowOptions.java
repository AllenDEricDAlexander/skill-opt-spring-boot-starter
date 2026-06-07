package top.egon.skillopt.record;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Configures workspace paths and validation thresholds for one optimization flow.
 */
public record SkillOptFlowOptions(Path workspace, Path versionsDirectory, Path bestDirectory,
    boolean autoOverwriteBestSkill, double minValidationImprovement, int maxSkillBytes) {

  public SkillOptFlowOptions(Path workspace, double minValidationImprovement, int maxSkillBytes) {
    this(workspace, workspace.resolve("versions"), workspace.resolve("best"), false,
        minValidationImprovement, maxSkillBytes);
  }

  public SkillOptFlowOptions {
    Objects.requireNonNull(workspace, "workspace must not be null");
    Objects.requireNonNull(versionsDirectory, "versionsDirectory must not be null");
    Objects.requireNonNull(bestDirectory, "bestDirectory must not be null");
    if (minValidationImprovement < 0.0) {
      throw new IllegalArgumentException("minValidationImprovement must not be negative");
    }
    if (maxSkillBytes <= 0) {
      throw new IllegalArgumentException("maxSkillBytes must be positive");
    }
  }
}
