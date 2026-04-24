# 多 Agent 协作开发工作流（文档中心 + 代码隔离版）

> 本文档描述如何使用 Kimi CLI 的 Agent 工具，基于**文档中心化、代码物理隔离**的架构，完成从需求到交付的全流程。
>
> 核心设计：
> - 所有非代码产出（PRD、API、测试）集中到独立的文档目录（与代码仓库完全分离）
> - 每个 Agent 只能读写自己的代码目录，前后端代码互不暴露
> - 工作目录不由配置文件硬编码，由项目经理在调度时注入
> - 完全通用，不绑定任何项目结构或技术栈
> - 支持一次迭代并行开发多个后端服务

---

## 目录

- [准备工作](#准备工作)
- [阶段零：交互式参数收集](#阶段零交互式参数收集)
- [阶段一：产品经理调研与出方案](#阶段一产品经理调研与出方案)
- [阶段二：前后端接口对齐](#阶段二前后端接口对齐)
- [阶段三：同步开发](#阶段三同步开发)
- [阶段四：联调自测](#阶段四联调自测)
- [阶段五：QA 测试](#阶段五qa-测试)
- [阶段六：产品经理验收](#阶段六产品经理验收)
- [方式 B：并行多服务开发](#方式-b并行多服务开发)
- [关键技巧](#关键技巧)

---

## 准备工作

### 1. Agent 配置位置

Agent 配置文件（`.yaml` + `system_prompt.md`）集中放在本目录 `.kimi/agents/` 下，**不硬编码任何项目路径**，作为通用模板复用。

### 2. 启动主 Agent（项目经理角色）

```bash
# 进入任意工作目录均可，主 Agent 不直接操作代码
kimi --agent-file /path/to/pm-agent.yaml
```

启动后，PM Agent 会自动通过交互式问答收集参数，并自动创建文档中心目录。

---

## 阶段零：交互式参数收集

启动主 Agent 后，PM Agent 会自动通过 `AskUserQuestion` 工具向你收集本次迭代的参数：

**Step 1**: 选择文档中心目录
- 使用当前目录下的子目录（如 `./docs-center/`）
- 让我自己输入路径

**Step 2**: 选择涉及的后端服务及代码目录（可多选）
- 1 个后端服务 → 追问代码目录
- 2 个后端服务 → 追问各自代码目录
- 3 个或更多 → 追问各自代码目录
- 不涉及后端（纯前端迭代）

**Step 3**: 选择前端项目代码目录（如有）
- 1 个前端项目 → 追问代码目录
- 不涉及前端（纯后端迭代）

**Step 4**: PM Agent 构建完整路径后向你确认

```
本次迭代配置：
- 文档中心：{用户指定的文档中心路径}
- 后端服务：
  - {服务A} → {服务A代码路径}
  - {服务B} → {服务B代码路径}
- 前端项目：{前端代码路径}
```

**Step 5**: PM Agent 自动创建文档中心目录结构，然后开始阶段一。

---

> 如果你想跳过交互，直接手动声明参数：
> ```
> 本次迭代参数：
> - 文档中心：/path/to/docs-center/
> - 后端：order-service → /path/to/backend/order-service/
> - 前端：/path/to/frontend/
> ```

---

## 阶段一：产品经理调研出方案

### 目标
让 PM Agent 阅读现有文档和代码，输出新产品 PRD 到文档中心。

### 操作步骤

#### Step 1.1：现有系统调研（可选）

```
Agent 工具调用：
- description: "探索现有系统架构"
- subagent_type: "explore"
- prompt: |
    你是产品经理的调研助手。

    ## 本次迭代参数
    - 文档中心：{DOC_CENTER_DIR}

    ## 任务
    请探索以下项目，了解系统现状：
    1. {后端服务A代码目录} — 后端架构、技术栈、现有能力
    2. {前端代码目录} — 前端架构、页面结构（如有前端）

    输出结构化调研报告，保存到 {DOC_CENTER_DIR}/design/existing-system-report.md
```

#### Step 1.2：输出 PRD

```
Agent 工具调用：
- description: "产品经理出PRD"
- subagent_type: "plan"
- prompt: |
    你是产品经理。

    ## 本次迭代参数
    - 文档中心：{DOC_CENTER_DIR}
    - 涉及后端服务：{服务列表}
    - 前端项目：{如有}

    ## 任务
    请基于现有系统规划新功能。

    工作步骤：
    1. 阅读 {DOC_CENTER_DIR}/design/existing-system-report.md（如果有）
    2. 阅读各项目的 README
    3. 如需深入代码调研，使用 explore 子 Agent（只读）
    4. 输出 PRD 到 {DOC_CENTER_DIR}/design/PRD.md

    PRD 必须包含：
    - 背景与目标
    - 用户故事（至少 3 个）
    - 功能列表（Must/Should/Nice）
    - 业务流程描述
    - 新页面/模块清单
    - 涉及的后端服务及交互点
    - 非功能性需求
```

**输出物**：`{DOC_CENTER_DIR}/design/PRD.md` ✅

---

## 阶段二：前后端接口对齐

### 目标
FE/BE Agent 阅读 PRD 后，共同产出按服务拆分的接口契约文档。

### 操作步骤

#### Step 2.1：并行启动前后端阅读 PRD

```
Agent 工具调用 1（后台）：
- description: "后端阅读PRD出接口草案"
- subagent_type: "explore"
- run_in_background: true
- prompt: |
    你是后端工程师。

    ## 本次迭代参数
    - 你的代码工作目录：{后端服务A代码目录}
    - 文档中心：{DOC_CENTER_DIR}

    ## 任务
    1. 阅读 {DOC_CENTER_DIR}/design/PRD.md
    2. 探索你的代码工作目录，了解现有架构
    3. 根据 PRD 中涉及你的部分，构思需要实现的 API 接口
    4. 输出接口草案（只读总结，不写文件）

Agent 工具调用 2（后台）：
- description: "前端阅读PRD出接口需求"
- subagent_type: "explore"
- run_in_background: true
- prompt: |
    你是前端工程师。

    ## 本次迭代参数
    - 你的代码工作目录：{前端代码目录}
    - 文档中心：{DOC_CENTER_DIR}

    ## 任务
    1. 阅读 {DOC_CENTER_DIR}/design/PRD.md
    2. 探索前端代码目录，了解现有 API 封装风格
    3. 根据 PRD 中的页面需求，列出需要从后端获取的数据和接口
    4. 输出接口需求草案（只读总结，不写文件）
```

#### Step 2.2：主 Agent 汇总产出契约

等两个 Agent 都完成后，启动接口设计 Agent：

```
Agent 工具调用：
- description: "设计接口契约"
- subagent_type: "plan"
- prompt: |
    你是技术负责人。

    ## 本次迭代参数
    - 文档中心：{DOC_CENTER_DIR}

    ## 任务
    请阅读 PRD 和前后端 Agent 的接口草案，
    输出接口契约到 {DOC_CENTER_DIR}/design/api-contracts/{服务名}-api.md

    每个接口必须包含：Method、Path、请求参数（类型/必填/示例）、响应格式、错误码。
    写明通用约定（分页、时间格式、错误结构）。
```

**输出物**：`{DOC_CENTER_DIR}/design/api-contracts/{服务名}-api.md` ✅

---

## 阶段三：同步开发

### 目标
前后端按契约各自独立开发，代码互不可见。

```
Agent 工具调用 1（后台，后端开发）：
- description: "后端开发"
- subagent_type: "coder"
- run_in_background: true
- prompt: |
    你是后端工程师。

    ## 本次迭代参数（严格遵守）
    - 你的代码工作目录：{后端服务A代码目录}
    - 文档中心：{DOC_CENTER_DIR}
    - API 契约：{DOC_CENTER_DIR}/design/api-contracts/{服务名}-api.md
    - PRD：{DOC_CENTER_DIR}/design/PRD.md

    ## 约束
    1. 所有文件写入必须使用以 {后端服务A代码目录} 开头的绝对路径
    2. 禁止访问其他服务目录、前端目录
    3. 严格按契约实现，契约模糊处记录但不擅自修改
    4. 每完成一个接口标记 // API: ready
    5. 完成后执行项目编译/测试命令验证通过

Agent 工具调用 2（后台，前端开发）：
- description: "前端开发"
- subagent_type: "coder"
- run_in_background: true
- prompt: |
    你是前端工程师。

    ## 本次迭代参数（严格遵守）
    - 你的代码工作目录：{前端代码目录}
    - 文档中心：{DOC_CENTER_DIR}
    - API 契约目录：{DOC_CENTER_DIR}/design/api-contracts/
    - PRD：{DOC_CENTER_DIR}/design/PRD.md

    ## 约束
    1. 所有文件写入必须使用以 {前端代码目录} 开头的绝对路径
    2. 禁止访问所有后端代码目录
    3. 在 src/api/ 下为每个后端服务创建独立的 API 封装文件
    4. 严格按契约中的字段名和类型实现类型定义
    5. 处理加载态、空态、错误态
    6. 完成后执行项目构建命令验证通过
```

**输出物**：后端代码、前端代码 ✅

---

## 阶段四：联调自测

### 目标
前端实际调用后端接口，记录参数不匹配问题。

```
Agent 工具调用（前端 Agent）：
- description: "前后端联调"
- subagent_type: "coder"
- prompt: |
    你是前端工程师，进入联调阶段。

    ## 本次迭代参数
    - 你的代码工作目录：{前端代码目录}
    - 后端服务地址：（由项目经理提供，如 http://localhost:8080）
    - 文档中心：{DOC_CENTER_DIR}

    ## 任务
    1. 确保后端服务已启动（如未启动，告知项目经理）
    2. 实际调用后端 API，验证每个接口：
       - 请求参数是否正确传递
       - 响应数据结构是否与契约一致
       - 错误处理是否正常
       - 边界条件（空数据、大数据量）
    3. 每发现一个参数不匹配，记录：
       - 哪个接口
       - 前端传了什么 / 后端期望什么
       - 后端返回了什么 / 前端期望什么
    4. 输出联调报告到 {DOC_CENTER_DIR}/docs/integration-report.md

    前端侧问题自己修复，后端契约未兑现的问题记录等待修复。
```

根据报告，启动后端 Agent 修复：

```
Agent 工具调用：
- description: "修复后端接口问题"
- subagent_type: "coder"
- prompt: |
    你是后端工程师。

    ## 本次迭代参数
    - 你的代码工作目录：{后端服务A代码目录}
    - 联调报告：{DOC_CENTER_DIR}/docs/integration-report.md

    ## 任务
    请阅读联调报告，修复所有标记为"后端问题"的项。
    修复后重新编译/测试验证。
```

**输出物**：`{DOC_CENTER_DIR}/docs/integration-report.md` ✅

---

## 阶段五：QA 测试

### 目标
系统性测试。

```
Agent 工具调用：
- description: "QA系统测试"
- subagent_type: "coder"
- prompt: |
    你是 QA 工程师。

    ## 本次迭代参数
    - 文档中心：{DOC_CENTER_DIR}

    ## 任务
    1. 阅读 {DOC_CENTER_DIR}/design/PRD.md
    2. 阅读 {DOC_CENTER_DIR}/design/api-contracts/ 下所有契约
    3. 执行功能测试、接口测试、边界测试、回归测试
    4. 输出报告到 {DOC_CENTER_DIR}/docs/qa-reports/test-report-YYYYMMDD.md
```

**输出物**：测试报告 ✅

---

## 阶段六：产品经理验收

### 目标
PM 对照 PRD 验收最终成果。

```
Agent 工具调用：
- description: "产品经理验收"
- subagent_type: "explore"
- prompt: |
    你是产品经理。

    ## 本次迭代参数
    - 文档中心：{DOC_CENTER_DIR}

    ## 任务
    1. 阅读 {DOC_CENTER_DIR}/design/PRD.md
    2. 阅读 {DOC_CENTER_DIR}/docs/qa-reports/test-report-*.md
    3. 如有必要，阅读关键代码确认实现逻辑（只读）
    4. 输出验收报告到 {DOC_CENTER_DIR}/acceptance-report.md
```

**输出物**：验收报告 ✅

---

## 方式 B：并行多服务开发

当一次迭代涉及**多个后端服务**时，采用并行方式。

### 前提

- 阶段二（接口对齐）时，**所有涉及服务的契约必须已经确定**
- 服务之间通过契约解耦

### 并行调度示例

本次迭代同时修改 `service-a` 和 `service-b`：

```
Agent 工具调用 1（后台，service-a）：
- description: "开发service-a"
- subagent_type: "coder"
- run_in_background: true
- prompt: |
    你是 service-a 后端工程师。
    - 代码工作目录：{service-a代码目录}
    - 契约：{DOC_CENTER_DIR}/design/api-contracts/service-a-api.md
    ...（开发任务）

Agent 工具调用 2（后台，service-b）：
- description: "开发service-b"
- subagent_type: "coder"
- run_in_background: true
- prompt: |
    你是 service-b 后端工程师。
    - 代码工作目录：{service-b代码目录}
    - 契约：{DOC_CENTER_DIR}/design/api-contracts/service-b-api.md
    ...（开发任务）

Agent 工具调用 3（后台，前端）：
- description: "前端开发"
- subagent_type: "coder"
- run_in_background: true
- prompt: |
    你是前端工程师。
    - 代码工作目录：{前端代码目录}
    - 契约目录：{DOC_CENTER_DIR}/design/api-contracts/
    ...（开发任务）
```

三个 Agent 同时后台运行，完成后系统分别通知。

### 联调顺序

并行开发完成后，按依赖顺序逐个联调：
1. 先联调基础服务
2. 再联调依赖服务
3. 前端最后统一验证

如果服务之间无强依赖，可同时进行联调。

### 多服务契约文件命名

```
design/api-contracts/
├── service-a-api.md
├── service-b-api.md
└── ...
```

---

## 关键技巧

### 1. 路径注入是唯一的上下文传递方式

所有 Agent 之间的信息传递依赖文件。主 Agent 在每次调度时把路径注入 prompt，子 Agent 通过绝对路径读写文件。

### 2. 并行 vs 串行

| 阶段 | 策略 | 原因 |
|------|------|------|
| 读 PRD | **并行** | 前后端互不依赖 |
| 接口设计 | **串行** | 需等双方都读完，由主 Agent 汇总 |
| 单服务前后端开发 | **并行** | 有契约后可独立 |
| 多服务开发 | **并行（方式 B）** | 各服务代码目录物理隔离 |
| 联调 | **串行** | 需等开发完成，按依赖顺序 |
| QA 测试 | **串行** | 需等开发联调完 |

### 3. 恢复已有 Agent 实例

```
Agent 工具调用：
- description: "继续后端开发"
- subagent_type: "coder"
- resume: "backend-dev-instance-1"
- prompt: |
    继续完成未做完的接口开发。
    - 代码工作目录：{后端代码目录}
    - 契约：...
```

### 4. 代码隔离的三层防护

1. **系统提示词约束**：Agent 的 system_prompt.md 中写明禁止范围
2. **Prompt 路径前缀**：每次任务 prompt 中重申"所有写入必须以 X 开头"
3. **审批机制**：不开 `--yolo` 时，WriteFile 操作需要用户确认路径

### 5. 版本迭代

完成一次迭代后，下次迭代直接新建文档中心目录：

```bash
mkdir -p /path/to/new-docs-center/{design/api-contracts,docs/qa-reports}
```

文档按迭代隔离，代码仓库继续演进。

---

## 完整产出物清单

一次迭代完成后，文档中心应包含：

```
{DOC_CENTER_DIR}/
├── design/
│   ├── PRD.md                          ✅ 阶段一
│   └── api-contracts/
│       └── {服务名}-api.md             ✅ 阶段二
├── docs/
│   ├── integration-report.md           ✅ 阶段四
│   └── qa-reports/
│       └── test-report-YYYYMMDD.md     ✅ 阶段五
└── acceptance-report.md                ✅ 阶段六
```

代码变更在各自的代码仓库中（由 git 管理）。
