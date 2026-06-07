package top.egon.skillopt.interceptoer;

import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import top.egon.skillopt.record.SkillOptToolCall;

import java.util.List;
import java.util.Objects;

/**
 * Records tool calls executed inside the agent loop for rollout evidence.
 */
public class SkillOptToolTraceInterceptor extends ToolInterceptor {

  private final List<SkillOptToolCall> toolCalls;

  /**
   * Creates an interceptor that appends tool call evidence to the given list.
   */
  public SkillOptToolTraceInterceptor(List<SkillOptToolCall> toolCalls) {
    this.toolCalls = Objects.requireNonNull(toolCalls, "toolCalls must not be null");
  }

  /**
   * Captures tool request, response, and exception data without changing behavior.
   */
  @Override
  public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
    try {
      ToolCallResponse response = handler.call(request);
      toolCalls.add(new SkillOptToolCall(request.getToolName(), request.getArguments(),
          response.getResult(), response.isError() ? response.getStatus() : ""));
      return response;
    } catch (RuntimeException ex) {
      toolCalls.add(new SkillOptToolCall(request.getToolName(), request.getArguments(), "",
          ex.getClass().getSimpleName() + ": " + ex.getMessage()));
      throw ex;
    }
  }

  /**
   * Returns the interceptor name shown in agent diagnostics.
   */
  @Override
  public String getName() {
    return "skill-opt-tool-trace";
  }
}
