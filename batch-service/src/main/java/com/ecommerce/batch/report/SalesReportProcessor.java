package com.ecommerce.batch.report;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.ecommerce.batch.client.OrderClient.OrderDto;
import com.ecommerce.batch.client.OrderClient.OrderItemDto;

/** The PROCESSOR of the sales report: counts the pieces in one order. */
@Component
public class SalesReportProcessor implements ItemProcessor<OrderDto, OrderSale> {

    @Override
    public OrderSale process(OrderDto order) {
        int pieces = order.items().stream().mapToInt(OrderItemDto::quantity).sum();
        return new OrderSale(order.id(), order.totalAmount(), pieces, order.items());
    }
}
