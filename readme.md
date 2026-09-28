# SM_apps 网络聊天系统目录规划

## 1 项目定位

本项目由个人独立设计与开发，目标是完成一个基于RabbitMQ的Java C/S架构网络聊天系统。系统范围严格限定为用户注册、登录、在线聊天、公共消息、私人消息和文件传输，并围绕这些功能验证点对点、发布订阅和主题路由等消息通信模式。

**技术基线：Java 17、Maven 3.6.3、RabbitMQ 4.3.6。** **运行边界：Windows 10及以上桌面客户端、Ubuntu 22.04服务端，客户端只连接应用服务器，RabbitMQ仅作为服务端内部基础设施。**

## 2 总体模块与依赖方向

项目采用四个Maven模块，数量保持最小且与课程功能一一对应：

```text
chat-client ──┐
              ├──> chat-protocol ──> chat-common
chat-server ──┘
```

- **chat-common：** 提供跨模块使用的ID、时间、基础校验和通用错误类型，不承载聊天业务或外部系统访问。
- **chat-protocol：** 定义客户端与服务端共享的消息信封、注册登录、聊天、文件元数据、错误响应及编解码契约。
- **chat-server：** 承载认证、用户、聊天、文件、客户端连接、RabbitMQ路由和数据持久化，是所有业务事实的唯一来源。
- **chat-client：** 承载桌面界面、登录会话、服务端连接、聊天展示和文件收发，不直接连接RabbitMQ或数据库。

**禁止依赖：chat-common不得依赖其他业务模块，chat-protocol不得依赖client或server，client不得依赖server实现。** 模块间只通过公开类型和稳定协议协作，禁止形成循环依赖。

## 3 全局目录结构

下列目录已在项目中创建；树中的 `pom.xml`、配置文件和源码文件表示后续工程骨架阶段应放置的位置，不以空文件占位。

