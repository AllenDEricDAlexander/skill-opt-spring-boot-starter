package top.egon.skillopt.nacos;

import com.alibaba.nacos.api.exception.NacosException;

/**
 * Narrow adapter over the official Nacos AI skill ZIP APIs used by SkillOpt.
 */
public interface NacosSkillClient {

  byte[] downloadSkillZip(String skillName) throws NacosException;

  byte[] downloadSkillZipByVersion(String skillName, String version) throws NacosException;

  byte[] downloadSkillZipByLabel(String skillName, String label) throws NacosException;

  String uploadSkillFromZip(String namespaceId, byte[] zipBytes, boolean overwrite,
      String targetVersion, String commitMessage) throws NacosException;
}
