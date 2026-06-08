package top.egon.biz;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DemoBizComplexTest {

  @TempDir
  Path tempDir;

  @Test
  void complexEndpointRunsComplexSkillPackageScenario() throws Exception {
    String previousWorkspace = System.getProperty("skillopt.demo.workspace");
    System.setProperty("skillopt.demo.workspace", tempDir.resolve(".skillopt").toString());
    try {
      MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DemoBiz()).build();

      mockMvc.perform(get("/complex")).andExpect(status().isOk())
          .andExpect(
              content().string(org.hamcrest.Matchers.containsString("skill=order-risk-audit")))
          .andExpect(content().string(org.hamcrest.Matchers.containsString("registry=true")))
          .andExpect(
              content().string(org.hamcrest.Matchers.containsString("\"decision\":\"REVIEW\"")))
          .andExpect(
              content().string(org.hamcrest.Matchers.containsString("\"decision\":\"APPROVE\"")));
    } finally {
      if (previousWorkspace == null) {
        System.clearProperty("skillopt.demo.workspace");
      } else {
        System.setProperty("skillopt.demo.workspace", previousWorkspace);
      }
    }
  }
}
