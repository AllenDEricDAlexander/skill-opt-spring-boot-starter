package top.egon.skillopt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.skillopt.core.SingleFlowSkillOptimizer;
import top.egon.skillopt.core.SkillEditPlanner;
import top.egon.skillopt.core.SkillReflector;
import top.egon.skillopt.core.SkillRolloutRunner;
import top.egon.skillopt.enums.SkillOptCaseSplit;
import top.egon.skillopt.record.SkillEditOperation;
import top.egon.skillopt.record.SkillOptCase;
import top.egon.skillopt.record.SkillOptFlowOptions;
import top.egon.skillopt.record.SkillOptRolloutEvidence;
import top.egon.skillopt.record.SkillOptimizationResult;
import top.egon.skillopt.record.SkillReflection;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SingleFlowSkillOptimizerTest {

  @TempDir
  Path tempDir;

  @Test
  void acceptsCandidateAndExportsBestSkillWhenValidationImproves() throws Exception {
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
          "output for " + skillCase.id(), score, List.of());
    };
    SkillReflector reflector =
        rollouts -> new SkillReflection("缺少稳定的用户意图分析步骤", List.of("在写诗流程中加入意图识别、体裁选择和语气控制。"));
    SkillEditPlanner editPlanner = (skillContent, reflection) -> List
        .of(SkillEditOperation.add("## 写诗流程\n", "- 先判断用户意图，再选择意象、体裁和语气。\n"));

    SingleFlowSkillOptimizer optimizer = new SingleFlowSkillOptimizer(
        new SkillOptFlowOptions(tempDir.resolve(".skillopt"), 0.01, 64000), runner, reflector,
        editPlanner);

    SkillOptimizationResult result = optimizer.optimize("poem-intent", skillFile,
        List.of(new SkillOptCase("reflect-001", "我想写一首表达离别的诗", "需要识别离别意图",
            SkillOptCaseSplit.REFLECTION)),
        List.of(new SkillOptCase("valid-001", "帮我写一首春天里重逢的诗", "需要识别重逢意图",
            SkillOptCaseSplit.VALIDATION)));

    assertThat(result.accepted()).isTrue();
    assertThat(result.candidateScore()).isGreaterThan(result.baselineScore());
    assertThat(result.bestSkillFile()).exists();
    assertThat(Files.readString(result.bestSkillFile(), StandardCharsets.UTF_8))
        .contains("先判断用户意图");
    assertThat(result.rollouts()).extracting(SkillOptRolloutEvidence::caseId)
        .containsExactly("reflect-001", "valid-001", "valid-001");
  }

  @Test
  void preservesSkillPackageResourcesInCandidateAndBestDirectories() throws Exception {
    Path skillFile = tempDir.resolve("skills/complex-skill/SKILL.md");
    Files.createDirectories(skillFile.getParent().resolve("scripts"));
    Files.writeString(skillFile, """
        ---
        name: complex-skill
        description: 使用脚本校验输入
        ---

        # 复杂 Skill
        """, StandardCharsets.UTF_8);
    Files.writeString(skillFile.getParent().resolve("scripts/check.sh"), "echo ok\n",
        StandardCharsets.UTF_8);

    SkillRolloutRunner runner = (skillCase, currentSkillFile) -> {
      String skillContent = Files.readString(currentSkillFile, StandardCharsets.UTF_8);
      double score = skillContent.contains("scripts/check.sh") ? 0.91 : 0.42;
      return SkillOptRolloutEvidence.success(skillCase.id(), skillCase.input(), currentSkillFile,
          "output for " + skillCase.id(), score, List.of());
    };
    SkillReflector reflector =
        rollouts -> new SkillReflection("缺少脚本校验约束", List.of("说明 scripts/check.sh 的使用时机。"));
    SkillEditPlanner editPlanner = (skillContent, reflection) -> List
        .of(SkillEditOperation.add("", "\n## 脚本校验\n- 使用 scripts/check.sh 校验输入。\n"));
    SingleFlowSkillOptimizer optimizer = new SingleFlowSkillOptimizer(
        new SkillOptFlowOptions(tempDir.resolve(".skillopt"), 0.01, 64000), runner, reflector,
        editPlanner);

    SkillOptimizationResult result = optimizer.optimize("complex-skill", skillFile,
        List.of(new SkillOptCase("reflect-001", "校验这个输入", "需要调用脚本", SkillOptCaseSplit.REFLECTION)),
        List.of(new SkillOptCase("valid-001", "校验另一个输入", "需要调用脚本", SkillOptCaseSplit.VALIDATION)));

    assertThat(result.accepted()).isTrue();
    assertThat(result.candidateSkillFile().getParent().resolve("scripts/check.sh")).exists();
    assertThat(result.bestSkillFile().getParent().resolve("scripts/check.sh")).exists();
  }
}
