package top.egon.skillopt;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.tools.ShellTool2;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.skillopt.nacos.NacosSkillClient;
import top.egon.skillopt.nacos.NacosSkillPackageRepository;
import top.egon.skillopt.record.NacosSkillLocation;
import top.egon.skillopt.record.SkillPackage;
import top.egon.skillopt.sandbox.SkillPackageShellOptions;
import top.egon.skillopt.sandbox.SkillPackageShellToolFactory;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ComplexSkillPackageSmokeTest {

  private static final String SKILL_NAME = "order-risk-audit";

  @TempDir
  Path tempDir;

  @Test
  void runsComplexNacosSkillPackageWithRegistryAndShellScript() throws Exception {
    Path fixtureRoot = fixtureRoot();
    FakeNacosSkillClient client = new FakeNacosSkillClient();
    client.downloadedZip = zipSkillDirectory(fixtureRoot);
    NacosSkillPackageRepository repository =
        new NacosSkillPackageRepository(client, tempDir.resolve(".skillopt/nacos-cache"));

    SkillPackage skillPackage =
        repository.download(new NacosSkillLocation("public", SKILL_NAME, "2026.06.08", "", false));

    SkillRegistry registry = skillPackage.createFileSystemSkillRegistry();
    assertThat(registry.contains(SKILL_NAME)).isTrue();
    assertThat(registry.readSkillContent(SKILL_NAME)).contains("订单风险审计", "scripts/audit_order.sh",
        "JSON 输出");

    ShellTool2 shellTool = new SkillPackageShellToolFactory(new SkillPackageShellOptions(5000L, 50,
        List.of("/bin/sh"), Map.of("SKILL_OPT_MODE", "smoke"))).createShellTool(skillPackage);
    RunnableConfig config = RunnableConfig.builder().threadId("complex-skill-smoke").build();
    shellTool.getSessionManager().initialize(config);
    try {
      String highRiskOutput = shellTool.getSessionManager()
          .executeCommand("sh scripts/audit_order.sh examples/high-risk-order.json", config)
          .getOutput();
      String normalOutput = shellTool.getSessionManager()
          .executeCommand("sh scripts/audit_order.sh examples/normal-order.json", config)
          .getOutput();

      assertThat(highRiskOutput).contains("\"decision\":\"REVIEW\"", "high_amount",
          "blocked_country", "manual_review_required");
      assertThat(normalOutput).contains("\"decision\":\"APPROVE\"", "\"riskScore\":12");
    } finally {
      shellTool.getSessionManager().cleanup(config);
    }

    String uploadedName =
        repository.upload(skillPackage, new NacosSkillLocation("public", SKILL_NAME, "", "", false),
            "2026.06.08-candidate", "complex skill smoke");

    assertThat(uploadedName).isEqualTo(SKILL_NAME);
    assertThat(client.uploadTargetVersion).isEqualTo("2026.06.08-candidate");
    assertThat(zipEntries(client.uploadZip)).contains(SKILL_NAME + "/SKILL.md",
        SKILL_NAME + "/scripts/audit_order.sh",
        SKILL_NAME + "/schemas/order-risk-output.schema.json",
        SKILL_NAME + "/examples/high-risk-order.json", SKILL_NAME + "/examples/normal-order.json");
  }

  private Path fixtureRoot() throws Exception {
    URL resource = getClass().getClassLoader().getResource("skills/" + SKILL_NAME + "/SKILL.md");
    assertThat(resource).as("missing complex skill fixture").isNotNull();
    return Path.of(resource.toURI()).getParent();
  }

  private byte[] zipSkillDirectory(Path skillRoot) throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(output);
        Stream<Path> paths = Files.walk(skillRoot)) {
      for (Path path : paths.sorted(Comparator.naturalOrder()).toList()) {
        if (Files.isDirectory(path)) {
          continue;
        }
        String entryName =
            SKILL_NAME + "/" + skillRoot.relativize(path).toString().replace('\\', '/');
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        Files.copy(path, zipOutputStream);
        zipOutputStream.closeEntry();
      }
    }
    return output.toByteArray();
  }

  private TreeSet<String> zipEntries(byte[] zipBytes) throws Exception {
    TreeSet<String> entries = new TreeSet<>();
    try (ZipInputStream zipInputStream =
        new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        entries.add(entry.getName());
        zipInputStream.closeEntry();
      }
    }
    return entries;
  }

  private static class FakeNacosSkillClient implements NacosSkillClient {

    private byte[] downloadedZip;
    private byte[] uploadZip;
    private String uploadTargetVersion;

    @Override
    public byte[] downloadSkillZip(String skillName) {
      return downloadedZip;
    }

    @Override
    public byte[] downloadSkillZipByVersion(String skillName, String version) {
      return downloadedZip;
    }

    @Override
    public byte[] downloadSkillZipByLabel(String skillName, String label) {
      return downloadedZip;
    }

    @Override
    public String uploadSkillFromZip(String namespaceId, byte[] zipBytes, boolean overwrite,
        String targetVersion, String commitMessage) {
      uploadZip = zipBytes;
      uploadTargetVersion = targetVersion;
      return SKILL_NAME;
    }
  }
}
