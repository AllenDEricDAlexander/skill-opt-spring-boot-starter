package top.egon.biz;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.agent.tools.ShellTool2;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;
import com.alibaba.nacos.api.exception.NacosException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import top.egon.skillopt.core.SingleFlowSkillOptimizer;
import top.egon.skillopt.enums.SkillOptCaseSplit;
import top.egon.skillopt.interceptoer.SkillOptToolTraceInterceptor;
import top.egon.skillopt.nacos.NacosSkillClient;
import top.egon.skillopt.nacos.NacosSkillPackageRepository;
import top.egon.skillopt.record.NacosSkillLocation;
import top.egon.skillopt.record.SkillEditOperation;
import top.egon.skillopt.record.SkillOptCase;
import top.egon.skillopt.record.SkillOptFlowOptions;
import top.egon.skillopt.record.SkillOptRolloutEvidence;
import top.egon.skillopt.record.SkillOptToolCall;
import top.egon.skillopt.record.SkillOptimizationResult;
import top.egon.skillopt.record.SkillPackage;
import top.egon.skillopt.record.SkillReflection;
import top.egon.skillopt.sandbox.SkillPackageShellOptions;
import top.egon.skillopt.sandbox.SkillPackageShellToolFactory;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
public class DemoBiz {
  private static final String POEM_INTENT_SKILL = "poem-intent";

  private static final String ORDER_RISK_AUDIT_SKILL = "order-risk-audit";

  private ChatModel chatModel;

  private PoemOutputEvaluator poemOutputEvaluator;

  @GetMapping("/demo")
  public String demo() throws Exception {
    Path skillFile = resolveSkillFile(POEM_INTENT_SKILL);
    SingleFlowSkillOptimizer optimizer =
        new SingleFlowSkillOptimizer(new SkillOptFlowOptions(resolveWorkspace(), 0.01, 64000),
            this::runPoemRollout, this::reflectPoemRollouts, this::planPoemSkillEdits);

    SkillOptimizationResult result =
        optimizer.optimize(POEM_INTENT_SKILL, skillFile, reflectionCases(), validationCases());
    return "accepted=" + result.accepted() + ", baselineScore=" + result.baselineScore()
        + ", candidateScore=" + result.candidateScore() + ", candidateSkill="
        + result.candidateSkillFile() + ", bestSkill=" + result.bestSkillFile() + ", rolloutCount="
        + result.rollouts().size();
  }

  @GetMapping("/complex")
  public String complex() throws Exception {
    Path skillFile = resolveSkillFile(ORDER_RISK_AUDIT_SKILL);
    SkillPackage localPackage = new SkillPackage(ORDER_RISK_AUDIT_SKILL, "demo", "filesystem",
        skillFile.getParent(), skillFile);
    LocalZipNacosSkillClient client = new LocalZipNacosSkillClient(zipSkillPackage(localPackage));
    NacosSkillPackageRepository repository =
        new NacosSkillPackageRepository(client, resolveWorkspace().resolve("nacos-cache"));
    SkillPackage skillPackage = repository
        .download(new NacosSkillLocation("public", ORDER_RISK_AUDIT_SKILL, "demo", "", false));
    SkillRegistry registry = skillPackage.createFileSystemSkillRegistry();
    ShellTool2 shellTool = new SkillPackageShellToolFactory(
        new SkillPackageShellOptions(5000L, 50, List.of("/bin/sh"), Map.of()))
        .createShellTool(skillPackage);
    RunnableConfig config = RunnableConfig.builder().threadId("complex-demo").build();

    shellTool.getSessionManager().initialize(config);
    try {
      String highRiskOutput = shellTool.getSessionManager()
          .executeCommand("sh scripts/audit_order.sh examples/high-risk-order.json", config)
          .getOutput();
      String normalOutput = shellTool.getSessionManager()
          .executeCommand("sh scripts/audit_order.sh examples/normal-order.json", config)
          .getOutput();
      return "skill=" + ORDER_RISK_AUDIT_SKILL + ", registry="
          + registry.contains(ORDER_RISK_AUDIT_SKILL) + ", highRisk=" + highRiskOutput + ", normal="
          + normalOutput;
    } finally {
      shellTool.getSessionManager().cleanup(config);
    }
  }

