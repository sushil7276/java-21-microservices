package com.ecommerce.product.dto;

import java.util.List;

/** Answer of the bulk import: how many products were created, and which rows were refused (and why). */
public record BulkResult(int created, List<Rejected> rejected) {

    /** @param index position of the product in the request list (0 = first) */
    public record Rejected(int index, String name, String reason) {
    }
}
