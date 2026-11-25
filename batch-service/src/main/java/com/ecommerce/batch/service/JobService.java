package com.ecommerce.batch.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ecommerce.batch.cleanup.CleanupJobConfig;
import com.ecommerce.batch.imports.ImportProductsJobConfig;
import com.ecommerce.batch.imports.RejectedRowRepository;
import com.ecommerce.batch.report.DailySalesReportJobConfig;
import com.ecommerce.batch.web.JobSummary;
import com.ecommerce.batch.web.JobSummary.StepSummary;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.common.exception.NotFoundException;

/**
 * Starts the jobs and describes their runs.
 *
 * Starting a job = JobOperator.start(job, parameters). Spring Batch creates a JOB INSTANCE (job + parameters) and a
 * JOB EXECUTION (one attempt) in batch_db and runs it in its own thread. The execution id is what the website
 * uses to ask "how far is it?".
 */
@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    /** Only these numbers of a step are shown to the admin (the other entries of a step context are Spring Batch internals). */
    private static final Set<String> VISIBLE_DETAILS =
            Set.of("created", "refusedByProductService", "reportDate", "orders", "itemsSold", "revenue", "deleted");

    private final JobOperator operator;
    private final JobRepository repository;
    private final RejectedRowRepository rejected;
    private final Job importJob;
    private final Job salesJob;
    private final Job cleanupJob;
    private final Path uploadDir;
    private final ZoneId zone;
    private final int cartDays;
    private final int auditDays;
    private final int fileDays;

    public JobService(JobOperator operator, JobRepository repository, RejectedRowRepository rejected,
                      @Qualifier(ImportProductsJobConfig.JOB_NAME) Job importJob,
                      @Qualifier(DailySalesReportJobConfig.JOB_NAME) Job salesJob,
                      @Qualifier(CleanupJobConfig.JOB_NAME) Job cleanupJob,
                      @Value("${batch.upload-dir}") String uploadDir,
                      @Value("${batch.sales-report.zone}") String zone,
                      @Value("${batch.cleanup.cart-days}") int cartDays,
                      @Value("${batch.cleanup.audit-days}") int auditDays,
                      @Value("${batch.cleanup.file-days}") int fileDays) {
        this.operator = operator;
        this.repository = repository;
        this.rejected = rejected;
        this.importJob = importJob;
        this.salesJob = salesJob;
        this.cleanupJob = cleanupJob;
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
        this.zone = ZoneId.of(zone);
        this.cartDays = cartDays;
        this.auditDays = auditDays;
        this.fileDays = fileDays;
    }

    // ------------------------------------------------------------------ starting jobs

    /** Saves the uploaded file and starts the import. Returns at once; the import runs in the background. */
    public long startImport(MultipartFile upload) {
        String original = upload.getOriginalFilename() == null ? "products.csv" : upload.getOriginalFilename();
        if (upload.isEmpty()) {
            throw new BadRequestException("The file is empty");
        }
        if (!original.toLowerCase().endsWith(".csv")) {
            throw new BadRequestException("Please upload a .csv file");
        }
        Path saved;
        try {
            Files.createDirectories(uploadDir);
            saved = uploadDir.resolve(UUID.randomUUID() + ".csv");       // our own name: the user's file name is never used as a path
            try (InputStream in = upload.getInputStream()) {
                Files.copy(in, saved, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not save the uploaded file: " + e.getMessage(), e);
        }
        JobParameters parameters = new JobParametersBuilder()
                .addString("file", saved.toString())
                .addString("originalName", original, false)                // false = not part of the job's identity
                .addLong("runId", System.currentTimeMillis())               // makes every run a new job instance
                .toJobParameters();
        return launch(importJob, parameters);
    }

    /**
     * Runs the report for one day. Running it again for the same day replaces that day's report.
     * For today the report only contains the orders so far; the nightly run makes the final one for the finished day.
     */
    public long startSalesReport(LocalDate date) {
        if (date.isAfter(LocalDate.now(zone))) {
            throw new BadRequestException("A report cannot be made for a future day (today is " + LocalDate.now(zone) + ")");
        }
        JobParameters parameters = new JobParametersBuilder()
                .addString("date", date.toString())
                .addLong("runId", System.currentTimeMillis())
                .toJobParameters();
        return launch(salesJob, parameters);
    }

    public LocalDate yesterday() {
        return LocalDate.now(zone).minusDays(1);
    }

    /** Runs the clean-up. Any argument that is null uses the default from application.yml. */
    public long startCleanup(Integer cartDaysOrNull, Integer auditDaysOrNull, Integer fileDaysOrNull) {
        JobParameters parameters = new JobParametersBuilder()
                .addLong("cartDays", (long) (cartDaysOrNull == null ? cartDays : cartDaysOrNull))
                .addLong("auditDays", (long) (auditDaysOrNull == null ? auditDays : auditDaysOrNull))
                .addLong("fileDays", (long) (fileDaysOrNull == null ? fileDays : fileDaysOrNull))
                .addLong("runId", System.currentTimeMillis())
                .toJobParameters();
        return launch(cleanupJob, parameters);
    }

    private long launch(Job job, JobParameters parameters) {
        try {
            JobExecution execution = operator.start(job, parameters);
            log.info("Started job {} (execution {})", job.getName(), execution.getId());
            return execution.getId();
        } catch (org.springframework.batch.core.launch.JobExecutionAlreadyRunningException e) {
            throw new ConflictException("This job is already running");
        } catch (Exception e) {
            throw new IllegalStateException("Could not start job " + job.getName() + ": " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ describing runs

    public JobSummary get(long executionId) {
        JobExecution execution = repository.getJobExecution(executionId);
        if (execution == null) {
            throw new NotFoundException("Job execution not found: " + executionId);
        }
        boolean isImport = ImportProductsJobConfig.JOB_NAME.equals(execution.getJobInstance().getJobName());
        return toSummary(execution, isImport ? rejected.find(executionId, 200) : List.of());
    }

    /** The most recent runs of all jobs, newest first. */
    public List<JobSummary> recent(int limit) {
        List<JobExecution> all = new ArrayList<>();
        for (String name : List.of(ImportProductsJobConfig.JOB_NAME, DailySalesReportJobConfig.JOB_NAME, CleanupJobConfig.JOB_NAME)) {
            for (JobInstance instance : repository.getJobInstances(name, 0, limit)) {
                all.addAll(repository.getJobExecutions(instance));
            }
        }
        return all.stream()
                .sorted(Comparator.comparing(JobExecution::getCreateTime).reversed())
                .limit(limit)
                .map(e -> toSummary(repository.getJobExecution(e.getId()), List.of()))
                .toList();
    }

    private JobSummary toSummary(JobExecution e, List<com.ecommerce.batch.imports.RejectedRowRepository.RejectedRow> rejectedRows) {
        Map<String, String> parameters = new LinkedHashMap<>();
        e.getJobParameters().parameters().forEach(p -> {
            if (!"runId".equals(p.name())) {
                parameters.put(p.name(), String.valueOf(p.value()));
            }
        });
        List<StepSummary> steps = e.getStepExecutions().stream()
                .sorted(Comparator.comparing(StepExecution::getId))
                .map(JobService::toStep).toList();
        LocalDateTime start = e.getStartTime();
        LocalDateTime end = e.getEndTime();
        Long duration = start != null && end != null ? Duration.between(start, end).toMillis() : null;
        String message = failureMessage(e);
        return new JobSummary(e.getId(), e.getJobInstance().getJobName(), e.getStatus().name(), message, start, end, duration,
                parameters, steps, rejectedRows);
    }

    /**
     * For a failed job: the reason in plain words, not the whole stack trace. Null when the job did not fail.
     * A job loaded from the database only has the stack trace as text, so the last "Caused by:" line is used
     * (that is the real cause), without the Java class name in front of it.
     */
    private static String failureMessage(JobExecution e) {
        String trace = e.getExitStatus().getExitDescription();
        if (!e.getStatus().isUnsuccessful() || trace == null || trace.isBlank()) {
            return null;
        }
        String line = trace.lines().filter(l -> l.startsWith("Caused by: ")).reduce((first, second) -> second)
                .map(l -> l.substring("Caused by: ".length()))
                .orElse(trace.lines().findFirst().orElse(trace));
        int colon = line.indexOf(": ");
        if (colon > 0 && line.substring(0, colon).matches("[\\w.$]+")) {
            line = line.substring(colon + 2);                  // drop "java.lang.NullPointerException: "
        }
        return line.length() > 500 ? line.substring(0, 500) : line;
    }

    private static StepSummary toStep(StepExecution s) {
        Map<String, Object> details = new LinkedHashMap<>();
        s.getExecutionContext().entrySet().forEach(entry -> {
            if (VISIBLE_DETAILS.contains(entry.getKey())) {
                details.put(entry.getKey(), entry.getValue());
            }
        });
        return new StepSummary(s.getStepName(), s.getStatus().name(), s.getReadCount(), s.getWriteCount(), s.getSkipCount(),
                s.getFilterCount(), details);
    }
}
