package top.egon.skillopt.core;

import top.egon.skillopt.record.SkillOptCase;
import top.egon.skillopt.record.SkillOptRolloutEvidence;

import java.nio.file.Path;

/**
 * Runs the fixed target model with one skill version and one case.
 */
@FunctionalInterface
public interface SkillRolloutRunner {

  /**
   * Executes the case against the supplied skill file and returns trace evidence.
   */
  SkillOptRolloutEvidence run(SkillOptCase skillCase, Path skillFile) throws Exception;
}
