1. MySQL的BinLog
   - 概念：BinLog存储一系列的事件，用于记录数据库更新。
   
   - 模式：statement、row、mixed。
   
   - 事件：
   
     - BinLog中存储的内容称之为事件，每一个数据库更新操作(Insert、Update、Delete，**不包括Select**)等都对应一个事件。
     - row模式下，BinLog中主要包括 `TABLE_MAP_EVENT`、增、删、改事件。
     - 通过`show binlog events`命令可查看BinLog日志。
   
   - 其他特征：
   
     - 存在多个BinLog文件，可通过`show binary logs` 命令查看。
     - 业务模块读取BinLog文件之前，需要先给该用户赋予`Repl_slave_priv`和`Repl_client_priv`权限。
   
   - 参考文献：
   
     - [MySQL :: MySQL 8.0 Reference Manual :: 5.4.4 The Binary Log](https://dev.mysql.com/doc/refman/8.0/en/binary-log.html) 
   
     - [MySQL binlog原来可以这样用？各种场景和原理剖析！ - 墨天轮 (modb.pro)](https://www.modb.pro/db/55740) 
   
   
   
2. canal

   - 概念：阿里开源的项目，基于MySQL的BinLog，提供增量数据订阅和消费。

   - 原理：模拟MySQL slave的交换协议，将自己伪装成slave，从master获取BinLog然后解析。

   - 优点：相对稳定，经过阿里复杂场景以及高并发的验证。

   - 缺点：运维成本较高，需要单独部署canal后端服务，且HA部署依赖zookeeper。

   - 问题：

     - 部署一个canal服务端，可以多个客户端拿到同一份数据吗？

   - 参考文献：

     - [Canal监听mysql的binlog日志实现数据同步](https://blog.csdn.net/m0_37583655/article/details/119517336) 
     - [Java订阅Binlog的几种方式 ](https://jasonkayzk.github.io/2023/03/26/Java订阅Binlog的几种方式/)

   

3. mysql-binlog-connector

   - 概念：jar包，用来连接MySQL并获取BinLog。
   - 原理：同canal，模拟MySQL slave的交换协议，将自己伪装成slave，从master获取binary log然后解析。
   - 优点：项目内引入依赖就可以集成。自动重连、实时获取事件更新。
   - 缺点：如果多个业务模块需要订阅binlog日志，则会产生多个slave，进而给master带来更多的网络开销。
   - 特性：
     - 建立连接后，从最新BinLog文件的最后位置开始解析。也支持指定BinLog文件指定位置。
     - 不支持订阅部分数据库、表的事件，参考社区回答： [How to get events for specified set of tables · Issue #132 ](https://github.com/shyiko/mysql-binlog-connector-java/issues/132) 
   - 问题
     - iresource-common多副本会怎样？
       1. 会产生多个slave。
       2. 每个副本会订阅到同一份binlog日志。
       3. 每个副本解析到符合条件的binlog事件之后，推送消息给各自的客户端。
   - 待办
     - 写下ppt
     - 配置文件改下，不能写死了
     - 代码放到iresource-common里
     - iresource-common账号赋读取binlog的权限：`Repl_slave_priv`，`Repl_client_priv`。
     - 配置线程池，多线程消费符合条件的BinLog的event
     - 是否需要在服务重启后，从上一次关闭时的binlog位置继续处理
   - 参考文献：
     - [Java监听mysql的binlog详解(mysql-binlog-connector)](https://blog.csdn.net/m0_37583655/article/details/119148470) 
     - [GitHub - shyiko/mysql-binlog-connector-java: MySQL Binary Log connector](https://github.com/shyiko/mysql-binlog-connector-java) 
