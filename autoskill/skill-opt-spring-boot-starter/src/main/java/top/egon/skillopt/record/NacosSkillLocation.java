package top.egon.skillopt.record;

/**
 * Identifies a Nacos-managed skill package and the version selector used by SkillOpt.
 */
public record NacosSkillLocation(String namespaceId, String skillName, String version, String label,
    boolean overwrite) {

  public NacosSkillLocation {
    namespaceId = namespaceId == null || namespaceId.isBlank() ? "public" : namespaceId;
    if (skillName == null || skillName.isBlank()) {
      throw new IllegalArgumentException("skill name must not be blank");
    }
    version = version == null ? "" : version;
    label = label == null ? "" : label;
  }
}
