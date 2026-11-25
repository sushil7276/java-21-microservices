package com.ecommerce.batch.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

import com.ecommerce.batch.client.OrderClient.OrderItemDto;
import com.ecommerce.batch.report.SalesReportRepository.TopProduct;

/**
 * The WRITER of the sales report. Unlike the import, nothing is written per chunk: each chunk of orders is only
 * ADDED to running totals in memory. When the whole step is finished, {@link #afterStep} saves ONE report row
 * (plus the best sellers). This "read a lot, write one summary" shape is typical for reports.
 */
public class SalesReportWriter implements ItemWriter<OrderSale>, StepExecutionListener {

    private static final int TOP_PRODUCTS = 5;

    private final SalesReportRepository repository;
    private final LocalDate date;

    private int orders;
    private int itemsSold;
    private BigDecimal revenue = BigDecimal.ZERO;
    private final Map<Long, TopProduct> perProduct = new HashMap<>();

    public SalesReportWriter(SalesReportRepository repository, LocalDate date) {
        this.repository = repository;
        this.date = date;
    }

    @Override
    public void write(Chunk<? extends OrderSale> chunk) {
        for (OrderSale sale : chunk) {
            orders++;
            itemsSold += sale.itemsSold();
            revenue = revenue.add(sale.total());
            for (OrderItemDto item : sale.items()) {
                BigDecimal line = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
                perProduct.merge(item.productId(), new TopProduct(item.productId(), item.productName(), item.quantity(), line),
                        (a, b) -> new TopProduct(a.productId(), a.productName(), a.quantity() + b.quantity(), a.revenue().add(b.revenue())));
            }
        }
    }

    /** Called by Spring Batch when the step has read everything. */
    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (stepExecution.getStatus().isUnsuccessful()) {
            return null;                                      // the step failed: do not save a half report
        }
        List<TopProduct> best = perProduct.values().stream()
                .sorted(Comparator.comparingInt(TopProduct::quantity).reversed().thenComparing(TopProduct::revenue, Comparator.reverseOrder()))
                .limit(TOP_PRODUCTS).toList();
        repository.save(date, orders, itemsSold, revenue, best, stepExecution.getJobExecutionId());

        // Totals for the result screen (kept with the step in the job history).
        var context = stepExecution.getExecutionContext();
        context.putString("reportDate", date.toString());
        context.putLong("orders", orders);
        context.putLong("itemsSold", itemsSold);
        context.putString("revenue", revenue.toPlainString());
        return null;
    }
}
