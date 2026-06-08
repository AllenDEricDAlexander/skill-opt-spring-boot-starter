package top.egon.skillopt.record;

import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents one local skill package including SKILL.md and its resource files.
 */
public record SkillPackage(String skillName, String version, String source, Path rootDirectory,
    Path skillFile) {

  public SkillPackage {
    if (skillName == null || skillName.isBlank()) {
      throw new IllegalArgumentException("skill name must not be blank");
    }
    version = version == null ? "" : version;
    source = source == null ? "" : source;
    Objects.requireNonNull(rootDirectory, "rootDirectory must not be null");
    Objects.requireNonNull(skillFile, "skillFile must not be null");
  }

  /**
   * Resolves a resource path inside the skill package directory.
   */
  public Path resolve(String relativePath) {
    if (relativePath == null || relativePath.isBlank()) {
      throw new IllegalArgumentException("relative path must not be blank");
    }
    Path resolved = rootDirectory.resolve(relativePath).normalize();
    if (!resolved.startsWith(rootDirectory.normalize())) {
      throw new SecurityException("path escapes skill package directory: " + relativePath);
    }
    return resolved;
  }

  /**
   * Creates the Spring AI Alibaba filesystem registry over the package parent directory.
   */
  public SkillRegistry createFileSystemSkillRegistry() {
    Path registryDirectory = rootDirectory.getParent();
    if (registryDirectory == null) {
      throw new IllegalStateException("skill package root must have a parent directory");
    }
    return FileSystemSkillRegistry.builder()
        .projectSkillsDirectory(registryDirectory.toAbsolutePath().toString()).build();
  }
}
