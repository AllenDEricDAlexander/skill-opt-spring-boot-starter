package top.egon.skillopt.storage;

import top.egon.skillopt.record.SkillEditOperation;
import top.egon.skillopt.record.SkillOptCase;
import top.egon.skillopt.record.SkillOptRolloutEvidence;
import top.egon.skillopt.record.SkillReflection;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * No-op trace store used when callers construct the optimizer outside Spring.
 */
public class NoOpSkillOptTraceStore implements SkillOptTraceStore {

  @Override
  public String startRound(String skillName, Path baseSkillFile, String baseSkillHash,
      double minValidationImprovement) {
    return UUID.randomUUID().toString();
  }

  @Override
  public void recordRollout(String roundId, String phase, SkillOptCase skillCase,
      SkillOptRolloutEvidence evidence) {}

  @Override
  public void recordReflection(String roundId, SkillReflection reflection) {}

  @Override
  public void recordEditOperations(String roundId, List<SkillEditOperation> editOperations) {}

  @Override
  public void completeRound(String roundId, double baselineScore, double candidateScore,
      Path baseSkillBackupFile, Path candidateSkillFile, String candidateSkillHash,
      Path bestSkillFile, boolean accepted, String gateReason) {}

  @Override
  public void failRound(String roundId, Exception ex) {}
}
