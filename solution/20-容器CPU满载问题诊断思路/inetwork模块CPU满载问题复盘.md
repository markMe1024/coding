1. **问题现象**

   k8s给inetwork pod分配2核cpu，问题产生时使用kubectl top命令可以看到inetwork pod的cpu已经被吃满，页面网络模块功能响应迟缓，并产生API调用超时问题。

2. **产生原因**

   1. 自定义的ThreadUtils工具类，存在一个while(true)轮询方法，用来等待全部入参Future线程执行完毕。
   2. 查询port列表方法，会为每个port开启一个新线程，同时每个新线程内部会继续开启多线程查询port绑定资源。这样产生的结果是一次和port列表相关查询，会调用较多次步骤1中的while(true)方法。最终CPU被这些while(true)吃满。
   3. 查询port详情接口，使用net.sf.json.JSONObject对neutron返回数据进行转换，相对于阿里巴巴的fastjson，该方法比较吃CPU和内存资源。
   4. 步骤二中一次port列表查询，会触发多次neutron的port详情查询，当压力上来时，neutron-server达到负载上限，这样就导致在出现该问题时，其他涉及底层操作如分配浮动IP，响应迟缓。

3. **解决方案**

   1. 更新ThreadUtils工具类的while(true)轮询方法，改用java8提供的CompletableFuture，用于阻塞主线程，等待多个需要拿到结果的子线程执行完毕。
   2. 优化port列表查询方法，不再使用多线程并发处理。由icompute、ibaremetal、inetwork提供根据资源mor值列表查询虚机、云物理机、浮动IP、路由器等资源接口，先一次性查询出相关资源，然后再进行循环组装。
   3. 将sdk中port类的net.sf.json替换为com.alibaba.fastjson。
   4. 重启neutron-server释放CPU负载。