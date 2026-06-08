package top.egon.skillopt.sandbox;

import com.alibaba.cloud.ai.graph.agent.hook.shelltool.ShellToolAgentHook;
import com.alibaba.cloud.ai.graph.agent.tools.ShellTool2;
import top.egon.skillopt.record.SkillPackage;

import java.util.Objects;

/**
 * Creates Spring AI Alibaba shell tools constrained to a skill package root directory.
 */
public class SkillPackageShellToolFactory {

  private final SkillPackageShellOptions options;

  public SkillPackageShellToolFactory(SkillPackageShellOptions options) {
    this.options = Objects.requireNonNull(options, "options must not be null");
  }

  /**
   * Creates the official ShellTool2 with workspace rooted at the skill package directory.
   */
  public ShellTool2 createShellTool(SkillPackage skillPackage) {
    Objects.requireNonNull(skillPackage, "skillPackage must not be null");
    ShellTool2.Builder builder =
        ShellTool2.builder(skillPackage.rootDirectory().toAbsolutePath().toString())
            .withCommandTimeout(options.commandTimeoutMillis())
            .withMaxOutputLines(options.maxOutputLines());
    if (!options.shellCommand().isEmpty()) {
      builder.withShellCommand(options.shellCommand());
    }
    if (!options.environment().isEmpty()) {
      builder.withEnvironment(options.environment());
    }
    return builder.build();
  }

  /**
   * Creates the official ShellToolAgentHook for ReactAgent hook wiring.
   */
  public ShellToolAgentHook createHook(SkillPackage skillPackage) {
    return ShellToolAgentHook.builder().shellTool2(createShellTool(skillPackage))
        .shellToolName("shell").build();
  }
}