```text
SM_apps/
├── readme.md
├── agents.md
├── code_rule.txt
├── pom.xml                              # 父工程与统一依赖管理
├── 1-SDA第1次课外（实验）指导书-202609.doc
├── 2-SDA第1次课外（实验）报告（模板）-202609.doc
│
├── chat-common/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/smapps/chat/common/
│       │   │   ├── id/                 # MessageId、UserId、FileId等值对象
│       │   │   ├── time/               # Clock与统一时间处理
│       │   │   ├── validation/         # 跨模块基础校验结果
│       │   │   └── error/              # 通用错误码基础类型
│       │   └── resources/
│       └── test/
│           ├── java/com/smapps/chat/common/
│           └── resources/
│
├── chat-protocol/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/smapps/chat/protocol/
│       │   │   ├── envelope/           # 统一消息信封与协议版本
│       │   │   ├── auth/               # 注册、登录、注销请求与响应
│       │   │   ├── chat/               # 公共消息、私人消息与消息回执
│       │   │   ├── file/               # 文件声明、授权、进度与结果元数据
│       │   │   ├── error/              # 稳定的协议错误响应
│       │   │   └── codec/              # UTF-8协议序列化与反序列化
│       │   └── resources/
│       └── test/
│           ├── java/com/smapps/chat/protocol/
│           └── resources/
│
├── chat-server/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/smapps/chat/server/
│       │   │   ├── bootstrap/          # 服务端启动与关闭入口
│       │   │   ├── config/             # 有界线程池、端口和外部配置装配
│       │   │   ├── transport/          # 客户端连接、会话与协议边界
│       │   │   ├── auth/               # 注册、登录、注销与会话验证
│       │   │   ├── user/               # 用户资料和在线状态
│       │   │   ├── chat/               # 公共消息与私人消息业务编排
│       │   │   ├── file/               # 文件授权、流式传输与完整性校验
│       │   │   ├── security/           # 密码哈希、权限与输入安全
│       │   │   ├── messaging/
│       │   │   │   ├── topology/       # 交换机、队列、绑定和死信拓扑
│       │   │   │   ├── publisher/      # publisher confirms与发布结果
│       │   │   │   └── consumer/       # manual ack、幂等和有限重试
│       │   │   └── persistence/
│       │   │       ├── entity/         # 服务端持久化实体
│       │   │       └── repository/     # 用户、消息、文件元数据仓储
│       │   └── resources/
│       │       ├── application-example.yml
│       │       ├── logback.xml
│       │       └── db/migration/        # 数据库版本迁移脚本
│       └── test/
│           ├── java/com/smapps/chat/server/
│           │   ├── unit/               # 领域与业务单元测试
│           │   └── integration/        # RabbitMQ、持久化和传输集成测试
│           └── resources/
│
├── chat-client/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/smapps/chat/client/
│       │   │   ├── bootstrap/          # 客户端启动与退出清理
│       │   │   ├── config/             # 服务地址、超时和本地非机密配置
│       │   │   ├── network/            # 服务端连接、重连和协议收发
│       │   │   ├── session/            # 当前登录会话和本地展示状态
│       │   │   ├── auth/               # 注册、登录和注销交互
│       │   │   ├── chat/               # 公共与私人会话状态
│       │   │   ├── file/               # 文件选择、传输和进度展示
│       │   │   └── ui/                 # 桌面窗口、视图和UI线程调度
│       │   └── resources/ui/            # 布局、样式、图标等界面资源
│       └── test/
│           ├── java/com/smapps/chat/client/
│           │   ├── unit/
│           │   └── integration/
│           └── resources/
│
├── docs/
│   ├── environment.txt                 # 已完成的开发与服务器环境记录
│   ├── architecture/                   # 总体架构图、时序图和RabbitMQ拓扑图
│   ├── adr/                            # 数据库、传输协议、UI框架等关键决策
│   ├── testing/                        # 测试计划、用例与可复现实验步骤
│   └── report/                         # 实验报告截图、图表和结果材料
│
└── scripts/
    ├── dev/                            # 本地构建、启动和测试辅助脚本
    └── deploy/                         # 服务端部署、停止和状态检查脚本
```

## 4 功能与目录映射

| 核心功能 | 公共协议 | 服务端实现 | 客户端实现 | RabbitMQ用途 |
| --- | --- | --- | --- | --- |
| 用户注册 | `protocol/auth` | `server/auth`、`server/user`、`persistence` | `client/auth` | 不承载密码原文，仅在需要时发布注册结果事件 |
| 用户登录 | `protocol/auth` | `server/auth`、`server/security`、`transport` | `client/auth`、`client/session` | 不作为认证入口，登录由应用服务器完成 |
| 公共消息 | `protocol/chat` | `server/chat`、`messaging` | `client/chat` | fanout交换机向在线会话广播 |
| 私人消息 | `protocol/chat` | `server/chat`、`messaging` | `client/chat` | direct交换机按 `user.<userId>` 路由 |
| 主题消息 | `protocol/chat` | `server/chat`、`messaging` | `client/chat` | topic交换机按 `room.<roomId>` 等模式匹配 |
| 文件传输 | `protocol/file` | `server/file`、`persistence` | `client/file` | 只发送文件元数据和状态事件，二进制走受控流式接口 |

## 5 RabbitMQ拓扑规划

RabbitMQ拓扑集中维护在 `chat-server/.../messaging/topology`，禁止在业务类中随意声明队列。名称先按以下语义规划，最终值通过服务端配置集中注入：

- **公共消息交换机：`chat.public.fanout`，类型：fanout。** 用于把公共消息广播到当前在线会话对应的消费队列。
- **私人消息交换机：`chat.private.direct`，类型：direct。** 路由键采用 `user.<userId>`，服务端必须先完成发送者认证和接收者授权校验。
- **主题消息交换机：`chat.topic`，类型：topic。** 路由键采用 `room.<roomId>` 或明确的业务主题，用于满足主题通配符模式的实验验证。
- **死信交换机：`chat.dlx`。** 超过有限重试次数、格式非法或无法处理的消息进入死信队列，禁止无限requeue。
- **文件事件：只传元数据。** 文件ID、大小、SHA-256摘要、发送者、接收者和传输状态可作为事件，文件二进制禁止进入RabbitMQ。

