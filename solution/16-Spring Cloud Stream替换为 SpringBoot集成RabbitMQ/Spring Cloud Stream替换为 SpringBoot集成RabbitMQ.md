> Spring Cloud Stream替换为SpringBoot集成RabbitMQ

1. 替换依赖

   ```xml
   <dependency>
       <groupId>org.springframework.cloud</groupId>
       <artifactId>spring-cloud-starter-stream-rabbit</artifactId>
   </dependency>
   ```

   替换为

   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-amqp</artifactId>
   </dependency>
   ```

2. bootstrap.yml删除如下配置

   ```yaml
       stream:
         binders:
           defaultRabbit: # 表示定义的名称，用于binding整合
             type: rabbit
             environment:
               spring:
                 rabbitmq:
                   host: ${incloud.common.rabbitmq.host}
                   port: ${incloud.common.rabbitmq.port}
                   username: ${incloud.common.rabbitmq.username}
                   password: ${incloud.common.rabbitmq.password}
                 virtual-host: /
         bindings:
           inputVdc:      # 监听ibase模块创建vdc的消息
             binder: defaultRabbit
             destination: ibaseVdcExchange  # exchange名称：ibaseVdcExchange
             group: inetworkVdc_${incloudConfigLabel:regionOne} # 队列名称
             content-type: application/json
     #如果连接到远程RabbitMQ报Rabbit health check failed异常
     #配置以下信息
     #因为默认会尝试连接localhost:5672
     rabbitmq:
       host: ${incloud.common.rabbitmq.host}
       port: ${incloud.common.rabbitmq.port}
       username: ${incloud.common.rabbitmq.username}
       password: ${incloud.common.rabbitmq.password}
   ```

3. application.yml更新配置

   ```yml
    cloud:
       stream:
         binders:
           defaultRabbit: # 表示定义的名称，用于于binding整合
             environment:
               spring:
                 rabbitmq:
                   host: ${incloud.inetwork.rabbitmq.host}
                   port: ${incloud.inetwork.rabbitmq.port}
                   username: ${incloud.inetwork.rabbitmq.username}
                   password: ${incloud.inetwork.rabbitmq.password}
   ```

   更新为

   ```yml
   spring:
     rabbitmq:
       host: ${incloud.inetwork.rabbitmq.host}
       port: ${incloud.inetwork.rabbitmq.port}
       username: ${incloud.inetwork.rabbitmq.username}
       password: ${incloud.inetwork.rabbitmq.password}
   ```

4. 编写配置类

   ```java
   package com.inspur.incloud.inetwork.config;
   
   import org.springframework.amqp.core.AmqpAdmin;
   import org.springframework.amqp.core.Binding;
   import org.springframework.amqp.core.BindingBuilder;
   import org.springframework.amqp.core.Exchange;
   import org.springframework.amqp.core.ExchangeBuilder;
   import org.springframework.amqp.core.Queue;
   import org.springframework.amqp.core.QueueBuilder;
   import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
   import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
   import org.springframework.amqp.rabbit.connection.ConnectionFactory;
   import org.springframework.amqp.rabbit.core.RabbitAdmin;
   import org.springframework.amqp.rabbit.core.RabbitTemplate;
   import org.springframework.beans.factory.annotation.Autowired;
   import org.springframework.context.annotation.Bean;
   import org.springframework.context.annotation.Configuration;
   import org.springframework.core.env.Environment;
   
   /**
    * RabbitMQ配置类
    * @author mark
    * @date 2023/2/6 15:56
    */
   @Configuration
   public class RabbitMqConfig {
   
       @Autowired
       private Environment env;
   
       public static final String vdcQueueName = "ibaseVdcExchange.inetworkVdc_regions";
   
       private static final String vdcExchangeName = "ibaseVdcExchange";
   
       @Bean
       public Queue vdcQueue() {
           return QueueBuilder.durable(vdcQueueName).build();
       }
   
       @Bean
       public Exchange vdcExchange() {
           return ExchangeBuilder.topicExchange(vdcExchangeName).durable(true).build();
       }
   
       @Bean
       public Binding vdcBinding() {
           return BindingBuilder.bind(vdcQueue()).to(vdcExchange()).with("#").noargs();
       }
   
       @Bean
       public ConnectionFactory connectionFactory() {
           final String host = env.getProperty("spring.rabbitmq.host");
           final Integer port = env.getProperty("spring.rabbitmq.port", Integer.class);
           final String username = env.getProperty("spring.rabbitmq.username");
           final String password = env.getProperty("spring.rabbitmq.password");
   
           CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host, port);
           connectionFactory.setUsername(username);
           connectionFactory.setPassword(password);
           connectionFactory.setVirtualHost("/");
   
           return connectionFactory;
       }
   
       @Bean
       public RabbitTemplate rabbitTemplate() {
           return new RabbitTemplate(connectionFactory());
       }
   
       @Bean
       public SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory() {
           SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
           factory.setConnectionFactory(connectionFactory());
           return factory;
       }
   
   }
   ```
   
5. 更新消费者方法注解和入参

   ```java
   @StreamListener(IVdcMsgProcessor.INPUT_VDC)
   public void receiveAndHandleVdcMsg(Message<String> msg) {}
   ```

   更新为

   ```java
   @RabbitListener(queues = RabbitMqConfig.vdcQueueName)
   public void receiveAndHandleVdcMsg(String msg) {}
   ```