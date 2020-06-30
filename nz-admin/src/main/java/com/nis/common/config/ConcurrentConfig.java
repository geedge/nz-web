package com.nis.common.config;

import java.util.concurrent.Executor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class ConcurrentConfig implements AsyncConfigurer {
	 @Value("${nezha.corePoolSize}")
     private Integer corePoolSize;
	 
	 @Value("${nezha.maxPoolSize}")
	 private Integer maxPoolSize;
	 
	 @Value("${nezha.queueCapacity}")
	 private Integer queueCapacity;
	
	 // ThredPoolTaskExcutor的处理流程
     // 当池子大小小于corePoolSize，就新建线程，并处理请求
     // 当池子大小等于corePoolSize，把请求放入workQueue中，池子里的空闲线程就去workQueue中取任务并处理
     // 当workQueue放不下任务时，就新建线程入池，并处理请求，如果池子大小撑到了maximumPoolSize，就用RejectedExecutionHandler来做拒绝处理
     // 当池子的线程数大于corePoolSize时，多余的线程会等待keepAliveTime长时间，如果无请求可处理就自行销毁
     @Override
     public Executor getAsyncExecutor() {
//    	 int availableProcessors = Runtime.getRuntime().availableProcessors();
         ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
         executor.setCorePoolSize(corePoolSize);
         executor.setMaxPoolSize(maxPoolSize);
         executor.setQueueCapacity(queueCapacity);
         executor.initialize();
         return executor;
     }
 
     @Override
     public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
         return null;
     }
	 
	
}
