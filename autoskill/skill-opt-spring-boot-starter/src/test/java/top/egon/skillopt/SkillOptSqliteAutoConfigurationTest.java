package top.egon.skillopt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.skillopt.autoconfigure.SkillOptSqliteAutoConfiguration;
import top.egon.skillopt.autoconfigure.SkillOptSqliteProperties;
import top.egon.skillopt.entity.SkillOptRoundEntity;
import top.egon.skillopt.repository.SkillOptRoundRepository;
import top.egon.skillopt.storage.SkillOptTraceStore;

import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SkillOptSqliteAutoConfigurationTest {

  @TempDir
  Path tempDir;

  @Test
  void createsSqliteJpaStorageAndPersistsRound() {
    Path database = tempDir.resolve("skillopt.db");
    ApplicationContextRunner contextRunner =
        new ApplicationContextRunner().withUserConfiguration(SkillOptSqliteAutoConfiguration.class)
            .withPropertyValues("skill-opt.sqlite.database=" + database);

    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(SkillOptTraceStore.class);
      assertThat(context).hasSingleBean(SkillOptRoundRepository.class);

      SkillOptRoundRepository repository = context.getBean(SkillOptRoundRepository.class);
      SkillOptRoundEntity round = new SkillOptRoundEntity();
      round.setRoundId("round-001");
      round.setSkillName("poem-intent");
      round.setBaseSkillFile("/tmp/SKILL.md");
      round.setBaseSkillHash("sha256:base");
      round.setMinValidationImprovement(0.01);
      round.setAccepted(false);
      round.setGateStatus("RUNNING");
      round.setStartedAt(Instant.now());
      round.setCreatedAt(Instant.now());
      repository.save(round);

      assertThat(database).exists();
      assertThat(repository.findByRoundId("round-001")).isPresent();
    });
  }

  @Test
  void bindsOutputPathAndAutoOverwriteProperties() {
    Path database = tempDir.resolve("skillopt.db");
    Path workspace = tempDir.resolve("skillopt-workspace");
    Path versionsDirectory = tempDir.resolve("skillopt-versions");
    Path bestDirectory = tempDir.resolve("skillopt-best");
    ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(SkillOptSqliteAutoConfiguration.class).withPropertyValues(
            "skill-opt.sqlite.database=" + database, "skill-opt.sqlite.workspace=" + workspace,
            "skill-opt.sqlite.versions-directory=" + versionsDirectory,
            "skill-opt.sqlite.best-directory=" + bestDirectory,
            "skill-opt.sqlite.auto-overwrite-best-skill=true");

    contextRunner.run(context -> {
      SkillOptSqliteProperties properties = context.getBean(SkillOptSqliteProperties.class);

      assertThat(properties.getDatabase()).isEqualTo(database);
      assertThat(properties.getWorkspace()).isEqualTo(workspace);
      assertThat(properties.getVersionsDirectory()).isEqualTo(versionsDirectory);
      assertThat(properties.getBestDirectory()).isEqualTo(bestDirectory);
      assertThat(properties.isAutoOverwriteBestSkill()).isTrue();
      assertThat(properties.toFlowOptions(0.01, 64000).versionsDirectory())
          .isEqualTo(versionsDirectory);
      assertThat(properties.toFlowOptions(0.01, 64000).bestDirectory()).isEqualTo(bestDirectory);
    });
  }
}
