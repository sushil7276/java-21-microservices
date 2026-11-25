package com.ecommerce.batch.cleanup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.ecommerce.batch.client.AuditClient;
import com.ecommerce.batch.client.CartClient;

/**
 * JOB 3: CLEAN-UP
 * ---------------
 *   Job  cleanupJob
 *    ├─ Step cleanCartsStep    delete carts nobody changed for N days            (cart-service, Redis)
 *    ├─ Step cleanAuditStep    delete audit events older than N days             (audit-service, MongoDB)
 *    └─ Step cleanFilesStep    delete uploaded CSV files older than N days       (this service's upload folder)
 *
 * Each step is a TASKLET: "do this one thing once" (no reader / processor / writer, no chunks). Use a tasklet when
 * the work is a single action; use chunks (jobs 1 and 2) when there are many items to process one by one.
 * If a step fails the job stops there (the steps before it already did their work) and the history shows which step failed.
 *
 * Job parameters (all whole numbers): cartDays, auditDays, fileDays.
 */
@Configuration
public class CleanupJobConfig {

    public static final String JOB_NAME = "cleanupJob";

    @Bean(name = JOB_NAME)
    Job cleanupJob(JobRepository jobRepository, Step cleanCartsStep, Step cleanAuditStep, Step cleanFilesStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(cleanCartsStep)
                .next(cleanAuditStep)
                .next(cleanFilesStep)
                .build();
    }

    @Bean
    Step cleanCartsStep(JobRepository jobRepository, PlatformTransactionManager tx, Tasklet cleanCartsTasklet) {
        return new StepBuilder("cleanCartsStep", jobRepository).tasklet(cleanCartsTasklet, tx).build();
    }

    @Bean
    Step cleanAuditStep(JobRepository jobRepository, PlatformTransactionManager tx, Tasklet cleanAuditTasklet) {
        return new StepBuilder("cleanAuditStep", jobRepository).tasklet(cleanAuditTasklet, tx).build();
    }

    @Bean
    Step cleanFilesStep(JobRepository jobRepository, PlatformTransactionManager tx, Tasklet cleanFilesTasklet) {
        return new StepBuilder("cleanFilesStep", jobRepository).tasklet(cleanFilesTasklet, tx).build();
    }

    @Bean
    @StepScope
    Tasklet cleanCartsTasklet(CartClient carts, @Value("#{jobParameters['cartDays']}") Long days) {
        return (contribution, chunkContext) -> {
            long deleted = carts.deleteAbandoned(days.intValue());
            record(contribution, chunkContext, deleted);
            return RepeatStatus.FINISHED;                 // FINISHED = this tasklet is done (CONTINUABLE would call it again)
        };
    }

    @Bean
    @StepScope
    Tasklet cleanAuditTasklet(AuditClient audit, @Value("#{jobParameters['auditDays']}") Long days) {
        return (contribution, chunkContext) -> {
            long deleted = audit.deleteOld(days.intValue());
            record(contribution, chunkContext, deleted);
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    @StepScope
    Tasklet cleanFilesTasklet(@Value("${batch.upload-dir}") String uploadDir, @Value("#{jobParameters['fileDays']}") Long days) {
        return (contribution, chunkContext) -> {
            Path dir = Path.of(uploadDir);
            long deleted = 0;
            if (Files.isDirectory(dir)) {
                Instant cutoff = Instant.now().minus(Duration.ofDays(days));
                try (Stream<Path> files = Files.list(dir)) {
                    for (Path file : files.filter(Files::isRegularFile).toList()) {
                        if (Files.getLastModifiedTime(file).toInstant().isBefore(cutoff)) {
                            Files.delete(file);
                            deleted++;
                        }
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Could not clean " + dir + ": " + e.getMessage(), e);
                }
            }
            record(contribution, chunkContext, deleted);
            return RepeatStatus.FINISHED;
        };
    }

    /** Remember how many things were deleted: shown in the job history (write count and the "deleted" detail). */
    private static void record(org.springframework.batch.core.step.StepContribution contribution,
                               org.springframework.batch.core.scope.context.ChunkContext chunkContext, long deleted) {
        contribution.incrementWriteCount(deleted);
        chunkContext.getStepContext().getStepExecution().getExecutionContext().putLong("deleted", deleted);
    }
}
