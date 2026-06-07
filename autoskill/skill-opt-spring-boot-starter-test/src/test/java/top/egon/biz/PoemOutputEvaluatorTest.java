package top.egon.biz;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import top.egon.skillopt.enums.SkillOptCaseSplit;
import top.egon.skillopt.record.SkillOptCase;
import top.egon.skillopt.record.SkillOptToolCall;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PoemOutputEvaluatorTest {

  @Test
  void usesChatModelToScorePoemOutput() {
    CapturingChatModel chatModel =
        new CapturingChatModel("{\"score\":0.86,\"reason\":\"识别了意图并完成诗歌输出\"}");
    PoemOutputEvaluator evaluator = new PoemOutputEvaluator(chatModel);
    SkillOptCase skillCase =
        new SkillOptCase("valid-001", "帮我写一首春天里重逢的短诗", "需要识别重逢和春天意象", SkillOptCaseSplit.VALIDATION);
    List<SkillOptToolCall> toolCalls =
        List.of(new SkillOptToolCall("read_skill", "{\"skill\":\"poem-intent\"}", "loaded", ""));

    double score = evaluator.score(skillCase, "意图：重逢与春天\n春风把旧路吹亮\n我们在花影里相认", toolCalls);

    assertThat(score).isEqualTo(0.86);
    assertThat(chatModel.prompt()).contains("帮我写一首春天里重逢的短诗", "需要识别重逢和春天意象", "意图：重逢与春天",
        "read_skill");
  }

  private static class CapturingChatModel implements ChatModel {
    private final String response;
    private String prompt;

    private CapturingChatModel(String response) {
      this.response = response;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
      this.prompt = prompt.getContents();
      return new ChatResponse(List.of(new Generation(new AssistantMessage(response))));
    }

    private String prompt() {
      return prompt;
    }
  }
}
