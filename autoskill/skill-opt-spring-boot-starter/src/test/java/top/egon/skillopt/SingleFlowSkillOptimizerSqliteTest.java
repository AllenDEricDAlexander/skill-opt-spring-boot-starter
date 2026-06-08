package top.egon.skillopt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.skillopt.autoconfigure.SkillOptSqliteAutoConfiguration;
import top.egon.skillopt.autoconfigure.SkillOptSqliteProperties;
import top.egon.skillopt.core.SingleFlowSkillOptimizer;
import top.egon.skillopt.core.SkillEditPlanner;
import top.egon.skillopt.core.SkillReflector;
import top.egon.skillopt.core.SkillRolloutRunner;
import top.egon.skillopt.enums.SkillOptCaseSplit;
import top.egon.skillopt.record.SkillEditOperation;
import top.egon.skillopt.record.SkillOptCase;
import top.egon.skillopt.record.SkillOptFlowOptions;
import top.egon.skillopt.record.SkillOptRolloutEvidence;
import top.egon.skillopt.record.SkillOptToolCall;
import top.egon.skillopt.record.SkillOptimizationResult;
import top.egon.skillopt.record.SkillReflection;
import top.egon.skillopt.repository.SkillOptEditOperationRepository;
import top.egon.skillopt.repository.SkillOptReflectionRepository;
import top.egon.skillopt.repository.SkillOptRolloutRepository;
import top.egon.skillopt.repository.SkillOptRoundRepository;
import top.egon.skillopt.repository.SkillOptToolCallRepository;
import top.egon.skillopt.storage.SkillOptTraceStore;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SingleFlowSkillOptimizerSqliteTest {

  @TempDir
  Path tempDir;

  @Test
  void optimizerPersistsOptimizationEvidenceToSqlite() throws Exception {
    Path database = tempDir.resolve("skillopt.db");
    ApplicationContextRunner contextRunner =
        new ApplicationContextRunner().withUserConfiguration(SkillOptSqliteAutoConfiguration.class)
            .withPropertyValues("skill-opt.sqlite.database=" + database);

    contextRunner.run(context -> {
      Path skillFile = tempDir.resolve("skills/poem-intent/SKILL.md");
      Files.createDirectories(skillFile.getParent());
      Files.writeString(skillFile, """
          ---
          name: poem-intent
          description: 分析用户意图并写诗
          ---

          # 写诗助手

          ## 写诗流程
          - 直接根据用户输入写一首短诗。
          """, StandardCharsets.UTF_8);

      SkillRolloutRunner runner = (skillCase, currentSkillFile) -> {
        String skillContent = Files.readString(currentSkillFile, StandardCharsets.UTF_8);
        double score = skillContent.contains("先判断用户意图") ? 0.92 : 0.48;
        return SkillOptRolloutEvidence.success(skillCase.id(), skillCase.input(), currentSkillFile,
            "output for " + skillCase.id(), score, List.of(new SkillOptToolCall("read_skill",
                "{\"skill_name\":\"poem-intent\"}", "skill content", "")));
      };
      SkillReflector reflector =
          rollouts -> new SkillReflection("缺少稳定的用户意图分析步骤", List.of("补充用户意图分析步骤"));
      SkillEditPlanner editPlanner = (skillContent, reflection) -> List
          .of(SkillEditOperation.add("## 写诗流程\n", "- 先判断用户意图，再选择意象、体裁和语气。\n"));
      SkillOptTraceStore traceStore = context.getBean(SkillOptTraceStore.class);
      SingleFlowSkillOptimizer optimizer = new SingleFlowSkillOptimizer(
          new SkillOptFlowOptions(tempDir.resolve(".skillopt"), 0.01, 64000), runner, reflector,
          editPlanner, traceStore);

      SkillOptimizationResult result = optimizer.optimize("poem-intent", skillFile,
          List.of(new SkillOptCase("reflect-001", "我想写一首表达离别的诗", "需要识别离别意图",
              SkillOptCaseSplit.REFLECTION)),
          List.of(new SkillOptCase("valid-001", "帮我写一首春天里重逢的诗", "需要识别重逢意图",
              SkillOptCaseSplit.VALIDATION)));

      assertThat(result.accepted()).isTrue();
      assertThat(context.getBean(SkillOptRoundRepository.class).findAll()).hasSize(1);
      assertThat(context.getBean(SkillOptRolloutRepository.class).findAll()).hasSize(3);
      assertThat(context.getBean(SkillOptToolCallRepository.class).findAll()).hasSize(3);
      assertThat(context.getBean(SkillOptReflectionRepository.class).findAll()).hasSize(1);
      assertThat(context.getBean(SkillOptEditOperationRepository.class).findAll()).hasSize(1);
    });
  }

  @Test
  void optimizerUsesConfiguredOutputPathsAndOverwritesLiveSkillWhenEnabled() throws Exception {
    Path database = tempDir.resolve("configured-skillopt.db");
    Path versionsDirectory = tempDir.resolve("configured-versions");
    Path bestDirectory = tempDir.resolve("configured-best");
    ApplicationContextRunner contextRunner =
        new ApplicationContextRunner().withUserConfiguration(SkillOptSqliteAutoConfiguration.class)
            .withPropertyValues("skill-opt.sqlite.database=" + database,
                "skill-opt.sqlite.versions-directory=" + versionsDirectory,
                "skill-opt.sqlite.best-directory=" + bestDirectory,
                "skill-opt.sqlite.auto-overwrite-best-skill=true");

    contextRunner.run(context -> {
      Path skillFile = tempDir.resolve("live-skills/poem-intent/SKILL.md");
      Files.createDirectories(skillFile.getParent());
      String originalSkillContent = """
          ---
          name: poem-intent
          description: 分析用户意图并写诗
          ---

          # 写诗助手

          ## 写诗流程
          - 直接根据用户输入写一首短诗。
          """;
      Files.writeString(skillFile, originalSkillContent, StandardCharsets.UTF_8);

      SkillRolloutRunner runner = (skillCase, currentSkillFile) -> {
        String skillContent = Files.readString(currentSkillFile, StandardCharsets.UTF_8);
        double score = skillContent.contains("先判断用户意图") ? 0.92 : 0.48;
        return SkillOptRolloutEvidence.success(skillCase.id(), skillCase.input(), currentSkillFile,
            "output for " + skillCase.id(), score, List.of());
      };
      SkillReflector reflector =
          rollouts -> new SkillReflection("缺少稳定的用户意图分析步骤", List.of("补充用户意图分析步骤"));
      SkillEditPlanner editPlanner = (skillContent, reflection) -> List
          .of(SkillEditOperation.add("## 写诗流程\n", "- 先判断用户意图，再选择意象、体裁和语气。\n"));
      SkillOptSqliteProperties properties = context.getBean(SkillOptSqliteProperties.class);
      SingleFlowSkillOptimizer optimizer =
          new SingleFlowSkillOptimizer(properties.toFlowOptions(0.01, 64000), runner, reflector,
              editPlanner, context.getBean(SkillOptTraceStore.class));

      SkillOptimizationResult result = optimizer.optimize("poem-intent", skillFile,
          List.of(new SkillOptCase("reflect-001", "我想写一首表达离别的诗", "需要识别离别意图",
              SkillOptCaseSplit.REFLECTION)),
          List.of(new SkillOptCase("valid-001", "帮我写一首春天里重逢的诗", "需要识别重逢意图",
              SkillOptCaseSplit.VALIDATION)));

      assertThat(result.candidateSkillFile()).startsWith(versionsDirectory);
      assertThat(result.bestSkillFile())
          .isEqualTo(bestDirectory.resolve("poem-intent").resolve("best_skill.md"));
      Path baseSkillBackupFile =
          result.candidateSkillFile().getParent().getParent().resolve("base_skill.md");
      assertThat(baseSkillBackupFile).exists();
      assertThat(Files.readString(baseSkillBackupFile, StandardCharsets.UTF_8))
          .isEqualTo(originalSkillContent);
      assertThat(context.getBean(SkillOptRoundRepository.class).findAll().getFirst()
          .getBaseSkillBackupFile()).isEqualTo(baseSkillBackupFile.toString());
      assertThat(Files.readString(result.bestSkillFile(), StandardCharsets.UTF_8))
          .contains("先判断用户意图");
      assertThat(Files.readString(skillFile, StandardCharsets.UTF_8)).contains("先判断用户意图");
    });
  }
}
