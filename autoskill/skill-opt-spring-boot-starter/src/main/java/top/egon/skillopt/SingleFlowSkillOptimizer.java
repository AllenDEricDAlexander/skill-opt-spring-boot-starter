package top.egon.skillopt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
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

    /**
     * Creates a single-round optimizer with caller-provided model behaviors.
     */
    public SingleFlowSkillOptimizer(SkillOptFlowOptions options, SkillRolloutRunner rolloutRunner,
            SkillReflector reflector, SkillEditPlanner editPlanner) {
        this.options = Objects.requireNonNull(options, "options must not be null");
        this.rolloutRunner = Objects.requireNonNull(rolloutRunner, "rolloutRunner must not be null");
        this.reflector = Objects.requireNonNull(reflector, "reflector must not be null");
        this.editPlanner = Objects.requireNonNull(editPlanner, "editPlanner must not be null");
        this.skillEditor = new BoundedSkillEditor(options.maxSkillBytes());
    }

    /**
     * Optimizes the given skill once and exports only accepted validation winners.
     */
    public SkillOptimizationResult optimize(String skillName, Path skillFile, List<SkillOptCase> reflectionCases,
            List<SkillOptCase> validationCases) throws Exception {
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

        List<SkillOptRolloutEvidence> rollouts = new ArrayList<>();
        rollouts.addAll(runRollouts(reflectionCases, skillFile));

        SkillReflection reflection = reflector.reflect(List.copyOf(rollouts));
        String skillContent = Files.readString(skillFile, StandardCharsets.UTF_8);
        String candidateContent = skillEditor.apply(skillContent, editPlanner.plan(skillContent, reflection));
        Path candidateSkillFile = writeCandidate(skillName, candidateContent);

        List<SkillOptRolloutEvidence> baselineValidation = runRollouts(validationCases, skillFile);
        List<SkillOptRolloutEvidence> candidateValidation = runRollouts(validationCases, candidateSkillFile);
        rollouts.addAll(baselineValidation);
        rollouts.addAll(candidateValidation);

        double baselineScore = averageScore(baselineValidation);
        double candidateScore = averageScore(candidateValidation);
        boolean accepted = candidateScore > baselineScore
                && candidateScore - baselineScore >= options.minValidationImprovement();
        Path bestSkillFile = null;
        if (accepted) {
            bestSkillFile = exportBestSkill(skillName, candidateContent);
        }
        new SkillOptTraceWriter(options.workspace()).write(skillName, rollouts, reflection, accepted);
        return new SkillOptimizationResult(skillName, accepted, baselineScore, candidateScore, candidateSkillFile,
                bestSkillFile, reflection, List.copyOf(rollouts));
    }

    private List<SkillOptRolloutEvidence> runRollouts(List<SkillOptCase> cases, Path skillFile) {
        List<SkillOptRolloutEvidence> rollouts = new ArrayList<>();
        for (SkillOptCase skillCase : cases) {
            long start = System.nanoTime();
            try {
                SkillOptRolloutEvidence evidence = rolloutRunner.run(skillCase, skillFile);
                rollouts.add(evidence.withDurationMillis(elapsedMillis(start)));
            }
            catch (Exception ex) {
                rollouts.add(SkillOptRolloutEvidence.failure(skillCase.id(), skillCase.input(), skillFile, ex,
                        elapsedMillis(start)));
            }
        }
        return rollouts;
    }

    private Path writeCandidate(String skillName, String candidateContent) throws IOException {
        String roundId = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(':', '-');
        Path candidateSkillFile = options.workspace().resolve("versions").resolve(safeName(skillName)).resolve(roundId)
                .resolve(safeName(skillName)).resolve("SKILL.md");
        Files.createDirectories(candidateSkillFile.getParent());
        Files.writeString(candidateSkillFile, candidateContent, StandardCharsets.UTF_8);
        return candidateSkillFile;
    }

    private Path exportBestSkill(String skillName, String candidateContent) throws IOException {
        Path bestSkillFile = options.workspace().resolve("best").resolve(safeName(skillName)).resolve("best_skill.md");
        Files.createDirectories(bestSkillFile.getParent());
        Files.writeString(bestSkillFile, candidateContent, StandardCharsets.UTF_8);
        return bestSkillFile;
    }

    private double averageScore(List<SkillOptRolloutEvidence> rollouts) {
        return rollouts.stream().mapToDouble(SkillOptRolloutEvidence::score).average().orElse(0.0);
    }

    private long elapsedMillis(long startNanos) {
        return Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
    }

    private String safeName(String skillName) {
        return skillName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
