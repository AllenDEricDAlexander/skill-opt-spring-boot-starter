package top.egon.skillopt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;

class SkillOptTraceWriter {

    private final Path workspace;

    SkillOptTraceWriter(Path workspace) {
        this.workspace = workspace;
    }

    void write(String skillName, List<SkillOptRolloutEvidence> rollouts, SkillReflection reflection, boolean accepted)
            throws IOException {
        Path traceFile = workspace.resolve("traces").resolve(safeName(skillName)).resolve("rollouts.jsonl");
        Files.createDirectories(traceFile.getParent());
        StringBuilder builder = new StringBuilder();
        for (SkillOptRolloutEvidence rollout : rollouts) {
            builder.append(toJsonLine(skillName, rollout, reflection, accepted)).append('\n');
        }
        Files.writeString(traceFile, builder.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
    }

    private String toJsonLine(String skillName, SkillOptRolloutEvidence rollout, SkillReflection reflection,
            boolean accepted) {
        return "{"
                + "\"time\":\"" + escape(Instant.now().toString()) + "\","
                + "\"skillName\":\"" + escape(skillName) + "\","
                + "\"caseId\":\"" + escape(rollout.caseId()) + "\","
                + "\"skillFile\":\"" + escape(rollout.skillFile().toString()) + "\","
                + "\"skillHash\":\"" + escape(rollout.skillHash()) + "\","
                + "\"score\":" + rollout.score() + ","
                + "\"passed\":" + rollout.passed() + ","
                + "\"durationMillis\":" + rollout.durationMillis() + ","
                + "\"error\":\"" + escape(rollout.error()) + "\","
                + "\"toolCallCount\":" + rollout.toolCalls().size() + ","
                + "\"reflection\":\"" + escape(reflection.summary()) + "\","
                + "\"accepted\":" + accepted
                + "}";
    }

    private String safeName(String skillName) {
        return skillName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
