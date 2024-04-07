package org.codesystem.server.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfiguration implements AsyncConfigurer {

    private ThreadPoolTaskScheduler createTaskSchedulerWithPoolSizeOne() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(1);
        return threadPoolTaskScheduler;
    }

    @Bean(name = "encryptPackageTask")
    public ThreadPoolTaskScheduler threadPoolTaskSchedulerEncryptPackage() {
        return createTaskSchedulerWithPoolSizeOne();
    }

    @Bean(name = "deletePackageTask")
    public ThreadPoolTaskScheduler threadPoolTaskSchedulerDeletePackage() {
        return createTaskSchedulerWithPoolSizeOne();
    }

    @Bean(name = "deleteLogsTask")
    public ThreadPoolTaskScheduler threadPoolTaskSchedulerDeleteLogs() {
        return createTaskSchedulerWithPoolSizeOne();
    }

    @Bean(name = "collectSystemUsageTask")
    public ThreadPoolTaskScheduler threadPoolTaskSchedulerCollectSystemUsage() {
        return createTaskSchedulerWithPoolSizeOne();
    }

    @Bean(name = "deleteSystemUsageTask")
    public ThreadPoolTaskScheduler threadPoolTaskSchedulerDeleteSystemUsageEntries() {
        return createTaskSchedulerWithPoolSizeOne();
    }
}