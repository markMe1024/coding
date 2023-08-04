**jstat**

JVM Statistics Monitoring Tool，JDK自带的一个监控工具，可以监控JVM的类加载、GC、内存、即时编译等信息。

但是它与Arthas相比，看起来还是更费劲一点儿。



**jstatd**

JVM的jstat守护进程，主要用于监控JVM的创建和终止，并提供一个接口允许远程监控工具依附到本地主机上运行的JVM。

VisualVM工具想要连上远程的JVM，则需要给该JVM开启jstatd权限，这是个前提。



参考资料：

- [JVM远程监控工具-jstatd](https://blog.csdn.net/huanqingdong/article/details/104095402/)