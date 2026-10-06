# SM Apps 协议 1.0 契约

## 信封字段

所有消息使用单个 UTF-8 JSON 对象，并按以下稳定字段表达：

| 字段 | 类型 | 规则 |
| --- | --- | --- |
| `protocolVersion` | string | 当前值为 `1.0`，不支持的版本返回 `UNSUPPORTED_VERSION` |
| `messageId` | UUID | 逻辑消息全局唯一；同一动作重试必须复用 |
| `messageType` | enum | 使用 `MessageType.wireValue()` 的稳定值 |
| `senderId` | UUID 或 null | 匿名请求为 null，其他客户端请求必须匹配登录会话 |
| `timestamp` | ISO-8601 UTC | 客户端值仅供诊断，服务端接收时重新生成 |
| `payload` | object | 必须与 `messageType` 对应的独立 record 匹配 |
| `requestId` | UUID 或 null | 响应关联原请求；客户端原始请求必须为 null |

注册、登录和查询的 `messageId` 只标识该请求。发送聊天时，请求信封的 `messageId`
同时作为最终业务聊天 ID，重试不得改变。服务端回执生成新的信封 `messageId`，并用
`requestId` 指向原请求；向其他会话推送的 `CHAT_EVENT` 沿用业务聊天 ID。

## 消息记录

| 消息类型 | payload record | 必须字段 |
| --- | --- | --- |
| `REGISTER_REQUEST` | `RegisterRequest` | username, password |
| `LOGIN_REQUEST` | `LoginRequest` | username, password |
| `LOGOUT_REQUEST` | `LogoutRequest` | sessionToken |
| `RESUME_REQUEST` | `ResumeRequest` | sessionToken, cursor |
| `CHAT_PUBLIC_SEND` | `ChatPublicSend` | text |
| `CHAT_PRIVATE_SEND` | `ChatPrivateSend` | recipientId, text |
| `CHAT_TOPIC_SEND` | `ChatTopicSend` | topicId, text |
| `CHAT_EVENT` | `ChatEvent` | conversationId, senderId, text, serverSequence |
| `FILE_DECLARE` | `FileDeclare` | recipientId, displayName, size, sha256 |
| `FILE_EVENT` | `FileEvent` | fileId, state, size, sha256 |
| `ERROR` | `ErrorResponse` | code, message, requestId |

成功响应使用 `RegisterResult`、`LoginResult`、`LogoutResult`、`ResumeResult`、
`ChatAccepted` 和 `FileReady`。DTO 只包含协议值，不包含数据库实体、RabbitMQ Channel、
存储路径或服务端密钥。

## 初始边界

- 用户名为 3 至 32 个 Unicode 编码点，UTF-8 编码后最多 64 字节。
- 密码为 8 至 128 个 Unicode 编码点；协议对象的字符串输出会隐藏密码和会话令牌。
- 聊天正文为 1 至 4096 个 Unicode 编码点，且不能只含空白。
- 展示文件名为 1 至 255 个 Unicode 编码点。
- 文件大小为 1 字节至 20 MiB；SHA-256 为 64 位小写十六进制字符串。
- 单个 WSS 文本帧为 1 字节至 64 KiB。

`ProtocolLimits` 集中保存可配置阈值，`ProtocolValidator` 同时用于 DTO 构造和服务端
边界重检。若负载测试后调整阈值，必须同步示例配置、`docs/environment.txt` 和边界测试。

## 文件内容接口

文件元数据先通过 WSS 的 `FILE_DECLARE` 申请，文件字节只走同域名、同证书下的 HTTPS：

- `PUT /api/files/{fileId}/content` 上传文件，必须携带 Bearer 会话令牌和准确的
  `Content-Length`。长度必须等于声明值且不得超过 20 MiB。
- `GET /api/files/{fileId}/content` 下载文件，服务端流式写出，使用净化后的展示名称生成
  `Content-Disposition`，不返回存储路径。
- 每次请求都重新校验会话用户、所有者或接收者、`FileStatus` 和授权过期时间。
- 同一 `fileId` 的上传重试只允许处于 `READY` 或 `UPLOADING` 状态。重试必须先删除或
  原子替换旧临时文件，使用 `REPLACE_TEMPORARY_FILE` 语义，禁止拼接两次字节流。
