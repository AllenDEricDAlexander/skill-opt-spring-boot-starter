# skill-opt-spring-boot-starter-test

`skill-opt-spring-boot-starter-test` 是 starter 的可运行示例模块。它用一个 `poem-intent` skill 演示如何把 skill
文档优化流程接入真实 agent 任务。

## 这里做了什么

示例模块包含：

- `src/main/resources/skills/poem-intent/SKILL.md`：被优化的写诗 skill。
- `DemoBiz`：暴露 `/demo` 接口，运行一轮 `poem-intent` 优化。
- `PoemOutputEvaluator`：调用模型给 rollout 输出打分，分数范围是 `0.0` 到 `1.0`。
- `application.yaml`：配置 DashScope API Key、服务端口和 `.skillopt` 输出路径。
- `PoemOutputEvaluatorTest`：验证评分 prompt 会包含任务、期望行为、模型输出和 `read_skill` 工具调用证据。

`/demo` 的流程是：

1. 定位 `poem-intent/SKILL.md`。
2. 构造 `SingleFlowSkillOptimizer`。
3. 使用 `ReactAgent`、`SkillsAgentHook` 和 `SkillOptToolTraceInterceptor` 执行 reflection / validation rollout。
4. 评分模型根据输出和工具调用证据打分。
5. 反思模型总结 skill 问题。
6. 编辑规划器追加一条写诗流程规则。
7. 验证 candidate 分数是否超过 baseline，并按闸门结果导出 `best_skill.md`。

## 运行测试

在 `autoskill` 目录执行：

```bash
./mvnw -pl skill-opt-spring-boot-starter-test test
```

该测试不需要真实 DashScope 调用，使用本地 fake `ChatModel` 验证评分逻辑。

## 启动示例应用

示例应用需要 DashScope API Key：

```bash
export DASH_SCOPE_API_KEY=your_dashscope_api_key
./mvnw -pl skill-opt-spring-boot-starter-test spring-boot:run
```

默认端口是 `18888`。启动后访问：

```bash
curl http://localhost:18888/demo
curl http://localhost:18888/complex
```

接口返回示例：

```text
accepted=true, baselineScore=0.48, candidateScore=0.92, candidateSkill=..., bestSkill=..., rolloutCount=6
```

实际分数由模型评分结果决定，所以每次运行可能不同。

`/complex` 会运行 `order-risk-audit` 复杂 skill 示例：先把本地 skill 包按 Nacos ZIP 结构进入
`.skillopt/nacos-cache`，再通过 `FileSystemSkillRegistry` 读取 skill，并用 `ShellTool2` 在 skill 包根目录执行
`scripts/audit_order.sh`，返回高风险和普通订单的 JSON 判断。

## 输出结果

运行 `/demo` 后，工作目录下会生成 `.skillopt`：

```text
.skillopt/
├── versions/poem-intent/<round>/base_skill.md
├── versions/poem-intent/<round>/poem-intent/SKILL.md
├── best/poem-intent/SKILL.md
└── best/poem-intent/best_skill.md
```

- `base_skill.md`：本轮优化前的原始 skill。
- `SKILL.md`：本轮生成的 candidate skill。
- `best_skill.md`：通过验证闸门后导出的最佳 skill 内容快照。

当前示例配置 `auto-overwrite-best-skill: false`，所以不会直接覆盖 `src/main/resources/skills/poem-intent/SKILL.md`。

## Nacos 3.2 Skill ZIP

starter 已接入 Nacos 3.2.2 的官方 AI skill ZIP API：

- 下载：`AiService.downloadSkillZip(...)`、`downloadSkillZipByVersion(...)`、`downloadSkillZipByLabel(...)`。
- 上传：`AiMaintainerService.skill().uploadSkillFromZip(...)`。
- 本地运行：ZIP 会展开到 `.skillopt/nacos-cache`，再通过 Spring AI Alibaba `FileSystemSkillRegistry` 提供给
  `SkillsAgentHook` 的 `read_skill`。
- 脚本执行：带 `scripts/` 的 skill 可以用 `SkillPackageShellToolFactory` 创建官方 `ShellTool2` 和 `ShellToolAgentHook`
  ，工作目录限制在该 skill 包根目录。

配置示例：

```yaml
skill-opt:
  nacos:
    enabled: true
    server-addr: 127.0.0.1:8848
    namespace-id: public
    username: ${NACOS_USERNAME}
    password: ${NACOS_PASSWORD}
    cache-directory: ./.skillopt/nacos-cache
```

应用侧典型接法：

```java
SkillPackage skillPackage = nacosSkillPackageRepository.download(
    new NacosSkillLocation("public", "complex-skill", "1.0.0", "", false));
SkillRegistry registry = skillPackage.createFileSystemSkillRegistry();
SkillsAgentHook skillsHook = SkillsAgentHook.builder().skillRegistry(registry).build();
ShellToolAgentHook shellHook =
    new SkillPackageShellToolFactory(SkillPackageShellOptions.defaults()).createHook(skillPackage);
```

优化后如果验证通过，可以把 candidate 目录作为完整 skill 包上传；`SingleFlowSkillOptimizer`
会保留原包里的 `scripts/` 等资源文件，只覆盖 candidate 的 `SKILL.md`。

starter 模块里还提供了一个复杂 skill smoke test：`order-risk-audit`。它包含 `SKILL.md`、`scripts/audit_order.sh`、JSON
schema 和高/低风险订单样例，测试会模拟 Nacos ZIP 下载后通过 `FileSystemSkillRegistry` 读取 skill，并通过 `ShellTool2` 在
skill 包根目录执行脚本。

```bash
./mvnw -pl skill-opt-spring-boot-starter -Dtest=ComplexSkillPackageSmokeTest test
```

如果要连真实 Nacos 3.2.x 验证上传、发布、下载和脚本执行，可以启用默认跳过的 live 测试：

```bash
export NACOS_PASSWORD=your_nacos_password
./mvnw -pl skill-opt-spring-boot-starter \
  -Dtest=ComplexSkillNacosLiveIT \
  -Dskillopt.nacos.live=true \
  -Dskillopt.nacos.username=test \
  -Dskillopt.nacos.contextPath=/nacos \
  test
```

这个 live 测试会创建临时 skill、上传 ZIP、force publish、按版本下载，最后删除临时 skill。运行账号需要具备 Nacos AI skill
maintainer/admin 权限；只有普通登录权限时，上传接口会返回 `403 authorization failed`。

## 调整示例

可以从三个位置调整示例行为：

- 修改 `poem-intent/SKILL.md`，观察不同原始 skill 对优化结果的影响。
- 修改 `DemoBiz.reflectionCases()` 和 `DemoBiz.validationCases()`，调整反思集和验证集。
- 修改 `PoemOutputEvaluator`，调整评分标准。

如果要让通过验证的 candidate 自动覆盖原始 skill，可以在 `application.yaml` 中设置：

```yaml
skill-opt:
  sqlite:
    auto-overwrite-best-skill: true
```
