package top.egon.skillopt.storage;

import org.springframework.transaction.annotation.Transactional;
import top.egon.skillopt.entity.SkillOptEditOperationEntity;
import top.egon.skillopt.entity.SkillOptReflectionEntity;
import top.egon.skillopt.entity.SkillOptRolloutEntity;
import top.egon.skillopt.entity.SkillOptRoundEntity;
import top.egon.skillopt.entity.SkillOptToolCallEntity;
import top.egon.skillopt.record.SkillEditOperation;
import top.egon.skillopt.record.SkillOptCase;
import top.egon.skillopt.record.SkillOptRolloutEvidence;
import top.egon.skillopt.record.SkillOptToolCall;
import top.egon.skillopt.record.SkillReflection;
import top.egon.skillopt.repository.SkillOptEditOperationRepository;
import top.egon.skillopt.repository.SkillOptReflectionRepository;
import top.egon.skillopt.repository.SkillOptRolloutRepository;
import top.egon.skillopt.repository.SkillOptRoundRepository;
import top.egon.skillopt.repository.SkillOptToolCallRepository;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA implementation of SkillOpt trace persistence.
 */
public class JpaSkillOptTraceStore implements SkillOptTraceStore {

  private final SkillOptRoundRepository roundRepository;
  private final SkillOptRolloutRepository rolloutRepository;
  private final SkillOptToolCallRepository toolCallRepository;
  private final SkillOptReflectionRepository reflectionRepository;
  private final SkillOptEditOperationRepository editOperationRepository;

  /**
   * Creates the trace store with Spring Data JPA repositories.
   */
  public JpaSkillOptTraceStore(SkillOptRoundRepository roundRepository,
      SkillOptRolloutRepository rolloutRepository, SkillOptToolCallRepository toolCallRepository,
      SkillOptReflectionRepository reflectionRepository,
      SkillOptEditOperationRepository editOperationRepository) {
    this.roundRepository =
        Objects.requireNonNull(roundRepository, "roundRepository must not be null");
    this.rolloutRepository =
        Objects.requireNonNull(rolloutRepository, "rolloutRepository must not be null");
    this.toolCallRepository =
        Objects.requireNonNull(toolCallRepository, "toolCallRepository must not be null");
    this.reflectionRepository =
        Objects.requireNonNull(reflectionRepository, "reflectionRepository must not be null");
    this.editOperationRepository =
        Objects.requireNonNull(editOperationRepository, "editOperationRepository must not be null");
  }

  @Override
  @Transactional("skillOptTransactionManager")
  public String startRound(String skillName, Path baseSkillFile, String baseSkillHash,
      double minValidationImprovement) {
    String roundId = UUID.randomUUID().toString();
    Instant now = Instant.now();
    SkillOptRoundEntity round = new SkillOptRoundEntity();
    round.setRoundId(roundId);
    round.setSkillName(skillName);
    round.setBaseSkillFile(pathValue(baseSkillFile));
    round.setBaseSkillHash(baseSkillHash);
    round.setMinValidationImprovement(minValidationImprovement);
    round.setAccepted(false);
    round.setGateStatus("RUNNING");
    round.setStartedAt(now);
    round.setCreatedAt(now);
    roundRepository.save(round);
    return roundId;
  }

  @Override
  @Transactional("skillOptTransactionManager")
  public void recordRollout(String roundId, String phase, SkillOptCase skillCase,
      SkillOptRolloutEvidence evidence) {
    Instant now = Instant.now();
    SkillOptRolloutEntity rollout = new SkillOptRolloutEntity();
    rollout.setRoundId(roundId);
    rollout.setPhase(phase);
    rollout.setCaseId(skillCase.id());
    rollout.setCaseSplit(skillCase.split().name());
    rollout.setInput(skillCase.input());
    rollout.setExpected(skillCase.expected());
    rollout.setSkillFile(pathValue(evidence.skillFile()));
    rollout.setSkillHash(evidence.skillHash());
    rollout.setOutput(evidence.output());
    rollout.setScore(evidence.score());
    rollout.setPassed(evidence.passed());
    rollout.setError(evidence.error());
    rollout.setDurationMillis(evidence.durationMillis());
    rollout.setStartedAt(now.minusMillis(evidence.durationMillis()));
    rollout.setCompletedAt(now);
    rollout.setCreatedAt(now);
    SkillOptRolloutEntity savedRollout = rolloutRepository.save(rollout);
    saveToolCalls(roundId, savedRollout.getId(), evidence.toolCalls(), now);
  }

