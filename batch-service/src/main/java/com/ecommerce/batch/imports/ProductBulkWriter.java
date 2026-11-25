package com.ecommerce.batch.imports;

import java.util.List;

import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

import com.ecommerce.batch.client.ProductClient;
import com.ecommerce.batch.client.ProductClient.BulkResult;
import com.ecommerce.batch.client.ProductClient.NewProduct;

/**
 * The WRITER of the import: receives a whole chunk (up to 50 checked products) and sends it to product-service
 * in ONE call. Product-service answers how many were created and which were refused (a product with that
 * name already exists, ...); those are reported like any other rejected line.
 */
public class ProductBulkWriter implements ItemWriter<ImportItem> {

    private final ProductClient products;
    private final RejectedRowRepository rejected;
    private final StepExecution stepExecution;

    public ProductBulkWriter(ProductClient products, RejectedRowRepository rejected, StepExecution stepExecution) {
        this.products = products;
        this.rejected = rejected;
        this.stepExecution = stepExecution;
    }

    @Override
    public void write(Chunk<? extends ImportItem> chunk) {
        List<? extends ImportItem> items = chunk.getItems();
        BulkResult result = products.bulkCreate(items.stream().<NewProduct>map(ImportItem::product).toList());

        for (BulkResult.Rejected r : result.rejected()) {
            ImportItem item = items.get(r.index());
            rejected.add(stepExecution.getJobExecutionId(), item.lineNumber(), r.name(), null, r.reason());
        }
        // Totals for the result screen. They are saved with the step, so they survive a restart of the service.
        var context = stepExecution.getExecutionContext();
        context.putLong("created", context.getLong("created", 0) + result.created());
        context.putLong("refusedByProductService", context.getLong("refusedByProductService", 0) + result.rejected().size());
    }
}
