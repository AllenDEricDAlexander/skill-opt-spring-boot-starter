package top.egon.skillopt;

import java.util.Objects;

/**
 * Defines one rollout case used by reflection or held-out validation.
 */
public record SkillOptCase(String id, String input, String expected, SkillOptCaseSplit split) {

    public SkillOptCase {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("case id must not be blank");
        }
        Objects.requireNonNull(input, "input must not be null");
        expected = expected == null ? "" : expected;
        Objects.requireNonNull(split, "split must not be null");
    }
}
