package com.ecommerce.batch.schedule;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ecommerce.batch.service.JobService;

/**
 * The nightly schedule (times are in application.yml). Turn it off with batch.schedule.enabled=false.
 * Note: with TWO batch-service instances both would start the job; run only one instance (or add a lock, e.g. in Redis).
 */
@Component
@ConditionalOnProperty(name = "batch.schedule.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobs.class);

    private final JobService jobs;

    public ScheduledJobs(JobService jobs) {
        this.jobs = jobs;
    }

    @Scheduled(cron = "${batch.sales-report.cron}", zone = "${batch.sales-report.zone}")
    void dailySalesReport() {
        LocalDate day = jobs.yesterday();
        log.info("Schedule: starting the sales report for {}", day);
        try {
            jobs.startSalesReport(day);
        } catch (RuntimeException e) {
            log.error("Scheduled sales report failed to start: {}", e.toString());
        }
    }

    @Scheduled(cron = "${batch.cleanup.cron}", zone = "${batch.sales-report.zone}")
    void cleanup() {
        log.info("Schedule: starting the clean-up");
        try {
            jobs.startCleanup(null, null, null);
        } catch (RuntimeException e) {
            log.error("Scheduled clean-up failed to start: {}", e.toString());
        }
    }
}
