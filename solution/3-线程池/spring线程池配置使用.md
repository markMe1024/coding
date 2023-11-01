# 一、配置

```java
package com.inspur.incloud.inetwork.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * SpringBoot线程池配置类
 * @author mark
 * @date 2021/5/3 16:10
 */
@Configuration
public class SpringBootThreadPoolConfiguration {

    /**
     * 通用线程池
     */
    @Bean("commonExecutor")
    public ThreadPoolTaskExecutor commonExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程：核心线程一直存活
        executor.setCorePoolSize(10);
        // 最大线程
        executor.setMaxPoolSize(50);
        // 闲置线程存活时长
        executor.setKeepAliveSeconds(60);
        // 队列容量：任务将队列塞满之后，扩展核心线程，线程总数最多不超过最大线程数
        executor.setQueueCapacity(100);
        // 拒绝策略：任务超出队列容量后，新任务交还主线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 线程名前缀
        executor.setThreadNamePrefix("common-executor-");
        // 初始化线程池
        executor.initialize();
        return executor;
    }

    /**
     * 耗时较长的任务使用该线程池
     */
    @Bean("expensiveTaskExecutor")
    public ThreadPoolTaskExecutor expensiveTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程：核心线程一直存活
        executor.setCorePoolSize(0);
        // 最大线程
        executor.setMaxPoolSize(50);
        // 闲置线程存活时长
        executor.setKeepAliveSeconds(60);
        // 队列容量：任务将队列塞满之后，扩展核心线程，线程总数最多不超过最大线程数
        executor.setQueueCapacity(0);
        // 拒绝策略：任务超出队列容量后，新任务交还主线程处理
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 线程名前缀
        executor.setThreadNamePrefix("expensive-task-executor-");
        // 初始化线程池
        executor.initialize();
        return executor;
    }

}
```



# 二、使用

```java
	// 注入
	@Autowired
    private ThreadPoolTaskExecutor expensiveTaskExecutor;

	// 直接用
	expensiveTaskExecutor.execute(() -> {
        // your task
    });
```