生产端统一开启publisher confirms，消费端统一使用manual ack，并以 `messageId` 实现幂等处理。消费者prefetch必须为有限值，实际数值通过集成测试观察吞吐、延迟和内存后确定。

## 6 核心交互流程

### 6.1 注册与登录

客户端通过 `network` 将 `protocol/auth` 请求发送到服务端 `transport`，服务端完成输入校验、密码哈希、用户持久化和会话签发。RabbitMQ不参与密码传输和身份校验，任何客户端传入的用户ID都不能替代服务端会话身份。

### 6.2 公共与私人消息

客户端消息先进入服务端 `chat`，经认证、授权和协议校验后再由 `messaging/publisher` 发布。服务端消费者完成幂等检查、必要持久化和在线会话投递，处理成功后才执行ack。

### 6.3 文件传输

客户端先提交文件元数据并申请传输授权，再通过服务端 `file` 提供的流式接口上传或下载。服务端执行大小、类型、规范路径和SHA-256校验，校验完成后原子落盘，并通过RabbitMQ发布进度或完成事件。

### 6.4 断线恢复

客户端 `network` 使用有上限的指数退避重连，`session` 保存最后确认的服务端游标。重连成功后由服务端补发缺失消息，客户端按 `messageId` 去重，不依赖客户端本地时间判定消息先后。

## 7 配置与运行数据边界

- **服务端配置：** 应放在 `chat-server/src/main/resources/application-example.yml` 中提供无机密示例，真实RabbitMQ、数据库、会话密钥和存储路径通过环境变量注入。
- **客户端配置：** 仅包含应用服务器地址、连接超时和安全的界面偏好，不得包含RabbitMQ凭据、数据库口令或服务端私钥。
- **运行数据：** 上传文件、临时文件、数据库文件、日志和消息代理数据位于源码目录之外，通过配置指定；这些内容不得提交到Git。
- **课程材料：** 两份原始 `.doc` 保留在项目根目录，不修改、不重命名，作为需求和报告格式的最高依据。

## 8 测试目录职责

`unit` 只验证领域规则、协议校验、错误映射、幂等和文件校验，不访问真实公网。`integration` 使用隔离的RabbitMQ vhost、数据库schema和临时文件目录，覆盖注册、登录、公共消息、私人消息、文件传输、重复投递、断线重连和权限拒绝。

所有模块以 `mvn verify` 为统一质量门禁，测试类分别使用 `*Test` 和 `*IT` 命名。测试结束必须清理队列、连接和临时文件，不得依赖执行顺序或开发者机器上的固定路径。

## 9 明确不纳入当前目录的内容

本项目不预设微服务拆分、API网关、服务注册中心、Kubernetes、分布式缓存、搜索引擎、数据仓库或多云部署目录。只有当核心六项功能已经完成且出现可验证需求时，才通过ADR评估新增模块，避免个人课程项目被基础设施复杂度淹没。

## 10 建设顺序

1. 创建父POM与四个模块POM，固定Java 17、依赖版本、格式化、单元测试和集成测试插件。
2. 在 `chat-protocol` 定义消息信封、注册登录、聊天、文件元数据和错误协议，并完成序列化兼容测试。
3. 在 `chat-server` 完成持久化、认证会话和客户端传输边界，再接入RabbitMQ拓扑、可靠发布和消费。
4. 在 `chat-client` 完成连接与会话状态后实现登录界面、公共聊天、私人聊天和文件传输界面。
5. 按端到端场景执行 `mvn verify`，保留可复现实验步骤、架构图、RabbitMQ拓扑图和运行截图供实验报告使用。
