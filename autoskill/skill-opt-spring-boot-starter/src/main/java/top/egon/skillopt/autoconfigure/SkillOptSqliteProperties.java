package top.egon.skillopt.autoconfigure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import top.egon.skillopt.record.SkillOptFlowOptions;

import java.nio.file.Path;

/**
 * SQLite storage properties for SkillOpt trace persistence.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "skill-opt.sqlite")
public class SkillOptSqliteProperties {

  private Path database = Path.of(".skillopt", "skillopt.db");

  private Path workspace = Path.of(".skillopt");

  private Path versionsDirectory = Path.of(".skillopt", "versions");

  private Path bestDirectory = Path.of(".skillopt", "best");

  private boolean autoOverwriteBestSkill;

  /**
   * Creates flow options from configured storage/output paths.
   */
  public SkillOptFlowOptions toFlowOptions(double minValidationImprovement, int maxSkillBytes) {
    return new SkillOptFlowOptions(workspace, versionsDirectory, bestDirectory,
        autoOverwriteBestSkill, minValidationImprovement, maxSkillBytes);
  }
}
