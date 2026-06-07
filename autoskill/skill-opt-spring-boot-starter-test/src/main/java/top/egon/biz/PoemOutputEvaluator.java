package top.egon.biz;

import org.springframework.ai.chat.model.ChatModel;
import top.egon.skillopt.record.SkillOptCase;
import top.egon.skillopt.record.SkillOptToolCall;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Uses an evaluator model to score poem rollout output for SkillOpt gate checks.
 */
class PoemOutputEvaluator {
  private static final Pattern SCORE_FIELD_PATTERN =
      Pattern.compile("\"score\"\\s*:\\s*([01](?:\\.\\d+)?)");
  private static final Pattern NUMBER_PATTERN =
      Pattern.compile("(?<![\\d.])([01](?:\\.\\d+)?)(?![\\d.])");

  private final ChatModel chatModel;

  PoemOutputEvaluator(ChatModel chatModel) {
    this.chatModel = Objects.requireNonNull(chatModel, "chatModel must not be null");
  }

  /**
   * Scores one rollout by asking the evaluator model to judge task fit and skill usage.
   */
  double score(SkillOptCase skillCase, String output, List<SkillOptToolCall> toolCalls) {
    String response = chatModel.call(buildScorePrompt(skillCase, output, toolCalls));
    return parseScore(response);
  }

  private String buildScorePrompt(SkillOptCase skillCase, String output,
      List<SkillOptToolCall> toolCalls) {
    return """
        你是 SkillOpt 写诗任务的评分模型。请根据用户任务、期望行为、目标模型输出和工具调用证据打分。
        分数范围是 0.0 到 1.0，越高表示越符合期望。

        评分重点：
        - 是否读取并使用 poem-intent skill。
        - 是否识别用户意图、场景、意象和语气。
        - 是否完成诗歌输出，并且输出和用户任务一致。
        - 不要因为文字很长就给高分，优先判断任务完成质量。

        只输出 JSON，不要输出其他文本：
        {"score":0.0,"reason":"一句话说明理由"}

        用户任务：
        %s

        期望行为：
        %s

        目标模型输出：
        %s

        工具调用证据：
        %s
        """.formatted(skillCase.input(), skillCase.expected(), safe(output),
        summarizeToolCalls(toolCalls));
  }

  private String summarizeToolCalls(List<SkillOptToolCall> toolCalls) {
    if (toolCalls == null || toolCalls.isEmpty()) {
      return "无";
    }
    StringBuilder builder = new StringBuilder();
    for (SkillOptToolCall toolCall : toolCalls) {
      builder.append("tool=").append(toolCall.toolName()).append(", arguments=")
          .append(truncate(toolCall.arguments(), 160)).append(", result=")
          .append(truncate(toolCall.result(), 160)).append(", error=")
          .append(truncate(toolCall.error(), 160)).append('\n');
    }
    return builder.toString();
  }

  private double parseScore(String response) {
    String value = safe(response);
    Matcher scoreMatcher = SCORE_FIELD_PATTERN.matcher(value);
    if (scoreMatcher.find()) {
      return clamp(Double.parseDouble(scoreMatcher.group(1)));
    }
    Matcher numberMatcher = NUMBER_PATTERN.matcher(value);
    if (numberMatcher.find()) {
      return clamp(Double.parseDouble(numberMatcher.group(1)));
    }
    throw new IllegalStateException("model score response does not contain score: " + value);
  }

  private double clamp(double score) {
    if (score < 0.0) {
      return 0.0;
    }
    if (score > 1.0) {
      return 1.0;
    }
    return score;
  }

  private String safe(String value) {
    return value == null ? "" : value;
  }

  private String truncate(String value, int maxLength) {
    String safeValue = safe(value);
    if (safeValue.length() <= maxLength) {
      return safeValue;
    }
    return safeValue.substring(0, maxLength);
  }
}
