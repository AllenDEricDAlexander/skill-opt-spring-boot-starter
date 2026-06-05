package top.egon.skillopt;

import java.util.List;

/**
 * Holds optimizer reflection findings for later bounded editing.
 */
public record SkillReflection(String summary, List<String> recommendations) {

    public SkillReflection {
        summary = summary == null ? "" : summary;
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
    }
}
