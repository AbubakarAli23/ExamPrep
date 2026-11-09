package com.examprep.support;

import java.util.concurrent.Executor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Production async executors are disabled under the "test" profile so tests run with this
 * bounded pool and can observe genuine background behaviour.
 */
@TestConfiguration
public class TestAsyncConfig {

    @Bean(name = "documentTaskExecutor")
    public Executor documentTaskExecutor() {
        return executor("test-document-");
    }

    @Bean(name = "studyTaskExecutor")
    public Executor studyTaskExecutor() {
        return executor("test-study-");
    }

    private ThreadPoolTaskExecutor executor(String prefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix(prefix);
        executor.initialize();
        return executor;
    }
}
