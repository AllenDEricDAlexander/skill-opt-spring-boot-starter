package top.egon.skillopt;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.shelltool.ShellToolAgentHook;
import com.alibaba.cloud.ai.graph.agent.tools.ShellTool2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.skillopt.record.SkillPackage;
import top.egon.skillopt.sandbox.SkillPackageShellOptions;
import top.egon.skillopt.sandbox.SkillPackageShellToolFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillPackageShellToolFactoryTest {

  @TempDir
  Path tempDir;

  @Test
  void createsShellToolConstrainedToSkillPackageRoot() throws Exception {
    Path skillRoot = tempDir.resolve("complex-skill");
    Files.createDirectories(skillRoot.resolve("scripts"));
    Path skillFile = skillRoot.resolve("SKILL.md");
    Files.writeString(skillFile, "# Complex Skill\n", StandardCharsets.UTF_8);
    SkillPackage skillPackage =
        new SkillPackage("complex-skill", "1.0.0", "nacos", skillRoot, skillFile);
    SkillPackageShellToolFactory factory = new SkillPackageShellToolFactory(
        new SkillPackageShellOptions(5000L, 20, List.of("/bin/sh"), Map.of("SKILL_OPT", "true")));
    ShellTool2 shellTool = factory.createShellTool(skillPackage);
    RunnableConfig config = RunnableConfig.builder().threadId("shell-test").build();

    shellTool.getSessionManager().initialize(config);
    try {
      String output = shellTool.getSessionManager().executeCommand("pwd", config).getOutput();

      assertThat(Path.of(output.trim()).toRealPath()).isEqualTo(skillRoot.toRealPath());
      assertThat(shellTool.getSessionManager().getMaxOutputLines()).isEqualTo(20);
    } finally {
      shellTool.getSessionManager().cleanup(config);
    }
  }

  @Test
  void createsOfficialShellToolAgentHook() {
    Path skillRoot = tempDir.resolve("complex-skill");
    SkillPackage skillPackage = new SkillPackage("complex-skill", "1.0.0", "nacos", skillRoot,
        skillRoot.resolve("SKILL.md"));
    SkillPackageShellToolFactory factory =
        new SkillPackageShellToolFactory(SkillPackageShellOptions.defaults());

    ShellToolAgentHook hook = factory.createHook(skillPackage);

    assertThat(hook.getName()).isEqualTo("ShellToolAgentHook");
    assertThat(hook.getTools()).singleElement()
        .satisfies(tool -> assertThat(tool.getToolDefinition().name()).isEqualTo("shell"));
  }
}