  @Override
  @Transactional("skillOptTransactionManager")
  public void recordReflection(String roundId, SkillReflection reflection) {
    SkillOptReflectionEntity entity = new SkillOptReflectionEntity();
    entity.setRoundId(roundId);
    entity.setSummary(reflection.summary());
    entity.setRecommendationsJson(toJsonArray(reflection.recommendations()));
    entity.setResponseSnapshot(reflection.summary());
    entity.setCreatedAt(Instant.now());
    reflectionRepository.save(entity);
  }

  @Override
  @Transactional("skillOptTransactionManager")
  public void recordEditOperations(String roundId, List<SkillEditOperation> editOperations) {
    Instant now = Instant.now();
    for (int i = 0; i < editOperations.size(); i++) {
      SkillEditOperation editOperation = editOperations.get(i);
      SkillOptEditOperationEntity entity = new SkillOptEditOperationEntity();
      entity.setRoundId(roundId);
      entity.setSequenceNo(i + 1);
      entity.setEditType(editOperation.type().name());
      entity.setTarget(editOperation.target());
      entity.setContent(editOperation.content());
      entity.setApplied(true);
      entity.setCreatedAt(now);
      editOperationRepository.save(entity);
    }
  }

  @Override
  @Transactional("skillOptTransactionManager")
  public void completeRound(String roundId, double baselineScore, double candidateScore,
      Path baseSkillBackupFile, Path candidateSkillFile, String candidateSkillHash,
      Path bestSkillFile, boolean accepted, String gateReason) {
    SkillOptRoundEntity round = findRound(roundId);
    round.setBaselineScore(baselineScore);
    round.setCandidateScore(candidateScore);
    round.setBaseSkillBackupFile(pathValue(baseSkillBackupFile));
    round.setCandidateSkillFile(pathValue(candidateSkillFile));
    round.setCandidateSkillHash(candidateSkillHash);
    round.setBestSkillFile(pathValue(bestSkillFile));
    round.setAccepted(accepted);
    round.setGateStatus(accepted ? "ACCEPTED" : "REJECTED");
    round.setGateReason(gateReason);
    round.setCompletedAt(Instant.now());
    roundRepository.save(round);
  }

  @Override
  @Transactional("skillOptTransactionManager")
  public void failRound(String roundId, Exception ex) {
    SkillOptRoundEntity round = findRound(roundId);
    round.setAccepted(false);
    round.setGateStatus("FAILED");
    round.setGateReason(ex == null ? "" : ex.getClass().getSimpleName() + ": " + ex.getMessage());
    round.setCompletedAt(Instant.now());
    roundRepository.save(round);
  }

  private void saveToolCalls(String roundId, Long rolloutId, List<SkillOptToolCall> toolCalls,
      Instant now) {
    for (int i = 0; i < toolCalls.size(); i++) {
      SkillOptToolCall toolCall = toolCalls.get(i);
      SkillOptToolCallEntity entity = new SkillOptToolCallEntity();
      entity.setRoundId(roundId);
      entity.setRolloutId(rolloutId);
      entity.setSequenceNo(i + 1);
      entity.setToolName(toolCall.toolName());
      entity.setArguments(toolCall.arguments());
      entity.setResult(toolCall.result());
      entity.setError(toolCall.error());
      entity.setCreatedAt(now);
      toolCallRepository.save(entity);
    }
  }

  private SkillOptRoundEntity findRound(String roundId) {
    return roundRepository.findByRoundId(roundId)
        .orElseThrow(() -> new IllegalStateException("missing skill opt round: " + roundId));
  }

  private String pathValue(Path path) {
    return path == null ? null : path.toString();
  }

  private String toJsonArray(List<String> values) {
    StringBuilder builder = new StringBuilder("[");
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        builder.append(',');
      }
      builder.append('"').append(escape(values.get(i))).append('"');
    }
    return builder.append(']').toString();
  }

  private String escape(String value) {
    String safeValue = value == null ? "" : value;
    return safeValue.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r",
        "\\r");
  }
}
