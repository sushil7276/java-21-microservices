package com.ecommerce.batch.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ecommerce.batch.report.SalesReportRepository;
import com.ecommerce.batch.report.SalesReportRepository.DailyReport;
import com.ecommerce.batch.service.JobService;
import com.ecommerce.batch.exception.ForbiddenException;
import com.ecommerce.batch.web.Headers;

/**
 * The API of the batch service. Everything here is for the ADMIN only (the gateway also enforces this; the check
 * below is a second lock, like in the other services).
 *
 * Starting a job answers "202 Accepted" at once with an executionId; poll GET /api/batch/jobs/{executionId} for progress.
 */
@RestController
public class BatchController {

    private final JobService jobs;
    private final SalesReportRepository reports;

    public BatchController(JobService jobs, SalesReportRepository reports) {
        this.jobs = jobs;
        this.reports = reports;
    }

    // ---- job 1: import products from a CSV file

    @PostMapping("/api/batch/imports/products")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Long> importProducts(@RequestHeader(Headers.USER_ROLE) String role, @RequestParam("file") MultipartFile file) {
        requireAdmin(role);
        return Map.of("executionId", jobs.startImport(file));
    }

    // ---- job 2: daily sales report

    @PostMapping("/api/batch/reports/sales/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Long> runSalesReport(@RequestHeader(Headers.USER_ROLE) String role,
                                            @RequestParam(required = false) LocalDate date) {
        requireAdmin(role);
        return Map.of("executionId", jobs.startSalesReport(date == null ? jobs.yesterday() : date));
    }

    @GetMapping("/api/batch/reports/sales")
    public List<DailyReport> salesReports(@RequestHeader(Headers.USER_ROLE) String role, @RequestParam(defaultValue = "30") int days) {
        requireAdmin(role);
        return reports.latest(Math.min(Math.max(days, 1), 365));
    }

    // ---- job 3: clean-up

    @PostMapping("/api/batch/cleanup/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Long> runCleanup(@RequestHeader(Headers.USER_ROLE) String role,
                                        @RequestParam(required = false) Integer cartDays,
                                        @RequestParam(required = false) Integer auditDays,
                                        @RequestParam(required = false) Integer fileDays) {
        requireAdmin(role);
        return Map.of("executionId", jobs.startCleanup(cartDays, auditDays, fileDays));
    }

    // ---- the history of all jobs

    @GetMapping("/api/batch/jobs")
    public List<JobSummary> recentJobs(@RequestHeader(Headers.USER_ROLE) String role, @RequestParam(defaultValue = "20") int limit) {
        requireAdmin(role);
        return jobs.recent(Math.min(Math.max(limit, 1), 100));
    }

    @GetMapping("/api/batch/jobs/{executionId}")
    public JobSummary job(@RequestHeader(Headers.USER_ROLE) String role, @PathVariable long executionId) {
        requireAdmin(role);
        return jobs.get(executionId);
    }

    private static void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ForbiddenException("Admin only");
        }
    }
}
