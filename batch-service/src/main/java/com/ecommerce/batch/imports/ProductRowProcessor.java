package com.ecommerce.batch.imports;

import java.math.BigDecimal;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.ecommerce.batch.client.ProductClient.NewProduct;

/**
 * The PROCESSOR of the import: checks one line and turns the text into a real product.
 *   - returns an {@link ImportItem}  -> the line goes on to the writer
 *   - returns null                   -> the line is silently dropped ("filtered"), used for empty lines
 *   - throws InvalidRowException     -> the line is SKIPPED and reported with the reason
 */
@Component
public class ProductRowProcessor implements ItemProcessor<ProductCsvRow, ImportItem> {

    @Override
    public ImportItem process(ProductCsvRow row) {
        if (row.isBlank()) {
            return null;
        }
        String name = row.name().trim();
        if (name.isEmpty()) {
            throw new InvalidRowException("name is required");
        }
        if (name.length() > 255) {
            throw new InvalidRowException("name is longer than 255 characters");
        }
        BigDecimal price = parsePrice(row.price());
        int stock = parseStock(row.stock());
        String description = row.description().trim();
        if (description.length() > 1000) {
            throw new InvalidRowException("description is longer than 1000 characters");
        }
        String imageUrl = row.imageUrl().trim();
        if (!imageUrl.isEmpty() && !imageUrl.matches("^https?://\\S+$")) {
            throw new InvalidRowException("imageUrl must start with http:// or https://");
        }
        return new ImportItem(row.lineNumber(), new NewProduct(name, description.isEmpty() ? null : description, price,
                stock, row.category().trim().isEmpty() ? null : row.category().trim(), imageUrl.isEmpty() ? null : imageUrl));
    }

    private static BigDecimal parsePrice(String text) {
        try {
            BigDecimal price = new BigDecimal(text.trim());
            if (price.signum() <= 0) {
                throw new InvalidRowException("price must be greater than 0");
            }
            return price;
        } catch (NumberFormatException e) {
            throw new InvalidRowException("price '" + text.trim() + "' is not a number");
        }
    }

    private static int parseStock(String text) {
        try {
            int stock = Integer.parseInt(text.trim());
            if (stock < 0) {
                throw new InvalidRowException("stock must not be negative");
            }
            return stock;
        } catch (NumberFormatException e) {
            throw new InvalidRowException("stock '" + text.trim() + "' is not a whole number");
        }
    }
}
