# skill-opt-spring-boot-starter

`skill-opt-spring-boot-starter` 提供一套面向 skill 文档优化的 Spring Boot starter 能力。它通过任务运行证据、反思、受控编辑和验证闸门来优化
`SKILL.md`，不参与模型训练，也不会默认改写线上 skill 文件。

## 核心流程

`SingleFlowSkillOptimizer` 执行一轮完整优化：

1. 使用当前 `SKILL.md` 跑 reflection cases，收集输出、分数、工具调用和错误。
2. 调用 `SkillReflector` 总结 skill 的共性问题。
3. 调用 `SkillEditPlanner` 生成有限的 `add` / `delete` / `replace` 编辑。
4. 使用 `BoundedSkillEditor` 应用编辑，并校验目标片段存在、结果大小不超过限制。
5. 分别用原始 skill 和 candidate skill 跑 validation cases。
6. candidate 分数高于 baseline，且提升达到 `minValidationImprovement` 时才导出 `best_skill.md`。

## 引入依赖

在业务工程中引入 starter：

```xml

<dependency>
    <groupId>top.egon</groupId>
    <artifactId>skill-opt-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## 基本用法

先准备一个可读写的 skill 文件，例如：

```text
skills/poem-intent/SKILL.md
```

然后创建优化器。`SkillRolloutRunner`、`SkillReflector`、`SkillEditPlanner` 由业务侧提供，因为不同 skill 的任务执行方式、评分方式和编辑策略不同。

```java
Path workspace = Path.of(".skillopt");
Path skillFile = Path.of("skills/poem-intent/SKILL.md");

SingleFlowSkillOptimizer optimizer = new SingleFlowSkillOptimizer(
        new SkillOptFlowOptions(workspace, 0.01, 64000),
        rolloutRunner,
        reflector,
        editPlanner
);

SkillOptimizationResult result = optimizer.optimize(
        "poem-intent",
        skillFile,
        reflectionCases,
        validationCases
);
```

返回结果包含：

- `accepted`：candidate 是否通过验证闸门。
- `baselineScore`：原始 skill 在 validation cases 上的平均分。
- `candidateScore`：candidate skill 在 validation cases 上的平均分。
- `candidateSkillFile`：candidate skill 输出路径。
- `bestSkillFile`：通过验证后导出的最佳 skill 路径；未通过时为 `null`。
- `reflection`：本轮反思结果。
- `rollouts`：reflection、baseline validation、candidate validation 的运行证据。

## 配置项

SQLite 自动配置使用 `skill-opt.sqlite` 前缀：

```yaml
skill-opt:
  sqlite:
    database: ./.skillopt/skillopt.db
    workspace: ./.skillopt
    versions-directory: ./.skillopt/versions
    best-directory: ./.skillopt/best
    auto-overwrite-best-skill: false
```

- `database`：SQLite 数据库文件路径。
- `workspace`：默认工作目录。
- `versions-directory`：candidate skill 和 base backup 输出目录。
- `best-directory`：通过验证的 `best_skill.md` 输出目录。
- `auto-overwrite-best-skill`：是否在 candidate 通过验证后覆盖原始 `SKILL.md`。默认 `false`。

使用配置生成 flow options：

```java
SingleFlowSkillOptimizer optimizer = new SingleFlowSkillOptimizer(
        properties.toFlowOptions(0.01, 64000),
        rolloutRunner,
        reflector,
        editPlanner,
        traceStore
);
```

## 工具调用证据

`SkillOptToolTraceInterceptor` 用于记录 agent loop 内的工具调用。它不会改变工具执行行为，只把工具名称、参数、结果和错误写入传入的列表。

```java
List<SkillOptToolCall> toolCalls = new CopyOnWriteArrayList<>();

ReactAgent agent = ReactAgent.builder()
        .name("poem_skillopt_agent")
        .model(chatModel)
        .hooks(List.of(skillsHook))
        .interceptors(List.of(new SkillOptToolTraceInterceptor(toolCalls)))
        .build();
```

业务侧可以在 rollout 评分时检查是否调用过 `read_skill`，以及 skill 内容是否被正确读取。

## 输出目录

默认情况下，一轮优化会写入：

```text
.skillopt/
├── versions/<skill>/<round>/<skill>/base_skill.md
├── versions/<skill>/<round>/<skill>/SKILL.md
└── best/<skill>/best_skill.md
```

`base_skill.md` 保存本轮优化前的原始 skill。`SKILL.md` 是 candidate。`best_skill.md` 只有 candidate 通过验证闸门时才会生成或更新。

## 测试

在 `autoskill` 目录执行：

```bash
./mvnw -pl skill-opt-spring-boot-starter test
```

测试覆盖：

- 受控编辑器的 add / delete / replace 行为。
- 单轮优化接受 candidate 并导出 best skill。
- SQLite 自动配置、属性绑定和 trace store 持久化。
- 工具调用追踪拦截器记录 request / response。
