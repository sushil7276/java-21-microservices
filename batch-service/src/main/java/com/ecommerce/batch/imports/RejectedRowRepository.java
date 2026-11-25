package com.ecommerce.batch.imports;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Remembers which lines of an uploaded file were refused (table import_rejected_row, see schema.sql). */
@Repository
public class RejectedRowRepository {

    public record RejectedRow(int lineNumber, String productName, String rawLine, String reason) {
    }

    private final JdbcClient jdbc;

    public RejectedRowRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void add(long jobExecutionId, int lineNumber, String productName, String rawLine, String reason) {
        jdbc.sql("INSERT INTO import_rejected_row (job_execution_id, line_number, product_name, raw_line, reason) "
                        + "VALUES (?, ?, ?, ?, ?)")
                .params(jobExecutionId, lineNumber, cut(productName, 255), cut(rawLine, 2000), cut(reason, 500))
                .update();
    }

    public List<RejectedRow> find(long jobExecutionId, int limit) {
        return jdbc.sql("SELECT line_number, product_name, raw_line, reason FROM import_rejected_row "
                        + "WHERE job_execution_id = ? ORDER BY line_number LIMIT ?")
                .params(jobExecutionId, limit)
                .query((rs, n) -> new RejectedRow(rs.getInt("line_number"), rs.getString("product_name"),
                        rs.getString("raw_line"), rs.getString("reason")))
                .list();
    }

    private static String cut(String text, int max) {
        return text == null || text.length() <= max ? text : text.substring(0, max);
    }
}
