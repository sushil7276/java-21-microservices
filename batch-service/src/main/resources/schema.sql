-- Our own tables in batch_db (the Spring Batch history tables are created by Spring Batch itself).

-- Rows of an uploaded CSV file that were NOT imported, and why (one row per refused line).
CREATE TABLE IF NOT EXISTS import_rejected_row (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_execution_id BIGINT        NOT NULL,
    line_number      INT           NOT NULL,
    product_name     VARCHAR(255),
    raw_line         VARCHAR(2000),
    reason           VARCHAR(500)  NOT NULL,
    INDEX idx_rejected_execution (job_execution_id)
);

-- One row per day: the result of dailySalesReportJob.
CREATE TABLE IF NOT EXISTS daily_sales_report (
    report_date      DATE          PRIMARY KEY,
    order_count      INT           NOT NULL,
    items_sold       INT           NOT NULL,
    revenue          DECIMAL(14,2) NOT NULL,
    generated_at     TIMESTAMP     NOT NULL,
    job_execution_id BIGINT
);

-- The best sellers of that day.
CREATE TABLE IF NOT EXISTS daily_sales_report_product (
    report_date  DATE          NOT NULL,
    product_id   BIGINT        NOT NULL,
    product_name VARCHAR(255)  NOT NULL,
    quantity     INT           NOT NULL,
    revenue      DECIMAL(14,2) NOT NULL,
    PRIMARY KEY (report_date, product_id)
);
