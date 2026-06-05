package top.egon.skillopt;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoundedSkillEditorTest {

    @Test
    void applyEditsSupportsAddReplaceAndDeleteWithinLimit() {
        BoundedSkillEditor editor = new BoundedSkillEditor(1024);
        String skillContent = "# Skill\n旧约束\n删除项\n## 写诗流程\n";

        String edited = editor.apply(skillContent, List.of(
                SkillEditOperation.replace("旧约束", "新约束"),
                SkillEditOperation.delete("删除项\n"),
                SkillEditOperation.add("## 写诗流程\n", "- 先判断用户意图，再选择意象、体裁和语气。\n")));

        assertThat(edited).contains("新约束");
        assertThat(edited).contains("- 先判断用户意图，再选择意象、体裁和语气。");
        assertThat(edited).doesNotContain("旧约束");
        assertThat(edited).doesNotContain("删除项");
    }

    @Test
    void rejectReplaceWhenTargetIsMissing() {
        BoundedSkillEditor editor = new BoundedSkillEditor(1024);

        assertThatThrownBy(() -> editor.apply("# Skill\n", List.of(
                SkillEditOperation.replace("missing", "new text"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void rejectEditsThatExceedMaxSkillBytes() {
        BoundedSkillEditor editor = new BoundedSkillEditor(12);

        assertThatThrownBy(() -> editor.apply("# Skill\n", List.of(
                SkillEditOperation.add("", "this content is too long"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max skill bytes");
    }
}
