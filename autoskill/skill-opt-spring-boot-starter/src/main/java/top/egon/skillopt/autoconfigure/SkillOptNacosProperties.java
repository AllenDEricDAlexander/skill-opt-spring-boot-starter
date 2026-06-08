package top.egon.skillopt.autoconfigure;

import com.alibaba.nacos.api.PropertyKeyConst;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.Properties;

/**
 * Nacos AI skill package properties for SkillOpt ZIP transport.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "skill-opt.nacos")
public class SkillOptNacosProperties {

  private boolean enabled;

  private String serverAddr = "127.0.0.1:8848";

  private String namespaceId = "public";

  private String username = "";

  private String password = "";

  private String contextPath = "";

  private Path cacheDirectory = Path.of(".skillopt", "nacos-cache");

  /**
   * Creates the official Nacos client properties used by AI skill services.
   */
  public Properties toNacosProperties() {
    Properties properties = new Properties();
    setIfNotBlank(properties, PropertyKeyConst.SERVER_ADDR, serverAddr);
    setIfNotBlank(properties, PropertyKeyConst.NAMESPACE, namespaceId);
    setIfNotBlank(properties, PropertyKeyConst.USERNAME, username);
    setIfNotBlank(properties, PropertyKeyConst.PASSWORD, password);
    setIfNotBlank(properties, PropertyKeyConst.CONTEXT_PATH, contextPath);
    return properties;
  }

  private void setIfNotBlank(Properties properties, String key, String value) {
    if (value != null && !value.isBlank()) {
      properties.setProperty(key, value);
    }
  }
}
