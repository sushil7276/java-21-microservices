package com.ecommerce.batch.imports;

import org.springframework.batch.infrastructure.item.file.LineMapper;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;

/**
 * Turns one text line into a {@link ProductCsvRow}.
 * The tokenizer cuts the line at the commas (and understands "quoted, values"); a line with the wrong number of
 * columns throws an exception, which the step reports as a skipped (rejected) line.
 */
public class ProductCsvLineMapper implements LineMapper<ProductCsvRow> {

    public static final String[] COLUMNS = {"name", "description", "price", "stock", "category", "imageUrl"};

    private final DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();

    public ProductCsvLineMapper() {
        tokenizer.setNames(COLUMNS);
    }

    @Override
    public ProductCsvRow mapLine(String line, int lineNumber) {
        if (line.isBlank()) {
            return new ProductCsvRow(lineNumber, "", "", "", "", "", "", line);   // the processor will ignore it
        }
        FieldSet f = tokenizer.tokenize(line);
        return new ProductCsvRow(lineNumber, f.readString("name"), f.readString("description"), f.readString("price"),
                f.readString("stock"), f.readString("category"), f.readString("imageUrl"), line);
    }
}
