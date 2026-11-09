package com.examprep.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Bounded executors for document extraction and study package generation. Both jobs are
 * long running and must never block a browser request.
 */
@Configuration
@Profile("!test")
public class AsyncConfig {

    @Bean(name = "documentTaskExecutor")
    public Executor documentTaskExecutor() {
        return buildExecutor("document-", 2, 4, 100);
    }

    @Bean(name = "studyTaskExecutor")
    public Executor studyTaskExecutor() {
        return buildExecutor("study-", 2, 4, 100);
    }

    private ThreadPoolTaskExecutor buildExecutor(String prefix, int core, int max, int queue) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(core);
        executor.setMaxPoolSize(max);
        executor.setQueueCapacity(queue);
        executor.setThreadNamePrefix(prefix);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
