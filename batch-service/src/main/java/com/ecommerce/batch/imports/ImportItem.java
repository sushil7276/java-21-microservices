package com.ecommerce.batch.imports;

import com.ecommerce.batch.client.ProductClient.NewProduct;

/** A line that passed all checks: the product to create, plus its line number (to report problems later). */
public record ImportItem(int lineNumber, NewProduct product) {
}
