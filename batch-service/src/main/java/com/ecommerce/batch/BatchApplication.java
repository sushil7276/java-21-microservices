package com.ecommerce.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SPRING BATCH SERVICE
 * --------------------
 * A "batch job" is work that is done in bulk, often on a schedule, not while a customer waits for a page:
 * import 5,000 products from a file, add up yesterday's sales, delete old data.
 * Spring Batch gives such jobs a standard structure, remembers every run (status, counts, errors) and lets a failed
 * job be understood and re-run.
 *
 * This service has three jobs (read them in this order):
 *   1. imports/  importProductsJob       CSV file -> products           (chunk-oriented: read, check, write 50 at a time)
 *   2. report/   dailySalesReportJob     paid orders of one day -> one report row   (chunk-oriented, aggregates)
 *   3. cleanup/  cleanupJob              delete old carts, audit events and uploaded files   (three simple tasklets)
 *
 * It talks to the other services only through their APIs (client/), never to their databases.
 * Its own database (batch_db) holds the job history and the reports.
 */
@SpringBootApplication
@EnableScheduling
public class BatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchApplication.class, args);
    }
}
