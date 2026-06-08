package top.egon.skillopt;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.tools.ShellTool2;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.ai.AiService;
import com.alibaba.nacos.maintainer.client.ai.AiMaintainerFactory;
import com.alibaba.nacos.maintainer.client.ai.AiMaintainerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import top.egon.skillopt.nacos.NacosSkillPackageRepository;
import top.egon.skillopt.nacos.SdkNacosSkillClient;
import top.egon.skillopt.record.NacosSkillLocation;
import top.egon.skillopt.record.SkillPackage;
import top.egon.skillopt.sandbox.SkillPackageShellOptions;
import top.egon.skillopt.sandbox.SkillPackageShellToolFactory;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = "skillopt.nacos.live", matches = "true")
class ComplexSkillNacosLiveIT {

  private static final String FIXTURE_SKILL_NAME = "order-risk-audit";

  @TempDir
  Path tempDir;

  @Test
  void uploadsDownloadsAndRunsComplexSkillThroughLocalNacos() throws Exception {
    long suffix = System.currentTimeMillis();
    String liveSkillName = FIXTURE_SKILL_NAME + "-smoke-" + suffix;
    String liveVersion = "v" + suffix;
    String namespaceId = systemProperty("skillopt.nacos.namespaceId", "public");
    Properties properties = nacosProperties(namespaceId);
    AiService aiService = AiFactory.createAiService(properties);
    AiMaintainerService maintainerService =
        AiMaintainerFactory.createAiMaintainerService(properties);
    SdkNacosSkillClient client = new SdkNacosSkillClient(aiService, maintainerService);
    NacosSkillPackageRepository repository =
        new NacosSkillPackageRepository(client, tempDir.resolve(".skillopt/nacos-cache"));
    boolean uploaded = false;

    try {
      SkillPackage localPackage = copyFixtureAsSkill(liveSkillName);
      String uploadedName = repository.upload(localPackage,
          new NacosSkillLocation(namespaceId, liveSkillName, "", "", true), liveVersion,
          "skillopt complex live smoke");
      uploaded = true;
      assertThat(uploadedName).isEqualTo(liveSkillName);
      assertThat(
          maintainerService.skill().forcePublish(namespaceId, liveSkillName, liveVersion, true))
          .isTrue();

      SkillPackage downloadedPackage = repository
          .download(new NacosSkillLocation(namespaceId, liveSkillName, liveVersion, "", false));
      SkillRegistry registry = downloadedPackage.createFileSystemSkillRegistry();
      assertThat(registry.contains(liveSkillName)).isTrue();
      assertThat(registry.readSkillContent(liveSkillName)).contains("订单风险审计",
          "scripts/audit_order.sh");

      ShellTool2 shellTool = new SkillPackageShellToolFactory(
          new SkillPackageShellOptions(5000L, 50, List.of("/bin/sh"), Map.of()))
          .createShellTool(downloadedPackage);
      RunnableConfig config = RunnableConfig.builder().threadId("complex-nacos-live").build();
      shellTool.getSessionManager().initialize(config);
      try {
        String output = shellTool.getSessionManager()
            .executeCommand("sh scripts/audit_order.sh examples/high-risk-order.json", config)
            .getOutput();

        assertThat(output).contains("\"decision\":\"REVIEW\"", "high_amount", "blocked_country",
            "manual_review_required");
      } finally {
        shellTool.getSessionManager().cleanup(config);
      }
    } finally {
      if (uploaded) {
        maintainerService.skill().deleteSkill(namespaceId, liveSkillName);
      }
      aiService.shutdown();
    }
  }

  private Properties nacosProperties(String namespaceId) {
    String password = systemProperty("skillopt.nacos.password",
        System.getenv().getOrDefault("NACOS_PASSWORD", ""));
    assertThat(password).as("Nacos password must be supplied by system property or env")
        .isNotBlank();

    Properties properties = new Properties();
    properties.setProperty(PropertyKeyConst.SERVER_ADDR,
        systemProperty("skillopt.nacos.serverAddr", "127.0.0.1:8848"));
    properties.setProperty(PropertyKeyConst.NAMESPACE, namespaceId);
    properties.setProperty(PropertyKeyConst.USERNAME, systemProperty("skillopt.nacos.username",
        System.getenv().getOrDefault("NACOS_USERNAME", "test")));
    properties.setProperty(PropertyKeyConst.PASSWORD, password);
    String contextPath = systemProperty("skillopt.nacos.contextPath", "");
    if (!contextPath.isBlank()) {
      properties.setProperty(PropertyKeyConst.CONTEXT_PATH, contextPath);
    }
    return properties;
  }

  private String systemProperty(String key, String defaultValue) {
    String value = System.getProperty(key);
    return value == null || value.isBlank() ? defaultValue : value;
  }

  private SkillPackage copyFixtureAsSkill(String skillName) throws Exception {
    URL resource =
        getClass().getClassLoader().getResource("skills/" + FIXTURE_SKILL_NAME + "/SKILL.md");
    assertThat(resource).as("missing complex skill fixture").isNotNull();
    Path sourceRoot = Path.of(resource.toURI()).getParent();
    Path targetRoot = tempDir.resolve("live-skill").resolve(skillName);
    try (Stream<Path> paths = Files.walk(sourceRoot)) {
      for (Path source : paths.sorted(Comparator.naturalOrder()).toList()) {
        Path target = targetRoot.resolve(sourceRoot.relativize(source)).normalize();
        if (Files.isDirectory(source)) {
          Files.createDirectories(target);
        } else if (source.getFileName().toString().equals("SKILL.md")) {
          Files.createDirectories(target.getParent());
          String content = Files.readString(source, StandardCharsets.UTF_8)
              .replace("name: " + FIXTURE_SKILL_NAME, "name: " + skillName);
          Files.writeString(target, content, StandardCharsets.UTF_8);
        } else {
          Files.createDirectories(target.getParent());
          Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }
    return new SkillPackage(skillName, "", "fixture", targetRoot, targetRoot.resolve("SKILL.md"));
  }
}
