package top.egon.skillopt;

import java.util.Objects;

/**
 * Describes one bounded text edit against a skill document.
 */
public record SkillEditOperation(SkillEditOperationType type, String target, String content) {

    public SkillEditOperation {
        Objects.requireNonNull(type, "type must not be null");
        target = target == null ? "" : target;
        content = content == null ? "" : content;
    }

    /**
     * Adds content after target, or appends it when target is blank.
     */
    public static SkillEditOperation add(String target, String content) {
        return new SkillEditOperation(SkillEditOperationType.ADD, target, content);
    }

    /**
     * Deletes the first exact target match.
     */
    public static SkillEditOperation delete(String target) {
        return new SkillEditOperation(SkillEditOperationType.DELETE, target, "");
    }

    /**
     * Replaces the first exact target match with content.
     */
    public static SkillEditOperation replace(String target, String content) {
        return new SkillEditOperation(SkillEditOperationType.REPLACE, target, content);
    }
}
