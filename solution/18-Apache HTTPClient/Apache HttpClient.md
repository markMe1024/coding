> 调研替换已被Apache HttpClient废弃的DefaultHttpClient的必要性



1. 为什么DefaultHttpClient被废弃？

   答：目前从网上看到的结论，没有人解释为什么被废弃， 大家更关注于废弃之后的替代方案。

2. 源码解析

   1. 问题

      1. DefaultHttpClient

         1. 实例化

            方案：使用构造器，初始化ClientConnectionManager（连接池）和HttpParams（Http参数，如连接超时、响应超时）两个属性。

            缺点：功能是不是太简单了？

      2. InternalHttpClient

         1. 实例化

            答：使用HttpClientBuilder的build方法。

      3. DefaultHttpClient可能并没有漏洞，它也继承自CloseableHttpClient，更大的可能性，是HttpClientBuilder.build() 的方式，拥有更丰富的能力。那么这些丰富的能力，是指什么呢？这些丰富的能力对我们有什么意义吗？

      4. 是不是要通过阅读build方法和doExecute方法的源码，会更容易懂。

   2. 获取到的信息

      1. DefaultHttpClient继承了CloseableHttpClient抽象类，实现了HttpClient接口。DefaultHttpClient是4.3版本废弃的，但也是4.3版本继承的CloseableHttpClient。这可能是HttpClient为了具有向后兼容性吧。
      2. HttpClient接口中，定义了多个execute方法。

3. 不使用DefaultHttpClient的理由

   1. 随着HttpClient的版本升级，DefaultHttpClient不会随之有新特性更新。

4. 使用Apache HttpClient替换Java原生调用的理由

   1. Apache HttpClient有线程池。
   2. Apache HttpClient有重试机制。



**上周周报：**

问题：调研替换已被Apache HttpClient废弃的DefaultHttpClient的必要性。

当前进展：
1. 通过搜索引擎查询，目前没有发现有人解释它被废弃的原因，大家更关注于废弃之后的替代方案。
2. 初步阅读源码，发现DefaultHttpClient仅支持设置连接池和连接参数两个参数。与此相比，4.3版本以后推荐的创建HttpClient的方式，会多一些功能，比如重试机制。
3. 寻找到不再使用DefaultHttpClient的一个理由：随着HttpClient的版本升级，DefaultHttpClient不会随之有新特性更新。

下一步计划：

1. 继续深入原发，调研4.3版本后推荐的方式，有哪些优势。