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
```

接口返回示例：

```text
accepted=true, baselineScore=0.48, candidateScore=0.92, candidateSkill=..., bestSkill=..., rolloutCount=6
```

实际分数由模型评分结果决定，所以每次运行可能不同。

## 输出结果

运行 `/demo` 后，工作目录下会生成 `.skillopt`：

```text
.skillopt/
├── versions/poem-intent/<round>/poem-intent/base_skill.md
├── versions/poem-intent/<round>/poem-intent/SKILL.md
└── best/poem-intent/best_skill.md
```

- `base_skill.md`：本轮优化前的原始 skill。
- `SKILL.md`：本轮生成的 candidate skill。
- `best_skill.md`：通过验证闸门后导出的最佳 skill。

当前示例配置 `auto-overwrite-best-skill: false`，所以不会直接覆盖 `src/main/resources/skills/poem-intent/SKILL.md`。

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
