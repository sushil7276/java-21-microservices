package com.ecommerce.batch.imports;

import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;

/**
 * Called by Spring Batch every time a line is SKIPPED, so the reason can be saved for the admin:
 *   onSkipInRead     the line could not even be cut into columns (wrong number of columns)
 *   onSkipInProcess  the processor refused the line (InvalidRowException: bad price, no name ...)
 */
public class RejectedRowListener implements SkipListener<ProductCsvRow, ImportItem> {

    private final RejectedRowRepository rejected;
    private final long jobExecutionId;

    public RejectedRowListener(RejectedRowRepository rejected, long jobExecutionId) {
        this.rejected = rejected;
        this.jobExecutionId = jobExecutionId;
    }

    @Override
    public void onSkipInRead(Throwable t) {
        if (t instanceof FlatFileParseException e) {
            String reason = e.getCause() == null ? e.getMessage() : e.getCause().getMessage();
            rejected.add(jobExecutionId, e.getLineNumber(), null, e.getInput(),
                    "line cannot be read: " + reason + " (expected: " + String.join(",", ProductCsvLineMapper.COLUMNS) + ")");
        }
    }

    @Override
    public void onSkipInProcess(ProductCsvRow row, Throwable t) {
        rejected.add(jobExecutionId, row.lineNumber(), row.name(), row.rawLine(), t.getMessage());
    }
}
