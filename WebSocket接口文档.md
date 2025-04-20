# WebSocket 接口文档

## 概述

本文档描述了系统中WebSocket接口的使用方法，包括连接建立、消息发送和接收等操作。WebSocket提供了实时双向通信功能，主要用于AI对话服务。

## 连接信息

### 连接地址

```
ws://[服务器地址]:[端口]/chat
```

默认端口为4388，可通过服务器配置修改。

### 连接参数

连接不需要额外的查询参数，支持跨域访问（CORS已启用）。

## 消息格式

### 请求消息格式

#### 1. 普通AI对话请求

```json
{
  "prompt": "用户输入的问题或指令",
  "model": "模型名称",  // 可选，默认使用服务器配置的模型
  "stream": true,      // 建议设置为true以启用流式响应
  "messages": [       // 可选，用于保持对话上下文
    {
      "role": "user",
      "content": "历史消息内容"
    },
    {
      "role": "assistant",
      "content": "历史回复内容"
    }
  ],
  "context": "上下文标识符"  // 可选，用于保持对话上下文
}
```

#### 2. 心跳检测请求

```json
{
  "type": "ping"
}
```

### 响应消息格式

#### 1. AI对话响应

```json
{
  "response": "AI生成的回复内容",
  "model": "使用的模型名称",
  "done": false,  // false表示流式响应中的部分内容，true表示响应结束
  "createdAt": "2023-01-01T12:00:00Z",
  "doneReason": "stop",  // 完成原因，仅在done为true时出现
  "context": [...],  // 上下文标识符，用于后续请求
  "totalDuration": 1234,  // 总处理时间（毫秒）
  "loadDuration": 123,    // 模型加载时间（毫秒）
  "promptEvalCount": 10,  // 提示评估计数
  "promptEvalDuration": 100,  // 提示评估时间（毫秒）
  "evalCount": 50,        // 评估计数
  "evalDuration": 500     // 评估时间（毫秒）
}
```

#### 2. 错误响应

```json
{
  "response": "错误信息描述",
  "done": true
}
```

#### 3. 心跳检测响应

```json
{
  "type": "pong"
}
```

## 连接生命周期

### 1. 连接建立

客户端通过WebSocket协议连接到服务器指定端点。连接成功后，服务器会为该连接分配一个唯一的会话ID，并在日志中记录连接建立信息。

### 2. 消息交互

- 客户端发送消息：客户端可以发送JSON格式的消息到服务器。
- 服务器处理消息：服务器接收到消息后，会根据消息类型进行相应处理。
- 服务器响应：处理完成后，服务器会将结果以JSON格式发送回客户端。

### 3. 心跳检测

客户端可以定期发送心跳检测消息（ping）来确保连接的活跃状态，服务器会响应pong消息。

### 4. 连接关闭

连接可以由客户端主动关闭，也可能因为网络问题、服务器错误或超时而被关闭。连接关闭后，服务器会清理相关资源。

## 错误处理

### 常见错误

1. **提示信息为空**：当请求中的prompt字段为空时，服务器会返回错误消息。
2. **消息处理错误**：当服务器在处理消息过程中遇到错误时，会返回相应的错误信息。
3. **传输错误**：当WebSocket连接出现传输问题时，服务器会记录错误并可能关闭连接。

### 错误响应示例

```json
{
  "response": "提示信息不能为空",
  "done": true
}
```

## 配置参数

服务器配置了以下WebSocket相关参数：

- 最大文本消息缓冲区大小：32KB
- 最大二进制消息缓冲区大小：32KB
- 最大会话空闲超时时间：30分钟
- 异步发送超时时间：60秒

## 示例代码

### JavaScript客户端示例

```javascript
// 建立WebSocket连接
const socket = new WebSocket('ws://localhost:4388/chat');

// 连接建立时的处理
socket.onopen = function(event) {
  console.log('WebSocket连接已建立');
  
  // 发送消息示例
  const message = {
    prompt: '你好，请介绍一下自己',
    model: 'llama2',
    stream: true
  };
  socket.send(JSON.stringify(message));
};

// 接收消息的处理
socket.onmessage = function(event) {
  const response = JSON.parse(event.data);
  console.log('收到消息:', response);
  
  // 处理流式响应
  if (response.done) {
    console.log('响应完成');
  } else {
    // 继续接收流式响应
    console.log('部分响应:', response.response);
  }
};

// 错误处理
socket.onerror = function(error) {
  console.error('WebSocket错误:', error);
};

// 连接关闭的处理
socket.onclose = function(event) {
  console.log('WebSocket连接已关闭，代码:', event.code);
};

// 发送心跳检测
function sendPing() {
  if (socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify({type: 'ping'}));
  }
}

// 每30秒发送一次心跳
setInterval(sendPing, 30000);
```

## 注意事项

1. 建议启用流式响应（stream=true）以获得更好的用户体验。
2. 对于长时间运行的连接，客户端应实现心跳机制以保持连接活跃。
3. 客户端应妥善处理连接错误和重连逻辑。
4. 服务器配置了跨域访问支持，可以从任何源访问WebSocket服务。
5. 对于大型请求或响应，注意不要超过配置的缓冲区大小限制。