- 上传完成后必须同时核对实际字节数和 SHA-256；成功后才从临时文件原子移动到最终位置。

## 错误目录

WSS 通过 `ERROR` payload 返回安全消息。协议结构、版本和类型错误发送错误帧后关闭当前
请求连接；业务错误保持连接。HTTP 文件接口使用下表状态码。服务端日志仅用
`requestId`、`messageId` 关联内部诊断，不把 SQL、路径、堆栈、令牌或文件正文返回客户端。

| 错误码 | HTTP | WSS | 重试策略 | 安全提示 |
| --- | ---: | --- | --- | --- |
| `INVALID_INPUT` | 400 | 错误帧后关闭 | 不重试 | 请求内容不合法 |
| `UNSUPPORTED_VERSION` | 400 | 错误帧后关闭 | 不重试 | 协议版本不受支持 |
| `UNSUPPORTED_MESSAGE_TYPE` | 400 | 错误帧后关闭 | 不重试 | 消息类型不受支持 |
| `UNAUTHENTICATED` | 401 | 错误帧 | 重新认证 | 请先登录 |
| `SESSION_EXPIRED` | 401 | 错误帧 | 重新认证 | 登录已过期 |
| `FORBIDDEN` | 403 | 错误帧 | 不重试 | 无权执行此操作 |
| `NOT_FOUND` | 404 | 错误帧 | 不重试 | 请求的资源不存在 |
| `USERNAME_TAKEN` | 409 | 错误帧 | 不重试 | 用户名已被使用 |
| `INVALID_CREDENTIALS` | 401 | 错误帧 | 不重试 | 用户名或密码错误 |
| `LIMIT_EXCEEDED` | 413 | 错误帧 | 不重试 | 请求超过允许限制 |
| `CONFLICT` | 409 | 错误帧 | 刷新状态后决定 | 资源状态已发生变化 |
| `RATE_LIMITED` | 429 | 错误帧 | 幂等请求退避重试 | 请求过于频繁，请稍后重试 |
| `TEMPORARY_UNAVAILABLE` | 503 | 错误帧 | 幂等请求退避重试 | 服务暂时不可用，请稍后重试 |
| `INTERNAL_ERROR` | 500 | 错误帧 | 幂等请求退避重试 | 服务处理失败 |

## 编解码顺序

`ProtocolCodec` 严格按以下顺序处理输入：

1. 在创建 JSON 对象前检查原始 WSS 帧字节数。
2. 使用报告错误模式的 UTF-8 解码器拒绝非法字节序列。
3. 使用开启重复字段检测和读取约束的 JSON 解析器检查结构、嵌套深度、字符串长度、
   对象字段数和数组元素数。
4. 校验信封必需字段、协议版本和 `messageType`，再从固定映射选择唯一 payload record。
5. 校验 payload 必需字段并忽略不影响既有语义的未知可选字段，然后构造 record；任何
   `@class`、`@type`、`$type`、`_class` 或 `javaClass` 标记均直接拒绝。

编解码器从不启用 Jackson 多态默认类型。时间只接受带 `Z` 后缀的 ISO-8601 UTC
`Instant`，SHA-256 只接受 64 位小写十六进制。协议中没有金额字段，文件二进制也不进入
JSON。JSON 字段顺序不影响解析结果。

## 兼容策略

同一主版本只追加带明确缺省行为的可选字段。删除字段、字段改名、字段类型变化或语义变化
均属于破坏性变更，必须提升主版本并提供迁移说明。未知 `messageType` 返回
`UNSUPPORTED_MESSAGE_TYPE`，不得映射为现有类型。

示例位于 `chat-protocol/src/main/resources/examples/protocol-v1`，每个文件都是一个完整
信封，同时作为版本 1.0 golden fixtures。测试验证旧解析器可忽略未知可选字段，新解析器
可读取旧样例。`chat-public-send.json` 与 `chat-accepted.json` 使用同一业务消息 ID，并由
回执 `requestId` 指向请求，证明重试幂等和请求关联；两份证明样例不含密码。

示例值只用于契约说明，不得作为测试账号或生产凭据。
