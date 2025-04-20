```markdown:README.md
# 小组作业管理系统

一个基于 Spring Boot 和 WebSocket 的智能对话系统，使用 Ollama 作为 AI 模型后端。

## 目录
- [功能特点](#功能特点)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [API 文档](#api-文档)
  - [WebSocket 接口](#websocket-接口)
  - [HTTP 接口](#http-接口)
- [配置说明](#配置说明)
  - [AI模型配置](#ai模型配置)
  - [WebSocket配置](#websocket配置)
- [错误处理](#错误处理)
- [注意事项](#注意事项)

## 功能特点

- 实时对话：基于 WebSocket 的即时通讯
- 智能应答：使用 deepseek-r1:1.5b 模型，支持深度思考和分析
- 上下文理解：支持多轮对话记忆
- 流式响应：实时返回 AI 回答内容
- 跨域支持：支持多源访问

## 技术栈

- 后端框架：Spring Boot 3.x
- 通信协议：WebSocket
- AI 模型：Ollama (deepseek-r1:1.5b)
- 数据库：MySQL 8.0
- 构建工具：Maven

## 快速开始

1. 克隆仓库：
```bash
git clone https://github.com/your-repo/group-work-management.git
```

2. 配置环境：
```bash
mvn clean install
```

3. 启动服务：
```bash
mvn spring-boot:run
```

4. 访问 WebSocket 接口：
```javascript
const socket = new WebSocket('ws://localhost:4388/chat');
```

## API 文档

### WebSocket 接口

#### 聊天接口

- **URL**: `ws://localhost:4388/chat`
- **协议**: WebSocket
- **允许来源**: 所有域名 (`*`)
- **连接要求**:
  - 客户端需在连接时提供有效的认证信息
  - 支持断线重连机制
  - 最大消息长度：1024 字节

##### 消息格式

1. 发送消息格式：
```json
{
  "type": "message",
  "content": "用户输入内容",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

2. 接收消息格式：
```json
{
  "type": "response",
  "content": "AI 回复内容",
  "timestamp": "2024-01-01T12:00:01Z",
  "status": "success"
}
```

### HTTP 接口

#### 获取系统状态

- **URL**: `GET /api/status`
- **响应格式**:
```json
{
  "status": "running",
  "version": "1.0.0",
  "uptime": "2h 30m"
}
```

#### 获取AI模型信息

- **URL**: `GET /api/ai/info`
- **响应格式**:
```json
{
  "model": "deepseek-r1:1.5b",
  "temperature": 0.8,
  "max_tokens": 2048
}
```

## 配置说明

可以，系统已经支持通过 HTTP 协议访问。以下是 HTTP 接口的详细信息：

### HTTP 接口

#### 获取系统状态

- **URL**: `GET /api/status`
- **响应格式**:
```json
{
  "status": "running",
  "version": "1.0.0",
  "uptime": "2h 30m"
}
```

#### 获取AI模型信息

- **URL**: `GET /api/ai/info`
- **响应格式**:
```json
{
  "model": "deepseek-r1:1.5b",
  "temperature": 0.8,
  "max_tokens": 2048
}
```

#### 发送消息（同步）

- **URL**: `POST /api/ai/chat`
- **请求格式**:
```json
{
  "prompt": "你好，请介绍一下你自己",
  "model": "deepseek-r1:1.5b"
}
```

- **响应格式**:
```json
{
  "response": "我是基于 deepseek-r1:1.5b 模型的智能助手...",
  "model": "deepseek-r1:1.5b",
  "done": true
}
```

#### 获取历史记录

- **URL**: `GET /api/ai/history`
- **响应格式**:
```json
[
  {
    "role": "user",
    "content": "你好",
    "timestamp": "2024-01-01T12:00:00Z"
  },
  {
    "role": "assistant",
    "content": "你好！有什么我可以帮助你的吗？",
    "timestamp": "2024-01-01T12:00:01Z"
  }
]
```

### 使用示例

#### 使用 curl 测试

1. 获取系统状态：
```bash
curl -X GET http://localhost:4388/api/status
```

2. 发送消息：
```bash
curl -X POST http://localhost:4388/api/ai/chat \
-H "Content-Type: application/json" \
-d '{"prompt": "你好，请介绍一下你自己", "model": "deepseek-r1:1.5b"}'
```

3. 获取历史记录：
```bash
curl -X GET http://localhost:4388/api/ai/history
```

#### 使用 Postman 测试

1. 新建 GET 请求，URL 设置为 `http://localhost:4388/api/status`
2. 新建 POST 请求，URL 设置为 `http://localhost:4388/api/ai/chat`
  - 在 Body 选项卡中选择 raw 和 JSON 格式
  - 输入请求内容：
```json
{
  "prompt": "你好，请介绍一下你自己",
  "model": "deepseek-r1:1.5b"
}
```


## 错误处理

| 错误码 | 描述 | 解决方案 |
|--------|------|----------|
| 1001   | 连接超时 | 检查网络连接，重试 |
| 1002   | 消息格式错误 | 检查消息格式是否符合规范 |
| 1003   | 认证失败 | 提供有效的认证信息 |
| 1004   | 服务器内部错误 | 联系管理员 |

## 注意事项

1. 建议使用最新版本的浏览器
2. 确保网络连接稳定
3. 单个消息长度不应超过 1024 字节
4. 长时间未操作可能导致连接断开
```
