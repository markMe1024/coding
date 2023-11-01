问题：

1. 机房交换机断电后恢复，CMP各模块自动重新拉起，inetwork、icinder队列丢失。
2. 如果能把 Spring Cloud Stream 和 Spring Rabbit 的运行机制搞清楚，可以组织个内部培训。



核心问题：

1. 



问题：

1. Spring Cloud Stream，是谁创建的队列，有三个思路如下：
   1. 看下它创建队列的机制。
      1. 启动服务时，自动创建的exchange和queue。
      2. 
   2. 通过删除队列的报错信息，搜索引擎搜一下。
      1. 从这个报错信息中，还能看到什么有用信息？
   3. 看下Spring Cloud Stream是怎么配置的。
   
2. Spring Cloud Stream 如何进行参数配置？

3. Spring Cloud Stream 是基于SpringBoot和 Spring Integration的，那么是不是Spring Boot可以进行的配置，Spring Cloud Stream都可以？

4. Spring Cloud Stream 队列被删除后，有自动重建机制么，可以通过什么配置来实现吗？

   思路：

   - 从报错信息中分析，看有没有有用的信息。
   - 如果直接看不到，就开debug看下。

   问题：

   1. stream也restart了，但是rabbitAdmin为什么为null？
      - 它没有被初始化，因为没有调用doStart()方法，为什么springBoot调用了，stream没有？
   2. container是什么？
   3. 有个属性是applicationContext，inetwork启动时没有初始化，rabbitAdmin是这里获取的。
      1. 所以，我项目启动时，自己手动赋值行吗？
      2. 为什么iregion就赋值了呢？
   4. 纷繁复杂的线索中，究竟什么是核心问题？
   5. message listener container rabbitmq 是什么？
      1. message listener 是用来消费消息的？

   结论：

5. Spring Cloud Stream 和 Spring Amqp 之间的技术选型依据是什么？

|                              | Spring Cloud Stream                                          | Spring AMQP                                            |
| ---------------------------- | ------------------------------------------------------------ | ------------------------------------------------------ |
| 队列删除客户端是否会自动重建 | 默认不会                                                     | 默认会                                                 |
| 是否支持多种消息中间件       | 支持RabbitMQ、Kafka，可屏蔽消息中间件差异                    | 仅支持RabbitMQ                                         |
| 概念模型                     | 有自己的概念模型：binder、binding、input、output、consumer group等，用户需要理解这些概念，以及它们和RabbitMQ原生概念之间的关系 | 基本和RabbitMQ原生概念模型一致：exchange、队列、绑定等 |
| 框架能力                     | **AMQP可以做的，有些是Stream做不了的吗？**                   | **Stream可以做的，AMQP都可以做？**                     |
|                              |                                                              |                                                        |

6. Spring Cloud Stream 的运行机制

   问题：

   1. 什么叫运行机制？

      答：代码逻辑？内部逻辑？

   2. 为什么要了解运行机制？

   3. 了解他的运行机制有意义么？还是只要从使用手册中找到能解决问题的配置项就好？

   4. 入口在哪里？

   5. 启动时都做了什么？

   6. 调用链是什么？

   7. 涉及了哪些类哪些方法，它们的职责各是什么？

   思路：

   1. 看网上是否有现成的资料，有人分析过
      1. 查看官方文档
      2. 搜索引擎搜索
   2. 通过源码分析，逐个击破



进展：

1. 删除模块队列，是否会自动重建

   - inetwork、icinder、icompute不会。
   - ihypervisor、iregion会，这两个模块是使用spring-boot集成的，ihypervisor基于spring-boot 2.x版本，iregion基于spring-boot 1.x版本。

2. 先解决队列重建的问题。

   1. 思路一：spring-cloud-stream替换为直接使用spring-boot集成。

3. 队列是谁创建的？

   1. rabbit手动删除队列之后，inetwork就开始报那个错。
   2. inetwork重启之后，就会重建队列，是不是说明是inetwork启动的时候声明的队列？

4. 队列为什么会被删除？

   1. 队列已做持久化。
   2. 为了队列中的消息还能找回来，队列不被删除才是更重要的一个问题。

