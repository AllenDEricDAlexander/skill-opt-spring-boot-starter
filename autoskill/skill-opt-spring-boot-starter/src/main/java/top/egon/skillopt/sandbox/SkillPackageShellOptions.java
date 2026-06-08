package top.egon.skillopt.sandbox;

import java.util.List;
import java.util.Map;

/**
 * Shell tool limits used when running scripts from a local skill package.
 */
public record SkillPackageShellOptions(long commandTimeoutMillis, int maxOutputLines,
    List<String> shellCommand, Map<String, String> environment) {

  public SkillPackageShellOptions {
    if (commandTimeoutMillis <= 0L) {
      throw new IllegalArgumentException("command timeout must be positive");
    }
    if (maxOutputLines <= 0) {
      throw new IllegalArgumentException("max output lines must be positive");
    }
    shellCommand = shellCommand == null ? List.of() : List.copyOf(shellCommand);
    environment = environment == null ? Map.of() : Map.copyOf(environment);
  }

  /**
   * Creates conservative defaults for local skill package script execution.
   */
  public static SkillPackageShellOptions defaults() {
    return new SkillPackageShellOptions(30000L, 1000, List.of(), Map.of());
  }
}
