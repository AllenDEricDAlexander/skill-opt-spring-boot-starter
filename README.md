# skill-opt-spring-boot-starter

`skill-opt-spring-boot-starter` 是一个用于优化 agent skill 文档的 Spring Boot starter 示例工程。它的目标不是训练模型，而是基于真实任务运行证据，对 `SKILL.md` 做受控的小范围编辑，并通过验证集分数决定是否导出新的最佳 skill。

## 项目结构

```text
autoskill/
├── skill-opt-spring-boot-starter/
└── skill-opt-spring-boot-starter-test/
```

- `autoskill/skill-auto-optimize-starter-plan.md`：最初的实现方案，说明为什么主路径基于 agent hook / tool interceptor，而不是 advisor 或业务注解。
- `autoskill/skill-opt-spring-boot-starter`：starter 模块，提供单轮 skill 优化流程、工具调用证据记录、受控 skill 编辑、验证闸门和 SQLite trace store 自动配置。
- `autoskill/skill-opt-spring-boot-starter-test`：可运行示例模块，使用 `poem-intent` skill 演示写诗任务的 Rollout -> Reflect -> Edit -> Gate -> Export 流程。

## 当前能力

- 运行一轮 skill 优化：先执行 reflection cases，再生成反思和有限编辑，最后用 validation cases 对比 baseline / candidate 分数。
- 只接受验证分数提升达到阈值的 candidate skill。
- 将 candidate skill 写入 `.skillopt/versions/<skill>/<round>/...`。
- 将通过验证的 skill 导出到 `.skillopt/best/<skill>/best_skill.md`。
- 默认不覆盖线上 `SKILL.md`，只有配置 `auto-overwrite-best-skill=true` 时才会覆盖原文件。
- 可通过 `ToolInterceptor` 记录 agent loop 内的工具调用证据，例如 `read_skill` 的请求、响应和错误。
- 可通过独立 SQLite JPA store 持久化 round、rollout、tool call、reflection 和 edit operation。

## 快速开始

进入 Maven 工程目录：

```bash
cd autoskill
```

运行 starter 模块测试：

```bash
./mvnw -pl skill-opt-spring-boot-starter test
```

运行示例模块测试：

```bash
./mvnw -pl skill-opt-spring-boot-starter-test test
```

启动示例应用：

```bash
export DASH_SCOPE_API_KEY=your_dashscope_api_key
./mvnw -pl skill-opt-spring-boot-starter-test spring-boot:run
```

访问示例接口：

```bash
curl http://localhost:18888/demo
```

接口会返回本轮是否接受 candidate、baseline / candidate 分数、candidate skill 路径、best skill 路径和 rollout 数量。

## 文档入口

- starter 使用方式见 `autoskill/skill-opt-spring-boot-starter/README.md`。
- 示例模块说明见 `autoskill/skill-opt-spring-boot-starter-test/README.md`。
