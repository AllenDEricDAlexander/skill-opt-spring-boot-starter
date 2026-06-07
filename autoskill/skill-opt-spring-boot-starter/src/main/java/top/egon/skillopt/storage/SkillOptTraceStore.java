package top.egon.skillopt.storage;

import top.egon.skillopt.record.SkillEditOperation;
import top.egon.skillopt.record.SkillOptCase;
import top.egon.skillopt.record.SkillOptRolloutEvidence;
import top.egon.skillopt.record.SkillReflection;

import java.nio.file.Path;
import java.util.List;

/**
 * Persists SkillOpt round, rollout, reflection, edit, and gate evidence.
 */
public interface SkillOptTraceStore {

  /**
   * Starts one optimization round and returns its generated round id.
   */
  String startRound(String skillName, Path baseSkillFile, String baseSkillHash,
      double minValidationImprovement);

  /**
   * Persists one rollout and its observed tool calls.
   */
  void recordRollout(String roundId, String phase, SkillOptCase skillCase,
      SkillOptRolloutEvidence evidence);

  /**
   * Persists reflection findings for the current round.
   */
  void recordReflection(String roundId, SkillReflection reflection);

  /**
   * Persists bounded edit operations for the current round.
   */
  void recordEditOperations(String roundId, List<SkillEditOperation> editOperations);

  /**
   * Completes the round after gate evaluation.
   */
  void completeRound(String roundId, double baselineScore, double candidateScore,
      Path baseSkillBackupFile, Path candidateSkillFile, String candidateSkillHash,
      Path bestSkillFile, boolean accepted, String gateReason);

  /**
   * Marks the round failed while preserving previously written evidence.
   */
  void failRound(String roundId, Exception ex);
}
