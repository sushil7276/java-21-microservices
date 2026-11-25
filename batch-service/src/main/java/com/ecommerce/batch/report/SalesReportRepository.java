package com.ecommerce.batch.report;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Stores and reads the daily sales reports (tables daily_sales_report and daily_sales_report_product). */
@Repository
public class SalesReportRepository {

    public record TopProduct(Long productId, String productName, int quantity, BigDecimal revenue) {
    }

    public record DailyReport(LocalDate date, int orders, int itemsSold, BigDecimal revenue, Instant generatedAt,
                              List<TopProduct> topProducts) {
    }

    private final JdbcClient jdbc;

    public SalesReportRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Saves the report of one day. Running the job again for the same day replaces the old report. */
    @Transactional
    public void save(LocalDate date, int orders, int itemsSold, BigDecimal revenue, List<TopProduct> products, long jobExecutionId) {
        jdbc.sql("DELETE FROM daily_sales_report_product WHERE report_date = ?").param(date).update();
        jdbc.sql("DELETE FROM daily_sales_report WHERE report_date = ?").param(date).update();
        jdbc.sql("INSERT INTO daily_sales_report (report_date, order_count, items_sold, revenue, generated_at, job_execution_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")
                .params(date, orders, itemsSold, revenue, Timestamp.from(Instant.now()), jobExecutionId).update();
        for (TopProduct p : products) {
            jdbc.sql("INSERT INTO daily_sales_report_product (report_date, product_id, product_name, quantity, revenue) "
                            + "VALUES (?, ?, ?, ?, ?)")
                    .params(date, p.productId(), p.productName(), p.quantity(), p.revenue()).update();
        }
    }

    /** The reports of the last {@code days} days, newest first, each with its best sellers. */
    public List<DailyReport> latest(int days) {
        return jdbc.sql("SELECT report_date, order_count, items_sold, revenue, generated_at FROM daily_sales_report "
                        + "WHERE report_date >= ? ORDER BY report_date DESC")
                .param(LocalDate.now().minusDays(days))
                .query((rs, n) -> {
                    LocalDate date = rs.getDate("report_date").toLocalDate();
                    return new DailyReport(date, rs.getInt("order_count"), rs.getInt("items_sold"), rs.getBigDecimal("revenue"),
                            rs.getTimestamp("generated_at").toInstant(), topProducts(date));
                }).list();
    }

    private List<TopProduct> topProducts(LocalDate date) {
        return jdbc.sql("SELECT product_id, product_name, quantity, revenue FROM daily_sales_report_product "
                        + "WHERE report_date = ? ORDER BY quantity DESC, revenue DESC")
                .param(date)
                .query((rs, n) -> new TopProduct(rs.getLong("product_id"), rs.getString("product_name"), rs.getInt("quantity"),
                        rs.getBigDecimal("revenue")))
                .list();
    }
}
