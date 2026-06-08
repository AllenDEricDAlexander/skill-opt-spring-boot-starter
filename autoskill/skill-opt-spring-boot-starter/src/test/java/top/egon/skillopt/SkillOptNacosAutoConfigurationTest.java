package top.egon.skillopt;

import com.alibaba.nacos.api.PropertyKeyConst;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import top.egon.skillopt.autoconfigure.SkillOptNacosAutoConfiguration;
import top.egon.skillopt.autoconfigure.SkillOptNacosProperties;
import top.egon.skillopt.nacos.NacosSkillClient;
import top.egon.skillopt.nacos.NacosSkillPackageRepository;

import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class SkillOptNacosAutoConfigurationTest {

  @TempDir
  Path tempDir;

  @Test
  void createsPackageRepositoryWhenNacosIntegrationIsEnabled() {
    Path cacheDirectory = tempDir.resolve(".skillopt/nacos-cache");
    ApplicationContextRunner contextRunner =
        new ApplicationContextRunner().withUserConfiguration(SkillOptNacosAutoConfiguration.class)
            .withBean(NacosSkillClient.class, FakeNacosSkillClient::new).withPropertyValues(
                "skill-opt.nacos.enabled=true", "skill-opt.nacos.server-addr=127.0.0.1:8848",
                "skill-opt.nacos.namespace-id=public", "skill-opt.nacos.username=test",
                "skill-opt.nacos.password=secret", "skill-opt.nacos.context-path=/nacos",
                "skill-opt.nacos.cache-directory=" + cacheDirectory);

    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(SkillOptNacosProperties.class);
      assertThat(context).hasSingleBean(NacosSkillPackageRepository.class);

      SkillOptNacosProperties properties = context.getBean(SkillOptNacosProperties.class);
      assertThat(properties.getCacheDirectory()).isEqualTo(cacheDirectory);
      Properties nacosProperties = properties.toNacosProperties();
      assertThat(nacosProperties.getProperty(PropertyKeyConst.SERVER_ADDR))
          .isEqualTo("127.0.0.1:8848");
      assertThat(nacosProperties.getProperty(PropertyKeyConst.NAMESPACE)).isEqualTo("public");
      assertThat(nacosProperties.getProperty(PropertyKeyConst.USERNAME)).isEqualTo("test");
      assertThat(nacosProperties.getProperty(PropertyKeyConst.PASSWORD)).isEqualTo("secret");
      assertThat(nacosProperties.getProperty(PropertyKeyConst.CONTEXT_PATH)).isEqualTo("/nacos");
    });
  }

  @Test
  void doesNotCreateRepositoryByDefault() {
    ApplicationContextRunner contextRunner =
        new ApplicationContextRunner().withUserConfiguration(SkillOptNacosAutoConfiguration.class);

    contextRunner
        .run(context -> assertThat(context).doesNotHaveBean(NacosSkillPackageRepository.class));
  }

  private static class FakeNacosSkillClient implements NacosSkillClient {

    @Override
    public byte[] downloadSkillZip(String skillName) {
      return new byte[0];
    }

    @Override
    public byte[] downloadSkillZipByVersion(String skillName, String version) {
      return new byte[0];
    }

    @Override
    public byte[] downloadSkillZipByLabel(String skillName, String label) {
      return new byte[0];
    }

    @Override
    public String uploadSkillFromZip(String namespaceId, byte[] zipBytes, boolean overwrite,
        String targetVersion, String commitMessage) {
      return "";
    }
  }
}
