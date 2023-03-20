**命令：**

1. 启动
   1. java -jar arthas-boot.jar：启动服务端，同时开启一个客户端连接到该服务端
   2. java -jar arthas-client.jar：启动客户端
2. jvm
   1. dashboard：当前系统实时数据面板
   2. heapdump： dump java heap 
   3. jvm：查看jvm信息
   4. logger：查看日志信息、更新日志级别
   5. thread：查看线程堆栈信息
   6. sysenv：查看JVM环境变量
   7. sysprop：查看和修改JVM的系统属性
   8. vmoption：查看和修改JVM里诊断相关的option
   9. vmtool：查询内存对象、强制GC
3. class/classloader相关
   1. jad：反编译
   2. mc：内存编译器
   3. retransform：retransform jvm 已加载的 class
   4. sc：查找JVM中已加载的类
   5. sm：查找JVM中已加载的类的方法
4. monitor/watch/trace相关
   1. monitor：监控方法的调用次数、成功/失败次数、平均rt（response time）、失败率
   2. stack：查看方法的调用栈
   3. trace：查看方法内部调用路径，并输出方法路径上每个节点上的耗时
   4. tt：方法执行数据的时空隧道（TimeTunnel），记录下方法每次调用的入参、返回值和异常信息，可以后面再观测，甚至由arthas再次触发调用
   5. watch：查看方法的入参、返回值和异常信息
5. 火焰图
   1. profiler：比如生成一段时间cpu的火焰图。
   2. jfr：Java Flight Recorder，一种用于收集相关正在运行的Java应用程序的诊断和分析数据的工具。
6. 鉴权
   1. auth
7. arthas配置
   1. options：查看或设置Arthas全局开关
8. 任务
   1. &：后台执行任务
   2. jobs：查看后台任务
   3. fg：将命令转到前台
   4. kill：停止后台任务
   5. ctrl + c：停止前台任务
   6. \>和&结合使用：将任务输入结果重定向到指定文件中
9. 基础命令
   1. reset：重置增强类，但是retransform过的类不会重置
   2. tee：读取标准输入的数据，并将其内容输出成文件



**其他：**

1. *：通配符
2. -x：表示扩展深度，可以用来调整入参或返回值的展示内容，默认值为1，最大置为4
3. 日志目录：~/logs/arthas/
4. 支持远程telnet连接，还可以设置用户名、密码