  // Rollout：固定目标模型带着当前 skill 跑任务，并记录输出、工具调用和分数。
  private SkillOptRolloutEvidence runPoemRollout(SkillOptCase skillCase, Path skillFile)
      throws GraphRunnerException {
    List<SkillOptToolCall> toolCalls = new CopyOnWriteArrayList<>();
    SkillRegistry registry = FileSystemSkillRegistry.builder()
        .projectSkillsDirectory(resolveSkillRoot(skillFile).toString()).build();
    SkillsAgentHook hook = SkillsAgentHook.builder().skillRegistry(registry).build();
    ReactAgent agent = ReactAgent.builder().name("poem_skillopt_agent").model(chatModel())
        .systemPrompt("你是诗歌创作助手。必须先调用 read_skill 读取 poem-intent skill，再完成用户任务。")
        .saver(new MemorySaver()).hooks(List.of(hook))
        .interceptors(List.of(new SkillOptToolTraceInterceptor(toolCalls))).build();
    AssistantMessage response = agent.call("请使用 poem-intent skill 完成任务：" + skillCase.input());
    String output = response.getText();
    double score = poemOutputEvaluator().score(skillCase, output, toolCalls);
    return SkillOptRolloutEvidence.success(skillCase.id(), skillCase.input(), skillFile, output,
        score, List.copyOf(toolCalls));
  }

  // Reflect：优化器模型从成功和失败案例中总结 skill 的共性问题。
  private SkillReflection reflectPoemRollouts(List<SkillOptRolloutEvidence> rollouts) {
    String reflection = chatModel().call("""
        你是 skill 文档优化器。请分析下面的写诗任务 rollout 证据，找出 skill 文档的共性问题。
        只输出问题摘要和需要补充的规则，不要直接重写完整 skill。

        证据：
        """ + summarizeRollouts(rollouts));
    return new SkillReflection(reflection, List.of("在写诗流程中补充用户意图识别、体裁选择、意象选择和语气控制。"));
  }

  // Edit：只做有限 add/delete/replace 修改，示例先追加一条稳定的写诗流程规则。
  private List<SkillEditOperation> planPoemSkillEdits(String skillContent,
      SkillReflection reflection) {
    if (skillContent.contains("先判断用户意图")) {
      return List.of();
    }
    String addition = """
        - 先判断用户意图，再选择意象、体裁和语气；输出时先用一句话说明识别到的意图，再写诗。
        """;
    if (skillContent.contains("## 写诗流程\n")) {
      return List.of(SkillEditOperation.add("## 写诗流程\n", addition));
    }
    return List.of(SkillEditOperation.add("", "\n## 写诗流程\n" + addition));
  }

  private String summarizeRollouts(List<SkillOptRolloutEvidence> rollouts) {
    StringBuilder builder = new StringBuilder();
    for (SkillOptRolloutEvidence rollout : rollouts) {
      builder.append("case=").append(rollout.caseId()).append(", score=").append(rollout.score())
          .append(", error=").append(rollout.error()).append(", toolCalls=")
          .append(rollout.toolCalls().size()).append(", output=")
          .append(truncate(rollout.output(), 300)).append('\n');
    }
    return builder.toString();
  }

