package top.egon.skillopt.nacos;

import com.alibaba.nacos.api.ai.model.skills.SkillUtils;
import com.alibaba.nacos.api.exception.NacosException;
import top.egon.skillopt.record.NacosSkillLocation;
import top.egon.skillopt.record.SkillPackage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Downloads Nacos skill ZIP packages into a local Spring AI Alibaba filesystem registry.
 */
public class NacosSkillPackageRepository {

  private static final String SKILL_FILE_NAME = "SKILL.md";

  private final NacosSkillClient nacosSkillClient;

  private final Path cacheDirectory;

  public NacosSkillPackageRepository(NacosSkillClient nacosSkillClient, Path cacheDirectory) {
    this.nacosSkillClient =
        Objects.requireNonNull(nacosSkillClient, "nacosSkillClient must not be null");
    this.cacheDirectory = Objects.requireNonNull(cacheDirectory, "cacheDirectory must not be null");
  }

  /**
   * Downloads the selected Nacos skill ZIP and exposes it as a local skill package.
   */
  public SkillPackage download(NacosSkillLocation location) throws IOException, NacosException {
    Objects.requireNonNull(location, "location must not be null");
    byte[] zipBytes = downloadZip(location);
    SkillUtils.validateZipBytes(zipBytes);
    SkillUtils.validateZipEntryPaths(zipBytes);

    Path packageCacheDirectory = cacheDirectory.resolve(safeSegment(location.skillName()))
        .resolve(safeSegment(selector(location)));
    deleteDirectory(packageCacheDirectory);
    Files.createDirectories(packageCacheDirectory);
    unzip(zipBytes, packageCacheDirectory);

    Path rootDirectory = packageCacheDirectory.resolve(location.skillName()).normalize();
    Path skillFile = rootDirectory.resolve(SKILL_FILE_NAME);
    if (!Files.exists(skillFile)) {
      throw new IOException("downloaded skill package does not contain " + skillFile);
    }
    return new SkillPackage(location.skillName(), selectedVersion(location), "nacos", rootDirectory,
        skillFile);
  }

  /**
   * Uploads a local skill package as a Nacos skill ZIP candidate.
   */
  public String upload(SkillPackage skillPackage, NacosSkillLocation location, String targetVersion,
      String commitMessage) throws IOException, NacosException {
    Objects.requireNonNull(skillPackage, "skillPackage must not be null");
    Objects.requireNonNull(location, "location must not be null");
    byte[] zipBytes = zipSkillPackage(skillPackage);
    SkillUtils.validateZipBytes(zipBytes);
    SkillUtils.validateZipEntryPaths(zipBytes);
    return nacosSkillClient.uploadSkillFromZip(location.namespaceId(), zipBytes,
        location.overwrite(), targetVersion, commitMessage);
  }

  private byte[] downloadZip(NacosSkillLocation location) throws NacosException {
    if (!location.version().isBlank()) {
      return nacosSkillClient.downloadSkillZipByVersion(location.skillName(), location.version());
    }
    if (!location.label().isBlank()) {
      return nacosSkillClient.downloadSkillZipByLabel(location.skillName(), location.label());
    }
    return nacosSkillClient.downloadSkillZip(location.skillName());
  }

  private void unzip(byte[] zipBytes, Path targetDirectory) throws IOException {
    try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        SkillUtils.validatePathSafety(entry.getName());
        Path target = targetDirectory.resolve(entry.getName()).normalize();
        SkillUtils.validatePathContainment(targetDirectory, target);
        if (entry.isDirectory()) {
          Files.createDirectories(target);
        } else {
          Path parent = target.getParent();
          if (parent != null) {
            Files.createDirectories(parent);
          }
          Files.copy(zipInputStream, target);
        }
        zipInputStream.closeEntry();
      }
    }
  }

  private byte[] zipSkillPackage(SkillPackage skillPackage) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
        Stream<Path> paths = Files.walk(skillPackage.rootDirectory())) {
      for (Path path : paths.sorted(Comparator.naturalOrder()).toList()) {
        if (Files.isDirectory(path)) {
          continue;
        }
        String entryName = skillPackage.rootDirectory().getFileName() + "/"
            + skillPackage.rootDirectory().relativize(path).toString();
        SkillUtils.validatePathSafety(entryName);
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        Files.copy(path, zipOutputStream);
        zipOutputStream.closeEntry();
      }
    }
    return outputStream.toByteArray();
  }

  private void deleteDirectory(Path directory) throws IOException {
    if (!Files.exists(directory)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(directory)) {
      for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
        Files.delete(path);
      }
    }
  }

  private String selector(NacosSkillLocation location) {
    if (!location.version().isBlank()) {
      return location.version();
    }
    if (!location.label().isBlank()) {
      return "label-" + location.label();
    }
    return "latest";
  }

  private String selectedVersion(NacosSkillLocation location) {
    return location.version().isBlank() ? selector(location) : location.version();
  }

  private String safeSegment(String value) {
    return value.replaceAll("[^a-zA-Z0-9._-]", "_");
  }
}
