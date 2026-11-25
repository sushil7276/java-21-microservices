package com.ecommerce.batch.report;

import java.math.BigDecimal;
import java.util.List;

import com.ecommerce.batch.client.OrderClient.OrderItemDto;

/** One paid order, reduced to what the report needs. */
public record OrderSale(Long orderId, BigDecimal total, int itemsSold, List<OrderItemDto> items) {
}
