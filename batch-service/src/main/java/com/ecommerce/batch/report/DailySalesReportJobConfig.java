package com.ecommerce.batch.report;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.ecommerce.batch.client.OrderClient;
import com.ecommerce.batch.client.OrderClient.OrderDto;

/**
 * JOB 2: DAILY SALES REPORT
 * -------------------------
 *   Job  dailySalesReportJob
 *    └─ Step salesReportStep   (chunk-oriented, 50 orders per chunk)
 *         reader     page after page of the PAID orders of one day, from order-service   -> OrderDto
 *         processor  count the pieces of each order                                      -> OrderSale
 *         writer     add up totals; when the step ends, save ONE report row              (afterStep)
 *
 * Job parameter: date = the day to report on (yyyy-MM-dd). A "day" starts and ends at midnight of the time zone
 * set in application.yml (batch.sales-report.zone). Runs by itself every night, and an admin can run it for any day.
 */
@Configuration
public class DailySalesReportJobConfig {

    public static final String JOB_NAME = "dailySalesReportJob";
    private static final int CHUNK_SIZE = 50;

    @Bean(name = JOB_NAME)
    Job dailySalesReportJob(JobRepository jobRepository, Step salesReportStep) {
        return new JobBuilder(JOB_NAME, jobRepository).start(salesReportStep).build();
    }

    @Bean
    Step salesReportStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                         PagedOrderReader ordersOfTheDay, SalesReportProcessor processor, SalesReportWriter salesReportWriter) {
        return new StepBuilder("salesReportStep", jobRepository)
                .<OrderDto, OrderSale>chunk(CHUNK_SIZE, transactionManager)
                .reader(ordersOfTheDay)
                .processor(processor)
                .writer(salesReportWriter)
                .listener((StepExecutionListener) salesReportWriter)    // so that afterStep() is called
                .build();
    }

    @Bean
    @StepScope
    PagedOrderReader ordersOfTheDay(OrderClient orders, @Value("#{jobParameters['date']}") String date,
                                    @Value("${batch.sales-report.zone}") String zone) {
        ZoneId timeZone = ZoneId.of(zone);
        LocalDate day = LocalDate.parse(date);
        return new PagedOrderReader(orders, day.atStartOfDay(timeZone).toInstant(),
                day.plusDays(1).atStartOfDay(timeZone).toInstant(), CHUNK_SIZE);
    }

    @Bean
    @StepScope
    SalesReportWriter salesReportWriter(SalesReportRepository repository, @Value("#{jobParameters['date']}") String date) {
        return new SalesReportWriter(repository, LocalDate.parse(date));
    }
}
