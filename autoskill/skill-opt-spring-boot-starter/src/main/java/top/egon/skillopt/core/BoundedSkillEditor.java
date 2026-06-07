package top.egon.skillopt.core;

import top.egon.skillopt.record.SkillEditOperation;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Applies bounded add/delete/replace edits to a skill document.
 */
public class BoundedSkillEditor {

  private final int maxSkillBytes;

  /**
   * Creates an editor with the maximum accepted UTF-8 skill size.
   */
  public BoundedSkillEditor(int maxSkillBytes) {
    if (maxSkillBytes <= 0) {
      throw new IllegalArgumentException("max skill bytes must be positive");
    }
    this.maxSkillBytes = maxSkillBytes;
  }

  /**
   * Applies all edits in order and rejects missing targets or oversize results.
   */
  public String apply(String skillContent, List<SkillEditOperation> edits) {
    Objects.requireNonNull(skillContent, "skillContent must not be null");
    Objects.requireNonNull(edits, "edits must not be null");
    String edited = skillContent;
    for (SkillEditOperation edit : edits) {
      edited = applyOne(edited, edit);
      checkSize(edited);
    }
    return edited;
  }

  private String applyOne(String skillContent, SkillEditOperation edit) {
    Objects.requireNonNull(edit, "edit must not be null");
    return switch (edit.type()) {
      case ADD -> add(skillContent, edit);
      case DELETE -> delete(skillContent, edit);
      case REPLACE -> replace(skillContent, edit);
    };
  }

  private String add(String skillContent, SkillEditOperation edit) {
    if (edit.target().isBlank()) {
      return skillContent + edit.content();
    }
    int index = skillContent.indexOf(edit.target());
    if (index < 0) {
      throw new IllegalArgumentException("missing add target: " + edit.target());
    }
    int insertIndex = index + edit.target().length();
    return skillContent.substring(0, insertIndex) + edit.content()
        + skillContent.substring(insertIndex);
  }

  private String delete(String skillContent, SkillEditOperation edit) {
    requireTarget(edit);
    int index = skillContent.indexOf(edit.target());
    if (index < 0) {
      throw new IllegalArgumentException("missing delete target: " + edit.target());
    }
    return skillContent.substring(0, index)
        + skillContent.substring(index + edit.target().length());
  }

  private String replace(String skillContent, SkillEditOperation edit) {
    requireTarget(edit);
    int index = skillContent.indexOf(edit.target());
    if (index < 0) {
      throw new IllegalArgumentException("missing replace target: " + edit.target());
    }
    return skillContent.substring(0, index) + edit.content()
        + skillContent.substring(index + edit.target().length());
  }

  private void requireTarget(SkillEditOperation edit) {
    if (edit.target().isBlank()) {
      throw new IllegalArgumentException(
          edit.type().name().toLowerCase() + " target must not be blank");
    }
  }

  private void checkSize(String skillContent) {
    if (skillContent.getBytes(StandardCharsets.UTF_8).length > maxSkillBytes) {
      throw new IllegalArgumentException("edited skill exceeds max skill bytes: " + maxSkillBytes);
    }
  }
}
