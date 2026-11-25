package com.ecommerce.batch.imports;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import com.ecommerce.batch.client.ProductClient;

/**
 * JOB 1: IMPORT PRODUCTS FROM A CSV FILE
 * --------------------------------------
 *   Job  importProductsJob
 *    └─ Step importProductsStep   (chunk-oriented, 50 lines per chunk)
 *         reader     read one line of the file              -> ProductCsvRow   (text)
 *         processor  check it, convert text to numbers      -> ImportItem      (or skip the line with a reason)
 *         writer     send the 50 checked products at once   -> product-service
 *
 * After every chunk Spring Batch COMMITS its progress to batch_db. If the service stops in the middle, the work
 * already done is not lost and the job history shows exactly how far it got.
 *
 * Bad lines never stop the import: they are "skipped", and listed with the reason (rejected_row table).
 * Job parameter: file = full path of the uploaded CSV file.
 */
@Configuration
public class ImportProductsJobConfig {

    public static final String JOB_NAME = "importProductsJob";
    private static final int CHUNK_SIZE = 50;
    /** More refused lines than this and the job is marked FAILED (a file that bad is probably the wrong file). */
    private static final int MAX_BAD_LINES = 10_000;

    @Bean(name = JOB_NAME)
    Job importProductsJob(JobRepository jobRepository, Step importProductsStep) {
        return new JobBuilder(JOB_NAME, jobRepository).start(importProductsStep).build();
    }

    @Bean
    Step importProductsStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                            FlatFileItemReader<ProductCsvRow> productCsvReader, ProductRowProcessor processor,
                            ProductBulkWriter productBulkWriter, RejectedRowListener rejectedRowListener) {
        return new StepBuilder("importProductsStep", jobRepository)
                .<ProductCsvRow, ImportItem>chunk(CHUNK_SIZE, transactionManager)
                .reader(productCsvReader)
                .processor(processor)
                .writer(productBulkWriter)
                .faultTolerant()                                  // "keep going when a line is bad"
                .skip(FlatFileParseException.class)               // line has the wrong number of columns
                .skip(InvalidRowException.class)                  // line failed the checks in the processor
                .skipLimit(MAX_BAD_LINES)
                .listener(rejectedRowListener)                    // saves every skipped line with its reason
                .build();
    }

    // The three beans below are @StepScope: a NEW instance is created for every run of the step, so each import
    // gets its own file name, its own execution id and its own totals. (The values come from the job parameters.)

    @Bean
    @StepScope
    FlatFileItemReader<ProductCsvRow> productCsvReader(@Value("#{jobParameters['file']}") String file) {
        FlatFileItemReader<ProductCsvRow> reader = new FlatFileItemReader<>(new FileSystemResource(file), new ProductCsvLineMapper());
        reader.setName("productCsvReader");
        reader.setLinesToSkip(1);                                 // line 1 is the header (name,description,price,...)
        reader.setEncoding("UTF-8");
        return reader;
    }

    @Bean
    @StepScope
    ProductBulkWriter productBulkWriter(ProductClient products, RejectedRowRepository rejected,
                                        @Value("#{stepExecution}") StepExecution stepExecution) {
        return new ProductBulkWriter(products, rejected, stepExecution);
    }

    @Bean
    @StepScope
    RejectedRowListener rejectedRowListener(RejectedRowRepository rejected,
                                            @Value("#{stepExecution.jobExecutionId}") Long jobExecutionId) {
        return new RejectedRowListener(rejected, jobExecutionId);
    }
}
