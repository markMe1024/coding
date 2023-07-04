**简介及原理：**

WebSocket是一种基于HTTP的长连接技术，能够实现客户端和服务端的双向通信，可用来构建实时应用。

因为HTTP本身是基于TCP连接的，所以，WebSocket在HTTP协议的基础上做了一个简单的升级，即建立TCP连接后，浏览器发送请求时，附带几个头：

```
GET /chat HTTP/1.1
Host: www.example.com
Upgrade：websocket
Connection: Upgrade
```

这就表示客户端希望升级连接，变成长连接的WebSocket，服务器返回升级成功的响应：

```
HTTP/1.1. 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
```

收到成功响应后表示WebSocket“握手”成功，这样，代表WebSocket的这个TCP连接不会被服务器关闭，而是一直保持，服务器可随时向浏览器推送消息，浏览器也可随时向服务器推送消息。

双方推送的消息既可以是文本消息，也可以是二进制消息，一般来说，绝大部分应用程序会推送基于JSON的文本消息。



**Java中的具体代码实现：**

WebSocket采用CS架构，服务端集成WebSocket之后，会成为Server，浏览器则作为Client。

在Java中，可以通过在项目pom文件中添加`spring-websocket`或者`spring-boot-starter-websocket`依赖，来集成WebSocket。

集成WebSocket之后的服务端，会暴漏出一个URL，如下述代码中的`/chat`，客户端使用`ws`协议访问该URL，即可与服务端建立连接。

```java
@Bean
WebSocketConfigurer createWebSocketConfigurer(
        @Autowired ChatHandler chatHandler,
        @Autowired ChatHandshakeInterceptor chatInterceptor)
{
    return new WebSocketConfigurer() {
        public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
            // 把URL与指定的WebSocketHandler关联，可关联多个:
            registry.addHandler(chatHandler, "/chat").addInterceptors(chatInterceptor);
        }
    };
}
```

每一个WebSocket连接，会产生一个`WebSocketSession`，每个Session拥有唯一ID。和WebSocket相关的数据，如用户名等，均可放入关联的`getAttributes()`中。

```java
@Component
public class ChatHandler extends TextWebSocketHandler {
    // 保存所有Client的WebSocket会话实例:
    private Map<String, WebSocketSession> clients = new ConcurrentHashMap<>();

    /**
     * 连接建立后
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 新会话根据ID放入Map:
        clients.put(session.getId(), session);
        session.getAttributes().put("name", "Guest1");
    }

    /**
     * 连接断开后
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        clients.remove(session.getId());
    }
}
```

用实例`clients`持有当前所有的`WebSocketSession`是为了广播，即向所有用户推送同一消息时，可以这么写：

```java
String json = ...
TextMessage message = new TextMessage(json);
for (String id : clients.keySet()) {
    WebSocketSession session = clients.get(id);
    session.sendMessage(message);
}
```

如果想将消息推送给部分用户，则需要过滤出这些用户的Session，然后再发送消息。

接收消息的示例代码如下：

```java
@Component
public class ChatHandler extends TextWebSocketHandler {
    ...
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String s = message.getPayload();
        String r = ... // 根据输入消息构造待发送消息
        broadcastMessage(r); // 推送给所有用户
    }
}
```
