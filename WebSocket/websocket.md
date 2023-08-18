**简介及原理**

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

WebSocket连接建立之后，通过该连接传输的数据报文会小很多，因为此类报文中只包含数据不包含请求头。



**Java中的具体代码实现**

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



**STOMP Over WebSocket**

STOMP：Simple (or Streaming) Text Oriented Message Protocol, formely known as TTMP, is a simple text-based protocol, designed for working with message-origented middleware (MOM). It provides an interoperable with format that allows STOMP clients to talk with any message borker supporting the protocol.

**Overview**

The protocol is broadly similar to [HTTP](https://en.wikipedia.org/wiki/HTTP), and works over [TCP](https://en.wikipedia.org/wiki/Transmission_Control_Protocol) using the following commands:

- CONNECT
- SEND
- SUBSCRIBE
- UNSUBSCRIBE
- BEGIN
- COMMIT
- ABORT
- ACK
- NACK
- DISCONNECT

Communication between client and server is through a "frame" consisting of a number of lines. The first line contains the command, followed by headers in the form <key>: <value> (one per line), followed by a blank line and then the body content, ending in a [null character](https://en.wikipedia.org/wiki/Null_character). Communication between server and client is through a MESSAGE, RECEIPT or ERROR frame with a similar format of headers and body content.

**Example**

```
SEND
destination:/queue/a
content-type:text/plain

hello queue a
^@
```

**Implementations**

These are some MOM products that support STOMP:

- [Apache ActiveMQ](https://en.wikipedia.org/wiki/Apache_ActiveMQ),
- [Fuse Message Broker](http://fusesource.com/products/enterprise-activemq/)
- [HornetQ](https://en.wikipedia.org/wiki/HornetQ)
- [Open Message Queue (OpenMQ)](https://en.wikipedia.org/wiki/Open_Message_Queue)
- [RabbitMQ](https://en.wikipedia.org/wiki/RabbitMQ) (message broker, has [support for STOMP](http://www.rabbitmq.com/stomp.html))
- [syslog-ng](https://en.wikipedia.org/wiki/Syslog-ng) through its [STOMP destination plugin](http://www.balabit.com/sites/default/files/documents/syslog-ng-ose-3.5-guides/en/syslog-ng-ose-v3.5-guide-admin/html/configuring-destinations-stomp.html)

A [list of implementations](https://stomp.github.io/implementations.html) is also maintained on the STOMP web site.

STOMP is also supported by the [Spring Framework](https://en.wikipedia.org/wiki/Spring_Framework) in module `org.springframework:spring-websocket`. [[1\]](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/socket/config/annotation/StompEndpointRegistry.html)

**External links**

- [STOMP website](https://stomp.github.io/)



**Spring集成STOMP**

1. 引入依赖

   ```xml
   dependency>
   	<groupId>org.springframework.boot</groupId>
   	<artifactId>spring-boot-starter-websocket</artifactId>
   </dependency>
   <dependency>
   	<groupId>org.webjars</groupId>
   	<artifactId>stomp-websocket</artifactId>
   	<version>2.3.3</version>
   </dependency>
   ```

2. 配置类

   ```java
   package com.example.messagingstompwebsocket;
   
   import org.springframework.context.annotation.Configuration;
   import org.springframework.messaging.simp.config.MessageBrokerRegistry;
   import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
   import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
   import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
   
   @Configuration
   @EnableWebSocketMessageBroker
   public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
   
     @Override
     public void configureMessageBroker(MessageBrokerRegistry config) {
       // 服务端给客户端推送消息时，客户端订阅的destination前缀
       config.enableSimpleBroker("/topic");
       // 客户端给服务端发送消息时，服务端的destination前缀，配合注解MessageMapping使用
       config.setApplicationDestinationPrefixes("/app");
     }
   
     @Override
     public void registerStompEndpoints(StompEndpointRegistry registry) {
       // 客户端和服务端建立WebSocket连接的endpoint
       registry.addEndpoint("/gs-guide-websocket").withSockJS();
     }
   
   }
   ```

3. 收发消息controller

    ```java
   package com.example.messagingstompwebsocket;
   
   import org.springframework.messaging.handler.annotation.MessageMapping;
   import org.springframework.messaging.handler.annotation.SendTo;
   import org.springframework.stereotype.Controller;
   import org.springframework.web.util.HtmlUtils;
   
   @Controller
   public class GreetingController {
   
   
     @MessageMapping("/hello")
     @SendTo("/topic/greetings")
     public Greeting greeting(HelloMessage message) throws Exception {
       Thread.sleep(1000); // simulated delay
       return new Greeting("Hello, " + HtmlUtils.htmlEscape(message.getName()) + "!");
     }
   
   }
   ```

   配合配置类，客户端通过destination：/app/hello 发送消息给服务端，服务端则推送消息给订阅destination：/topic/greetings 的客户端。



**参考资料：**

- [Web端即时通讯实践干货：如何让WebSocket断网重连更快速？](http://www.52im.net/thread-3098-1-1.html) 
- [新手入门贴：史上最全Web端即时通讯技术原理详解](http://www.52im.net/thread-338-1-1.html) 

- [使用WebSocket和SSE技术实现Web端消息推送](http://www.52im.net/thread-907-1-1.html) 

- [推送开发，专项技术区](http://www.52im.net/forum-104-1.html) 
- [爱奇艺WebSocket实时推送网关技术实践](http://www.52im.net/thread-3539-1-1.html)