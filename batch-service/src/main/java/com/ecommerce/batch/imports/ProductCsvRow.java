package com.ecommerce.batch.imports;

/**
 * One line of the CSV file, exactly as text (nothing is checked yet).
 *
 * @param lineNumber the line in the file (the header is line 1), so an error can say where the problem is
 * @param rawLine    the whole original line, kept for the error report
 */
public record ProductCsvRow(int lineNumber, String name, String description, String price, String stock,
                            String category, String imageUrl, String rawLine) {

    /** An empty line in the file. */
    public boolean isBlank() {
        return rawLine == null || rawLine.isBlank();
    }
}
