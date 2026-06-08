package top.egon.skillopt;

import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.egon.skillopt.nacos.NacosSkillClient;
import top.egon.skillopt.nacos.NacosSkillPackageRepository;
import top.egon.skillopt.record.NacosSkillLocation;
import top.egon.skillopt.record.SkillPackage;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class NacosSkillPackageRepositoryTest {

  @TempDir
  Path tempDir;

  @Test
  void downloadsSkillZipIntoFilesystemRegistry() throws Exception {
    FakeNacosSkillClient client = new FakeNacosSkillClient();
    client.downloadedZip = skillZip("complex-skill", """
        ---
        name: complex-skill
        description: 需要脚本辅助的复杂 skill
        ---

        # Complex Skill
        请使用 scripts/check.sh 校验输入。
        """, "scripts/check.sh", "echo ok\n");
    NacosSkillPackageRepository repository =
        new NacosSkillPackageRepository(client, tempDir.resolve(".skillopt/nacos-cache"));

    SkillPackage skillPackage =
        repository.download(new NacosSkillLocation("public", "complex-skill", "1.0.0", "", false));

    assertThat(skillPackage.rootDirectory()).isDirectory();
    assertThat(skillPackage.skillFile()).exists();
    assertThat(skillPackage.resolve("scripts/check.sh")).exists();
    SkillRegistry registry = skillPackage.createFileSystemSkillRegistry();
    assertThat(registry.contains("complex-skill")).isTrue();
    assertThat(registry.readSkillContent("complex-skill")).contains("Complex Skill");
    assertThat(client.downloadedByVersion).isEqualTo("complex-skill:1.0.0");
  }

  @Test
  void uploadsCandidateZipWithTargetVersionAndCommitMessage() throws Exception {
    FakeNacosSkillClient client = new FakeNacosSkillClient();
    NacosSkillPackageRepository repository =
        new NacosSkillPackageRepository(client, tempDir.resolve(".skillopt/nacos-cache"));
    Path skillRoot = tempDir.resolve("candidate/complex-skill");
    Files.createDirectories(skillRoot.resolve("scripts"));
    Files.writeString(skillRoot.resolve("SKILL.md"), """
        ---
        name: complex-skill
        description: 需要脚本辅助的复杂 skill
        ---

        # Candidate
        """, StandardCharsets.UTF_8);
    Files.writeString(skillRoot.resolve("scripts/check.sh"), "echo candidate\n",
        StandardCharsets.UTF_8);
    SkillPackage skillPackage = new SkillPackage("complex-skill", "1.0.1", "nacos", skillRoot,
        skillRoot.resolve("SKILL.md"));

    String uploadedName = repository.upload(skillPackage,
        new NacosSkillLocation("public", "complex-skill", "", "", false), "1.0.1",
        "skillopt candidate");

    assertThat(uploadedName).isEqualTo("complex-skill");
    assertThat(client.uploadNamespaceId).isEqualTo("public");
    assertThat(client.uploadOverwrite).isFalse();
    assertThat(client.uploadTargetVersion).isEqualTo("1.0.1");
    assertThat(client.uploadCommitMessage).isEqualTo("skillopt candidate");
    assertThat(client.uploadZip).isNotEmpty();
  }

  private byte[] skillZip(String skillName, String skillMd, String resourcePath, String resource)
      throws Exception {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(output)) {
      zip.putNextEntry(new ZipEntry(skillName + "/SKILL.md"));
      zip.write(skillMd.getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
      zip.putNextEntry(new ZipEntry(skillName + "/" + resourcePath));
      zip.write(resource.getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    }
    return output.toByteArray();
  }

  private static class FakeNacosSkillClient implements NacosSkillClient {

    private byte[] downloadedZip;
    private String downloadedByVersion;
    private byte[] uploadZip;
    private String uploadNamespaceId;
    private boolean uploadOverwrite;
    private String uploadTargetVersion;
    private String uploadCommitMessage;

    @Override
    public byte[] downloadSkillZip(String skillName) {
      return downloadedZip;
    }

    @Override
    public byte[] downloadSkillZipByVersion(String skillName, String version) {
      downloadedByVersion = skillName + ":" + version;
      return downloadedZip;
    }

    @Override
    public byte[] downloadSkillZipByLabel(String skillName, String label) {
      return downloadedZip;
    }

    @Override
    public String uploadSkillFromZip(String namespaceId, byte[] zipBytes, boolean overwrite,
        String targetVersion, String commitMessage) {
      uploadNamespaceId = namespaceId;
      uploadZip = zipBytes;
      uploadOverwrite = overwrite;
      uploadTargetVersion = targetVersion;
      uploadCommitMessage = commitMessage;
      return "complex-skill";
    }
  }
}
