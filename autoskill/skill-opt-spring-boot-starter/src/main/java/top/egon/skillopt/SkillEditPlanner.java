package top.egon.skillopt;

import java.util.List;

/**
 * Converts reflection findings into bounded skill edit operations.
 */
@FunctionalInterface
public interface SkillEditPlanner {

    /**
     * Plans limited add/delete/replace edits for the current skill content.
     */
    List<SkillEditOperation> plan(String skillContent, SkillReflection reflection) throws Exception;
}
