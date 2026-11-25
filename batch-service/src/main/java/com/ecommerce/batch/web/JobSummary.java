package com.ecommerce.batch.web;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.ecommerce.batch.imports.RejectedRowRepository.RejectedRow;

/** What the API tells about ONE run of a job (a "job execution"). */
public record JobSummary(long executionId, String jobName, String status, String message, LocalDateTime startTime,
                         LocalDateTime endTime, Long durationMs, Map<String, String> parameters, List<StepSummary> steps,
                         List<RejectedRow> rejectedRows) {

    /** @param read/written/skipped the counters Spring Batch keeps for every step; details = extra numbers of that step */
    public record StepSummary(String name, String status, long read, long written, long skipped, long filtered,
                              Map<String, Object> details) {
    }
}
