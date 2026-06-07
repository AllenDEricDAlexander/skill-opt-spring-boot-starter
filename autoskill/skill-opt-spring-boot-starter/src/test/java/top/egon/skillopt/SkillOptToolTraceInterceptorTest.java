package top.egon.skillopt;

import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import org.junit.jupiter.api.Test;
import top.egon.skillopt.interceptoer.SkillOptToolTraceInterceptor;
import top.egon.skillopt.record.SkillOptToolCall;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillOptToolTraceInterceptorTest {

  @Test
  void recordsToolCallRequestAndResponse() {
    List<SkillOptToolCall> toolCalls = new ArrayList<>();
    SkillOptToolTraceInterceptor interceptor = new SkillOptToolTraceInterceptor(toolCalls);
    ToolCallRequest request =
        new ToolCallRequest("read_skill", "{\"skill_name\":\"poem-intent\"}", "call-1", null);

    ToolCallResponse response = interceptor.interceptToolCall(request, current -> ToolCallResponse
        .of(current.getToolName(), current.getToolCallId(), "skill content"));

    assertThat(response.getResult()).isEqualTo("skill content");
    assertThat(toolCalls).containsExactly(new SkillOptToolCall("read_skill",
        "{\"skill_name\":\"poem-intent\"}", "skill content", ""));
  }
}