  private String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value == null ? "" : value;
    }
    return value.substring(0, maxLength);
  }

  private List<SkillOptCase> reflectionCases() {
    return List.of(
        new SkillOptCase("reflect-001", "我想写一首表达离别但仍然温柔的诗", "需要识别离别和温柔语气",
            SkillOptCaseSplit.REFLECTION),
        new SkillOptCase("reflect-002", "写一首给深夜加班朋友的鼓励诗", "需要识别鼓励和陪伴意图",
            SkillOptCaseSplit.REFLECTION));
  }

  private List<SkillOptCase> validationCases() {
    return List.of(
        new SkillOptCase("valid-001", "帮我写一首春天里重逢的短诗", "需要识别重逢和春天意象", SkillOptCaseSplit.VALIDATION),
        new SkillOptCase("valid-002", "写一首适合生日贺卡的现代诗", "需要识别祝福和贺卡场景",
            SkillOptCaseSplit.VALIDATION));
  }

  private Path resolveSkillFile(String skillName) {
    Path userDir = Path.of(System.getProperty("user.dir"));
    List<Path> candidates = new ArrayList<>();
    candidates.add(userDir.resolve("skills").resolve(skillName).resolve("SKILL.md"));
    candidates
        .add(userDir.resolve("src/main/resources/skills").resolve(skillName).resolve("SKILL.md"));
    candidates.add(userDir.resolve("skill-opt-spring-boot-starter-test/src/main/resources/skills")
        .resolve(skillName).resolve("SKILL.md"));
    return candidates.stream().filter(Files::exists).min(Comparator.comparing(Path::toString))
        .orElseThrow(() -> new IllegalStateException("missing skill file: " + skillName));
  }

  private byte[] zipSkillPackage(SkillPackage skillPackage) throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
        Stream<Path> paths = Files.walk(skillPackage.rootDirectory())) {
      for (Path path : paths.sorted(Comparator.naturalOrder()).toList()) {
        if (Files.isDirectory(path)) {
          continue;
        }
        String entryName = skillPackage.skillName() + "/"
            + skillPackage.rootDirectory().relativize(path).toString().replace('\\', '/');
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        Files.copy(path, zipOutputStream);
        zipOutputStream.closeEntry();
      }
    }
    return outputStream.toByteArray();
  }

  private Path resolveSkillRoot(Path skillFile) {
    Path skillDirectory = skillFile.getParent();
    if (skillDirectory == null || skillDirectory.getParent() == null) {
      throw new IllegalStateException("invalid skill file path: " + skillFile);
    }
    return skillDirectory.getParent();
  }

  private Path resolveWorkspace() throws Exception {
    String configuredWorkspace = System.getProperty("skillopt.demo.workspace");
    Path workspace = configuredWorkspace == null || configuredWorkspace.isBlank()
        ? Path.of(System.getProperty("user.dir")).resolve(".skillopt")
        : Path.of(configuredWorkspace);
    Files.createDirectories(workspace);
    return workspace;
  }

  private ChatModel chatModel() {
    if (chatModel == null) {
      DashScopeApi dashScopeApi = DashScopeApi.builder().build();
      chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
    }
    return chatModel;
  }

  private PoemOutputEvaluator poemOutputEvaluator() {
    if (poemOutputEvaluator == null) {
      poemOutputEvaluator = new PoemOutputEvaluator(chatModel());
    }
    return poemOutputEvaluator;
  }

  private static class LocalZipNacosSkillClient implements NacosSkillClient {

    private final byte[] zipBytes;

    private LocalZipNacosSkillClient(byte[] zipBytes) {
      this.zipBytes = zipBytes;
    }

    @Override
    public byte[] downloadSkillZip(String skillName) throws NacosException {
      return zipBytes;
    }

    @Override
    public byte[] downloadSkillZipByVersion(String skillName, String version)
        throws NacosException {
      return zipBytes;
    }

    @Override
    public byte[] downloadSkillZipByLabel(String skillName, String label) throws NacosException {
      return zipBytes;
    }

    @Override
    public String uploadSkillFromZip(String namespaceId, byte[] zipBytes, boolean overwrite,
        String targetVersion, String commitMessage) throws NacosException {
      return ORDER_RISK_AUDIT_SKILL;
    }
  }
}
