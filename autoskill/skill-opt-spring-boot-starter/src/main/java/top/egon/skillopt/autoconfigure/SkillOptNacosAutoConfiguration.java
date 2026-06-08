package top.egon.skillopt.autoconfigure;

import com.alibaba.nacos.api.ai.AiService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.maintainer.client.ai.AiMaintainerService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.egon.skillopt.nacos.NacosSkillClient;
import top.egon.skillopt.nacos.NacosSkillPackageRepository;
import top.egon.skillopt.nacos.SdkNacosSkillClient;

/**
 * Configures the optional Nacos-backed skill package repository for SkillOpt.
 */
@AutoConfiguration
@ConditionalOnClass({ AiService.class, AiMaintainerService.class })
@ConditionalOnProperty(prefix = "skill-opt.nacos", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(SkillOptNacosProperties.class)
public class SkillOptNacosAutoConfiguration {

  /**
   * Creates the official Nacos SDK adapter when the application has not supplied one.
   */
  @Bean
  @ConditionalOnMissingBean
  public NacosSkillClient skillOptNacosSkillClient(SkillOptNacosProperties properties)
      throws NacosException {
    return new SdkNacosSkillClient(properties.toNacosProperties());
  }

  /**
   * Creates a local ZIP cache that can be exposed through Spring AI Alibaba SkillRegistry.
   */
  @Bean
  @ConditionalOnMissingBean
  public NacosSkillPackageRepository nacosSkillPackageRepository(NacosSkillClient nacosSkillClient,
      SkillOptNacosProperties properties) {
    return new NacosSkillPackageRepository(nacosSkillClient, properties.getCacheDirectory());
  }
}