5. 网络的队列持久化了吗？

   答：持久化了，重启了rabbit之后，队列还在。

6. 消息持久化了吗？

   答：看ibase生产者的代码，并没有声明消息是否持久化，那应该就是使用的默认配置。

7. 项目当前集成RabbitMq的方案

   答：`Spring Cloud Stream`。实际上，各模块交互的对象并不是`RabbitMq`，而是`Spring Cloud Stream`。

8. Spring Cloud Stream 是怎么创建队列的？
   - 启动服务时创建的。
   - 调用链：
     - org.springframework.cloud.spring-cloud-starter-stream-rabbit（inetwork的直接依赖）
     - org.springframework.cloud.spring-cloud-stream-binder-rabbit
     - org.springframework.cloud.spring-cloud-stream-binder-rabbit-core
       - RabbitExchangeQueueProvisioner.provisionConsumerDestination()
     - org.springframework.amqp.spring-rabbit
       - RabbitAdmin.declareQueue()
     - com.rabbitmq:amqp-client
       - ChannelN.queueDeclare()
9. SpringBoot 是怎么创建队列的？
   - 调用链
     - org.springframework.boot.spring-boot-starter-amqp（iregion的直接依赖）
     - org.springframework.amqp.spring-rabbit
       - PublisherCallbackChannelImpl.queueDeclare()
     - com.rabbitmq:amqp-client
       - ChannelN.queueDeclare()
10. SpringBoot 手动删除队列后，队列是怎么重建的？
    - 调用链
      - org.springframework.amqp.spring-rabbit（iregion的直接依赖）
        - SimpleMessageListenerContainer.AsyncMessageProcessingConsumer.run()
    - 逻辑
      - 消费者从队列中循环拉取消息；当它发现队列被删除后，会抛出异常；异常会触发重启消费者方法被调用，而重启消费者方法中包含重建队列逻辑。
    - 注意
      - 按照如上逻辑，只有有消费者的队列被删除后，才会自动重建。



参考资料：

1. [Spring Cloud Stream 集成 RabbitMQ](https://blog.csdn.net/a767815662/article/details/110790033)
2. [Spring Integration 简介](https://www.tony-bro.com/posts/1578338213/index.html)
3. [Spring AMQP 源码分析 - MessageListener](https://www.cnblogs.com/gordonkong/p/7115155.html)





**上周周报**01：

问题：机房交换机断电后恢复，CMP各模块自动重新拉起，inetwork、icinder队列丢失。

当前进展：
1. 从Rabbit后台手动删除队列，可重现事故场景。
2. 确认项目集成RabbitMq，是使用的Spring Cloud Stream，而不是直接与RabbitMq通信。（Spring Cloud Stream是Spring Cloud生态的一个消息中间件统一解决方案）
3. 测试删除inetwork、icompute队列，无法自动重建队列。
4. 测试删除ihypervisor、iregion队列，客户端会自动重建队列。这两个模块是使用spring-boot集成的，ihypervisor基于spring-boot 2.x版本，iregion基于spring-boot 1.x版本。

下一步计划：
1. 调研已持久化的队列被删除的可能原因。
2. 调研Spring Cloud Stream，寻找队列删除后，不重启服务可重新声明队列的方法。



**上周周报02：**

问题：机房交换机断电后恢复，CMP各模块自动重新拉起，inetwork、icinder队列丢失。

当前进展：
1. 已确认可以通过将消息中间件框架，由 Spring-Cloud-Stream 切换为 Spring-Amqp 解决。
2. 通过阅读源码，已确认 Spring-Cloud-Stream 和  Spring-Amqp 框架启动服务时队列创建的调用链。
3. 通过阅读源码，已确认 Spring-Amqp 框架，在对了别删除时，客户端重建队列的调用链。
4. 目前正在调研 Spring-Cloud-Stream 框架的运行机制，以及队列删除后客户端没有自动重建的原因。

下一步计划：

1. 调研 Spring-Cloud-Stream 框架的运行机制，确认他是否真的不可以重建队列。

