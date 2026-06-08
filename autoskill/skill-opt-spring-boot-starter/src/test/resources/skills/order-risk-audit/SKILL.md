---
name: order-risk-audit
description: 审计订单支付风险，结合规则说明、样例数据、JSON schema 和本地脚本给出是否需要人工复核的判断。
---

# 订单风险审计

你是订单风险审计助手。处理用户给出的订单 JSON 时，必须先阅读本 skill，再按下面流程执行。

## 适用场景

- 用户要求判断订单是否需要人工复核。
- 输入包含订单金额、收货国家、设备状态、客户等级等结构化字段。
- 需要输出可被系统解析的 JSON 结论。

## 资源文件

- `scripts/audit_order.sh`：本地规则脚本，接收一个订单 JSON 文件路径，输出风险判断 JSON。
- `schemas/order-risk-output.schema.json`：输出结构约束。
- `examples/high-risk-order.json`：高风险订单样例。
- `examples/normal-order.json`：低风险订单样例。

## 执行流程

1. 确认用户输入是订单 JSON，或确认用户引用了本 skill 包中的样例文件。
2. 使用 shell 工具在 skill 包根目录执行 `sh scripts/audit_order.sh <order-json-file>`。
3. 读取脚本输出的 JSON，并检查字段是否符合 `schemas/order-risk-output.schema.json`。
4. 用中文解释触发原因，但最终必须保留脚本输出中的 `decision`、`riskScore` 和 `reasons`。

## 风险规则

- 金额大于等于 `10000` 时增加高金额风险。
- 收货国家为 `IR`、`KP`、`SY` 时增加受限国家风险。
- `newDevice` 为 `true` 时增加新设备风险。
- 风险分大于等于 `70` 时输出 `REVIEW`，否则输出 `APPROVE`。

## JSON 输出

脚本输出必须是单行 JSON，字段含义如下：

- `orderId`：订单编号。
- `decision`：`REVIEW` 或 `APPROVE`。
- `riskScore`：整数风险分。
- `reasons`：触发的原因列表。
