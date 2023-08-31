`mysql-binlog-connector`组件用于实时接收并解析Binlog日志，支持订阅部分库部分表。



集成步骤如下：

1. pom.xml中添加依赖

   ```xml
   <dependency>
   	<groupId>com.inspur.incloudmanager</groupId>
   	<artifactId>mysql-binlog-connector</artifactId>
   	<version>1.0-SNAPSHOT</version>
   </dependency>
   ```

   

2. application.yml中添加配置

   ```yml
   binlog:
     datasource:
       url: 100.2.216.244:3306
       username: root
       password: 123456
     dbTables: '{"iresource": "t_net_net, t_net_subnet", "ibase": "t_ba_vdc"}'
   ```

   

   参数释义

   | 参数     | 含义                       |
   | :------- | :------------------------- |
   | url      | 数据库访问地址             |
   | username | 数据库用户名               |
   | password | 数据库密码                 |
   | dbTables | 订阅的部分库和表，JSON结构 |

   

   数据库用户，需要被赋予Binlog读取权限，赋权SQL：

   ```sql
   GRANT REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO '用户名'@'%';
   ```

   数据库用户，需要被赋予`dbTables`订阅的库的读权限，赋权SQL：

   ```sql
   GRANT select ON 库名.* TO '用户名'@'%';
   ```



3. 新建`BinlogEventListener`继承`EventListener`实现`CommandLineRunner`，集成代码：

   ```sql
   package com.inspur.incloud.inetwork.service;
   
   import com.inspur.incloud.binlog.listener.EventListener;
   import com.inspur.incloud.binlog.model.BinLogItem;
   import org.springframework.boot.CommandLineRunner;
   import org.springframework.core.annotation.Order;
   import org.springframework.stereotype.Component;
   
   @Component
   @Order(value = 1)
   public class BinlogEventListener extends EventListener implements CommandLineRunner {
   
       @Override
       public void run(String... args) throws Exception {
           super.start();
       }
   
       @Override
       public void consume(BinLogItem item) {
           // 消费解析后的Binlog日志
       }
   }
   ```

   实现接口`CommandLineRunner`的作用，是在Spring项目启动之后，自动执行`run`方法。

   `run`方法内会调用`mysql-binlog-connector`组件的`start`方法，开始接收并解析Binlog日志。

   `consume`方法用于接收解析后的Binlog日志，消费者在这个方法内实现自己的业务逻辑。