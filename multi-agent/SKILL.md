---
name: multi-agent-dev
description: 多 Agent 协作开发方法论。文档中心化 + 代码物理隔离 + 动态路径注入。支持一次迭代并行开发多个后端服务。完全通用，不绑定任何项目结构或技术栈。
---

# Multi-Agent Dev — 多 Agent 协作开发

本 Skill 提供一套**文档中心化、代码隔离的多 Agent 协作开发方法论**。

核心设计：
- **文档中心**：PRD、接口契约、测试报告集中存放在独立的目录（与代码仓库完全分离）
- **代码隔离**：每个 Agent 只能读写自己的代码目录，前后端代码互不暴露
- **动态路径**：工作目录不由配置文件硬编码，而是由项目经理在调度时通过 prompt 注入
- **完全通用**：不绑定任何具体目录结构、项目名或技术栈
- **并行开发**：一次迭代可并行开发多个后端服务（方式 B）

---

## 目录

- [核心概念](#核心概念)
- [架构概览](#架构概览)
- [六阶段工作流](#六阶段工作流)
- [方式 B：并行多服务开发](#方式-b并行多服务开发)
- [文件传递规范](#文件传递规范)
- [子 Agent 类型选择](#子-agent-类型选择)
- [Prompt 模板速查](#prompt-模板速查)
- [常见问题与陷阱](#常见问题与陷阱)
- [快速启动](#快速启动)

---

## 核心概念

### 1. 文档中心（Document Hub）

所有非代码产出集中存放在一个**与代码仓库完全分离**的目录中。路径由用户自己指定，没有强制要求：

```
{用户指定的文档中心}/
├── design/
│   ├── PRD.md
│   └── api-contracts/
│       ├── service-a-api.md
│       └── service-b-api.md
├── docs/
│   ├── integration-report.md
│   └── qa-reports/
│       └── test-report-*.md
└── acceptance-report.md
```

好处：
- 文档与代码仓库分离，代码仓库保持干净
- 一次迭代涉及多个服务的契约集中管理
- 文档位置完全由用户决定

### 2. 代码隔离（Code Isolation）

每个 Agent 有严格的读写边界。用户指定什么路径，Agent 就只能操作什么路径：

| Agent | 可读写代码 | 只读文档 |
|-------|-----------|---------|
| PM | 无 | 文档中心全部 |
| 后端 A | `{用户指定的服务A代码目录}/` | 文档中心 + 自己的契约 |
| 后端 B | `{用户指定的服务B代码目录}/` | 文档中心 + 自己的契约 |
| 前端 | `{用户指定的前端代码目录}/` | 文档中心全部契约 |
| QA | 无 | 文档中心全部 |

隔离手段：**系统提示词中的强制约束** + **绝对路径前缀校验** + **审批机制**。

### 3. 主 Agent = 路径分发器

项目经理（主 Agent）在启动时通过交互式问答收集用户的目录选择：

```
文档中心目录：由用户指定（如 ./docs-center/iteration-1/）
后端服务代码目录：由用户指定（如 ./backend/order-service/）
前端代码目录：由用户指定（如 ./frontend/admin-web/）
```

主 Agent 在调度每个子 Agent 时，把这些路径**注入到子 Agent 的 prompt 中**。子 Agent 的系统提示词使用 `${CODE_WORK_DIR}` 和 `${DOC_CENTER_DIR}` 占位符，实际值由任务 prompt 填充。

### 4. 前后端只通过 API 文档交互

后端开发完成后，前端通过**实际调用后端接口**进行联调。接口参数不匹配的问题记录在 `{文档中心}/docs/integration-report.md`，由项目经理协调修复。

**绝不**允许前后端 Agent 直接读写对方的代码目录来"适配"。

---

## 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│  文档中心（共享，所有 Agent 可读，位置由用户指定）               │
│  {用户指定的文档中心}/                                        │
│  ├── design/PRD.md                                          │
│  ├── design/api-contracts/                                  │
│  ├── docs/integration-report.md                             │
│  └── docs/qa-reports/                                       │
└─────────────────────────────────────────────────────────────┘
         ↑                    ↑                    ↑
    PM/QA 可读写          FE/BE 只读            主 Agent 协调
         ↓                    ↓                    ↓
┌─────────────────────┐    ┌─────────────────────────────────┐
│ 后端代码（隔离）      │    │ 前端代码（隔离）                   │
│ {用户指定的后端目录}/ │    │ {用户指定的前端目录}/              │
│ 只有对应 BE Agent 读写 │    │ 只有 FE Agent 读写               │
└─────────────────────┘    └─────────────────────────────────┘
```

---

## 六阶段工作流

### 阶段 0：交互式参数收集

启动主 Agent 后，PM Agent 会自动通过 `AskUserQuestion` 工具向用户收集本次迭代的参数：

**Step 1**: 文档中心目录（用户指定，如 `./docs-center/` 或绝对路径）
**Step 2**: 涉及的后端服务数量及各自代码目录（可多选）
**Step 3**: 前端项目代码目录（如有）
**Step 4**: PM Agent 构建完整路径后向用户确认
**Step 5**: PM Agent 自动创建文档中心目录结构

收集完成后，PM Agent 会在后续所有子 Agent 的 prompt 中自动注入对应的路径。

> 你也可以跳过交互，直接手动声明参数：
> ```
> 本次迭代参数：
> - 文档中心：/path/to/docs-center/
> - 后端：order-service → /path/to/backend/order-service/
> - 前端：/path/to/frontend/
> ```

### 阶段一：产品经理调研出方案

**目标**：产出 PRD，保存到文档中心。

```
Agent 工具调用：
- description: "产品经理出PRD"
- subagent_type: "plan"
- prompt: |
    你是产品经理。

    ## 本次迭代参数
    - 文档中心目录：{DOC_CENTER_DIR}
    - 涉及后端服务：{服务列表}
    - 前端项目：{如有}

    ## 任务
    请基于现有系统规划新功能。

    工作步骤：
    1. 阅读 {DOC_CENTER_DIR} 下是否已有相关文档
    2. 阅读各项目的 README 了解现有能力
    3. 如需深入代码调研，使用 explore 子 Agent（只读）
    4. 输出 PRD 到 {DOC_CENTER_DIR}/design/PRD.md

    PRD 必须包含：背景目标、用户故事、功能列表、业务流程、
    新页面/模块清单、涉及的后端服务、非功能性需求。
```

**输出物**：`{DOC_CENTER_DIR}/design/PRD.md` ✅

### 阶段二：前后端接口对齐

**目标**：产出按服务拆分的接口契约文档。

#### 2.1 并行启动前后端阅读 PRD

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

#### 2.2 主 Agent 汇总产出契约

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

### 阶段三：同步开发

**目标**：前后端按契约各自独立开发，代码互不可见。

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
    4. 严格按契约中的字段名和类型实现 TypeScript 类型（或其他语言类型）
    5. 处理加载态、空态、错误态
    6. 完成后执行项目构建命令验证通过
```

**输出物**：后端代码、前端代码 ✅

### 阶段四：联调自测

**目标**：前端实际调用后端接口，记录参数不匹配问题。

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

### 阶段五：QA 测试

**目标**：系统性测试。

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

### 阶段六：产品经理验收

**目标**：PM 对照 PRD 验收最终成果。

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

### 前提条件

- 阶段二（接口对齐）时，**所有涉及服务的契约必须已经确定**
- 服务之间通过契约解耦

### 并行调度

```
Agent 工具调用 1（后台，服务 A）：
- description: "开发服务A"
- subagent_type: "coder"
- run_in_background: true
- prompt: |
    你是服务 A 的后端工程师。
    - 代码工作目录：{服务A代码目录}
    - 契约：{DOC_CENTER_DIR}/design/api-contracts/service-a-api.md
    ...（开发任务）

Agent 工具调用 2（后台，服务 B）：
- description: "开发服务B"
- subagent_type: "coder"
- run_in_background: true
- prompt: |
    你是服务 B 的后端工程师。
    - 代码工作目录：{服务B代码目录}
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

并行开发完成后，**按依赖顺序逐个联调**：
1. 先联调基础服务
2. 再联调依赖服务
3. 前端最后统一验证

如果服务之间无强依赖，可同时进行联调。

---

## 文件传递规范

### 文档中心目录结构

```
{用户指定的文档中心}/
├── design/
│   ├── PRD.md                          # 阶段一：PM 输出
│   └── api-contracts/
│       ├── service-a-api.md            # 阶段二：接口契约
│       ├── service-b-api.md            # 阶段二：接口契约
│       └── ...
├── docs/
│   ├── integration-report.md           # 阶段四：联调报告
│   └── qa-reports/
│       └── test-report-YYYYMMDD.md     # 阶段五：测试报告
└── acceptance-report.md                # 阶段六：验收报告
```

### 代码目录（Agent 隔离）

```
{用户指定的后端服务A目录}/     # 后端 Agent A 独占
{用户指定的后端服务B目录}/     # 后端 Agent B 独占
{用户指定的前端目录}/           # 前端 Agent 独占
```

---

## 子 Agent 类型选择

| 类型 | 用途 | 可用工具 | 适用阶段 |
|------|------|---------|---------|
| `explore` | 只读探索代码库 | 读文件、搜索（无写入） | 调研、验收 |
| `plan` | 架构设计与方案制定 | 读文件、搜索（无 Shell、无写入） | 出 PRD、接口设计 |
| `coder` | 编码实现 | 读写文件、Shell、搜索 | 开发、联调、测试 |

---

## Prompt 模板速查

### 迭代参数注入模板（主 Agent 使用）

```
本次迭代参数：
- 文档中心目录：{用户指定的文档中心路径}
- 涉及后端服务：
  - {服务A} → {服务A代码路径}
  - {服务B} → {服务B代码路径}
- 前端项目：{前端代码路径}
- 新功能：{功能描述}
```

### 后端 Agent 任务 Prompt 模板

```
你是 {服务名} 的后端工程师。

## 本次迭代参数（严格遵守）
- 你的代码工作目录：{代码绝对路径}
- 文档中心：{文档中心绝对路径}
- API 契约：{契约文件绝对路径}
- PRD：{PRD 文件绝对路径}

## 约束
1. 所有文件写入必须使用以 {代码绝对路径} 开头的绝对路径
2. 禁止访问其他服务目录、前端目录
3. 严格按契约实现，契约模糊处记录但不擅自修改
4. 每完成一个接口标记 // API: ready
5. 完成后执行项目编译/测试命令验证通过

## 任务
{具体开发任务描述}
```

### 前端 Agent 任务 Prompt 模板

```
你是前端工程师。

## 本次迭代参数（严格遵守）
- 你的代码工作目录：{前端代码绝对路径}
- 文档中心：{文档中心绝对路径}
- API 契约目录：{契约目录绝对路径}
- PRD：{PRD 文件绝对路径}

## 约束
1. 所有文件写入必须使用以 {前端代码绝对路径} 开头的绝对路径
2. 禁止访问所有后端代码目录
3. 在 src/api/ 下为每个后端服务创建独立的 API 封装文件
4. 严格按契约中的字段名和类型实现类型定义
5. 处理加载态、空态、错误态
6. 完成后执行项目构建命令验证通过

## 任务
{具体开发任务描述}
```

### 接口契约文档模板

```markdown
# {服务名} API 接口契约

## 1. 接口名称
- **Method**: GET/POST/PUT/DELETE
- **Path**: /api/v1/...
- **Request**:
  - Query/Body/Path 参数，含类型、必填、示例
- **Response 200**: JSON 示例
- **Error Codes**: 列举常见错误码及含义

## 通用约定
- 分页参数: page, pageSize
- 时间格式: ISO 8601
- 错误格式: { "code": "", "message": "" }
```

---

## 常见问题与陷阱

### Q1: Agent 生成不存在的 API/组件怎么办？

**解法**：强制编译检查。后端跑编译命令，前端跑构建命令，报错就修。

### Q2: 多个后端 Agent 同时写代码会冲突吗？

**不会**，因为它们工作在不同的服务目录里（物理隔离）。但注意：如果两个服务共享同一个数据库或配置中心，需要提前在契约中约定好。

### Q3: 后端接口地址怎么告诉前端 Agent？

在联调阶段的 prompt 中注入：
```
- 服务 A 地址：http://localhost:8080
- 服务 B 地址：http://localhost:8081
```

### Q4: 服务之间有依赖怎么办？

**阶段二（接口对齐）必须先完成所有服务的契约**。如果服务 B 依赖服务 A 的新接口，那 A 的契约必须先定下来，B 才能按契约开发。并行开发时，各自只实现自己的部分，联调时再验证端到端流程。

### Q5: 怎么防止 Agent 写到错误目录？

三层防护：
1. **系统提示词约束**：Agent 的 system_prompt.md 中写明禁止范围
2. **Prompt 路径前缀**：每次任务 prompt 中重申"所有写入必须以 X 开头"
3. **审批机制**：不开 `--yolo` 时，WriteFile 操作需要用户确认路径

---

## 快速启动

### Step 1: 准备集中式 Agent 配置

将 `.kimi/agents/` 下的 Agent 配置文件放到任意统一位置（如 `~/.kimi/agents/` 或项目内），确保它们不硬编码任何项目路径。

### Step 2: 启动主 Agent

```bash
kimi --agent-file /path/to/pm-agent.yaml
```

### Step 3: 按交互式提示选择参数

PM Agent 会自动弹出问答，引导你选择：
1. 文档中心目录
2. 后端服务及代码路径
3. 前端项目路径

### Step 4: 按六阶段工作流调度

主 Agent 按本 Skill 的[六阶段工作流](#六阶段工作流)逐步启动子 Agent，每次启动时把路径注入 prompt。

---

## 延伸阅读

- `WORKFLOW.md` — 本目录下的完整实战操作指南
- `.kimi/agents/*.yaml` — Agent 配置文件示例
- `.kimi/agents/*-system.md` — 各角色的系统提示词模板
