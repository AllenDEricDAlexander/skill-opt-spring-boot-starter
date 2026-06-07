package top.egon.skillopt.record;

/**
 * Captures one tool call observed during a rollout.
 */
public record SkillOptToolCall(String toolName, String arguments, String result, String error) {

  public SkillOptToolCall {
    toolName = toolName == null ? "" : toolName;
    arguments = arguments == null ? "" : arguments;
    result = result == null ? "" : result;
    error = error == null ? "" : error;
  }
}
