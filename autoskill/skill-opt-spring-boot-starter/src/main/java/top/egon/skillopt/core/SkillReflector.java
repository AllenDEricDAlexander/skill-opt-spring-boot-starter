package top.egon.skillopt.core;

import top.egon.skillopt.record.SkillOptRolloutEvidence;
import top.egon.skillopt.record.SkillReflection;

import java.util.List;

/**
 * Reflects over rollout evidence and extracts common failure patterns.
 */
@FunctionalInterface
public interface SkillReflector {

  /**
   * Analyzes successful and failed rollout cases before edit planning.
   */
  SkillReflection reflect(List<SkillOptRolloutEvidence> rollouts) throws Exception;
}
