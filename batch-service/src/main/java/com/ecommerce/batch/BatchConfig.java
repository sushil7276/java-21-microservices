package com.ecommerce.batch;

import org.springframework.boot.batch.autoconfigure.BatchTaskExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@Configuration
public class BatchConfig {

    /**
     * By default Spring Batch runs a job in the thread that started it, so the HTTP request that starts an import
     * would wait until the whole file is processed. Spring Boot's job launcher uses the TaskExecutor marked with
     * @BatchTaskExecutor instead: the job runs in its OWN thread, "start" returns at once with the execution id,
     * and the website polls for the progress.
     */
    @Bean
    @BatchTaskExecutor
    TaskExecutor batchTaskExecutor() {
        return new SimpleAsyncTaskExecutor("batch-job-");
    }
}
