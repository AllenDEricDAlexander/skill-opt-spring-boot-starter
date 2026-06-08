package top.egon.skillopt.nacos;

import com.alibaba.nacos.api.ai.AiFactory;
import com.alibaba.nacos.api.ai.AiService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.maintainer.client.ai.AiMaintainerFactory;
import com.alibaba.nacos.maintainer.client.ai.AiMaintainerService;

import java.util.Objects;
import java.util.Properties;

/**
 * Uses the official Nacos 3.2 AI client and maintainer client for skill ZIP transport.
 */
public class SdkNacosSkillClient implements NacosSkillClient {

  private final AiService aiService;

  private final AiMaintainerService aiMaintainerService;

  public SdkNacosSkillClient(Properties properties) throws NacosException {
    this(AiFactory.createAiService(properties),
        AiMaintainerFactory.createAiMaintainerService(properties));
  }

  public SdkNacosSkillClient(AiService aiService, AiMaintainerService aiMaintainerService) {
    this.aiService = Objects.requireNonNull(aiService, "aiService must not be null");
    this.aiMaintainerService =
        Objects.requireNonNull(aiMaintainerService, "aiMaintainerService must not be null");
  }

  @Override
  public byte[] downloadSkillZip(String skillName) throws NacosException {
    return aiService.downloadSkillZip(skillName);
  }

  @Override
  public byte[] downloadSkillZipByVersion(String skillName, String version) throws NacosException {
    return aiService.downloadSkillZipByVersion(skillName, version);
  }

  @Override
  public byte[] downloadSkillZipByLabel(String skillName, String label) throws NacosException {
    return aiService.downloadSkillZipByLabel(skillName, label);
  }

  @Override
  public String uploadSkillFromZip(String namespaceId, byte[] zipBytes, boolean overwrite,
      String targetVersion, String commitMessage) throws NacosException {
    return aiMaintainerService.skill().uploadSkillFromZip(namespaceId, zipBytes, overwrite,
        targetVersion, commitMessage);
  }
}
