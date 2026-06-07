package top.egon.skillopt.core;

import top.egon.skillopt.record.SkillEditOperation;
import top.egon.skillopt.record.SkillOptCase;
import top.egon.skillopt.record.SkillOptFlowOptions;
import top.egon.skillopt.record.SkillOptRolloutEvidence;
import top.egon.skillopt.record.SkillOptimizationResult;
import top.egon.skillopt.record.SkillReflection;
import top.egon.skillopt.storage.NoOpSkillOptTraceStore;
import top.egon.skillopt.storage.SkillOptTraceStore;
import top.egon.skillopt.utils.SkillOptHash;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Runs one Rollout -> Reflect -> Edit -> Gate -> Export optimization round.
 */
public class SingleFlowSkillOptimizer {

  private final SkillOptFlowOptions options;
  private final SkillRolloutRunner rolloutRunner;
  private final SkillReflector reflector;
  private final SkillEditPlanner editPlanner;
  private final BoundedSkillEditor skillEditor;
  private final SkillOptTraceStore traceStore;

  /**
   * Creates a single-round optimizer with caller-provided model behaviors.
   */
  public SingleFlowSkillOptimizer(SkillOptFlowOptions options, SkillRolloutRunner rolloutRunner,
      SkillReflector reflector, SkillEditPlanner editPlanner) {
    this(options, rolloutRunner, reflector, editPlanner, new NoOpSkillOptTraceStore());
  }

  /**
   * Creates a single-round optimizer with persistent trace storage.
   */
  public SingleFlowSkillOptimizer(SkillOptFlowOptions options, SkillRolloutRunner rolloutRunner,
      SkillReflector reflector, SkillEditPlanner editPlanner, SkillOptTraceStore traceStore) {
    this.options = Objects.requireNonNull(options, "options must not be null");
    this.rolloutRunner = Objects.requireNonNull(rolloutRunner, "rolloutRunner must not be null");
    this.reflector = Objects.requireNonNull(reflector, "reflector must not be null");
    this.editPlanner = Objects.requireNonNull(editPlanner, "editPlanner must not be null");
    this.skillEditor = new BoundedSkillEditor(options.maxSkillBytes());
    this.traceStore = Objects.requireNonNull(traceStore, "traceStore must not be null");
  }

  /**
   * Optimizes the given skill once and exports only accepted validation winners.
   */
  public SkillOptimizationResult optimize(String skillName, Path skillFile,
      List<SkillOptCase> reflectionCases, List<SkillOptCase> validationCases) throws Exception {
    Objects.requireNonNull(skillName, "skillName must not be null");
    Objects.requireNonNull(skillFile, "skillFile must not be null");
    Objects.requireNonNull(reflectionCases, "reflectionCases must not be null");
    Objects.requireNonNull(validationCases, "validationCases must not be null");
    if (skillName.isBlank()) {
      throw new IllegalArgumentException("skillName must not be blank");
    }
    if (validationCases.isEmpty()) {
      throw new IllegalArgumentException("validationCases must not be empty");
    }

    String roundId = traceStore.startRound(skillName, skillFile, SkillOptHash.sha256(skillFile),
        options.minValidationImprovement());
    try {
      List<SkillOptRolloutEvidence> rollouts = new ArrayList<>();
      rollouts.addAll(runRollouts(roundId, "REFLECTION", reflectionCases, skillFile));

      SkillReflection reflection = reflector.reflect(List.copyOf(rollouts));
      traceStore.recordReflection(roundId, reflection);
      String skillContent = Files.readString(skillFile, StandardCharsets.UTF_8);
      List<SkillEditOperation> editOperations = editPlanner.plan(skillContent, reflection);
      String candidateContent = skillEditor.apply(skillContent, editOperations);
      traceStore.recordEditOperations(roundId, editOperations);
      Path baseSkillBackupFile = writeBaseSkillBackup(skillName, skillContent, roundId);
      Path candidateSkillFile = writeCandidate(skillName, candidateContent, roundId);

      List<SkillOptRolloutEvidence> baselineValidation =
          runRollouts(roundId, "VALIDATION_BASELINE", validationCases, skillFile);
      List<SkillOptRolloutEvidence> candidateValidation =
          runRollouts(roundId, "VALIDATION_CANDIDATE", validationCases, candidateSkillFile);
      rollouts.addAll(baselineValidation);
      rollouts.addAll(candidateValidation);

      double baselineScore = averageScore(baselineValidation);
      double candidateScore = averageScore(candidateValidation);
      boolean accepted = candidateScore > baselineScore
          && candidateScore - baselineScore >= options.minValidationImprovement();
      Path bestSkillFile = null;
      if (accepted) {
        bestSkillFile = exportBestSkill(skillName, skillFile, candidateContent);
      }
      String candidateSkillHash = SkillOptHash.sha256(candidateSkillFile);
      traceStore.completeRound(roundId, baselineScore, candidateScore, baseSkillBackupFile,
          candidateSkillFile, candidateSkillHash, bestSkillFile, accepted,
          gateReason(accepted, baselineScore, candidateScore));
      return new SkillOptimizationResult(skillName, accepted, baselineScore, candidateScore,
          candidateSkillFile, bestSkillFile, reflection, List.copyOf(rollouts));
    } catch (Exception ex) {
      traceStore.failRound(roundId, ex);
      throw ex;
    }
  }

  private List<SkillOptRolloutEvidence> runRollouts(String roundId, String phase,
      List<SkillOptCase> cases, Path skillFile) {
    List<SkillOptRolloutEvidence> rollouts = new ArrayList<>();
    for (SkillOptCase skillCase : cases) {
      long start = System.nanoTime();
      SkillOptRolloutEvidence evidence;
      try {
        evidence = rolloutRunner.run(skillCase, skillFile).withDurationMillis(elapsedMillis(start));
      } catch (Exception ex) {
        evidence = SkillOptRolloutEvidence.failure(skillCase.id(), skillCase.input(), skillFile, ex,
            elapsedMillis(start));
      }
      traceStore.recordRollout(roundId, phase, skillCase, evidence);
      rollouts.add(evidence);
    }
    return rollouts;
  }

  private Path writeCandidate(String skillName, String candidateContent, String roundId)
      throws IOException {
    Path candidateSkillFile = options.versionsDirectory().resolve(safeName(skillName))
        .resolve(roundId).resolve(safeName(skillName)).resolve("SKILL.md");
    Files.createDirectories(candidateSkillFile.getParent());
    Files.writeString(candidateSkillFile, candidateContent, StandardCharsets.UTF_8);
    return candidateSkillFile;
  }

  private Path writeBaseSkillBackup(String skillName, String skillContent, String roundId)
      throws IOException {
    Path baseSkillBackupFile = options.versionsDirectory().resolve(safeName(skillName))
        .resolve(roundId).resolve(safeName(skillName)).resolve("base_skill.md");
    Files.createDirectories(baseSkillBackupFile.getParent());
    Files.writeString(baseSkillBackupFile, skillContent, StandardCharsets.UTF_8);
    return baseSkillBackupFile;
  }

  private Path exportBestSkill(String skillName, Path skillFile, String candidateContent)
      throws IOException {
    Path bestSkillFile =
        options.bestDirectory().resolve(safeName(skillName)).resolve("best_skill.md");
    Files.createDirectories(bestSkillFile.getParent());
    Files.writeString(bestSkillFile, candidateContent, StandardCharsets.UTF_8);
    if (options.autoOverwriteBestSkill()) {
      Files.writeString(skillFile, candidateContent, StandardCharsets.UTF_8);
    }
    return bestSkillFile;
  }

  private double averageScore(List<SkillOptRolloutEvidence> rollouts) {
    return rollouts.stream().mapToDouble(SkillOptRolloutEvidence::score).average().orElse(0.0);
  }

  private long elapsedMillis(long startNanos) {
    return Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
  }

  private String gateReason(boolean accepted, double baselineScore, double candidateScore) {
    if (accepted) {
      return "candidate score improved from " + baselineScore + " to " + candidateScore;
    }
    return "candidate score did not improve enough: baseline=" + baselineScore + ", candidate="
        + candidateScore;
  }

  private String safeName(String skillName) {
    return skillName.replaceAll("[^a-zA-Z0-9._-]", "_");
  }
}
