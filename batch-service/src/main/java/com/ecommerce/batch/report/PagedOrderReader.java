package com.ecommerce.batch.report;

import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;

import org.springframework.batch.infrastructure.item.data.AbstractPaginatedDataItemReader;

import com.ecommerce.batch.client.OrderClient;
import com.ecommerce.batch.client.OrderClient.OrderDto;
import com.ecommerce.batch.client.OrderClient.OrdersPage;

/**
 * The READER of the sales report. A day can have thousands of orders, so they are NOT loaded at once:
 * the reader asks order-service for ONE PAGE (50 orders), hands them out one by one, and asks for the next page
 * when the page is used up. The base class calls doPageRead() again and again until it returns nothing.
 */
public class PagedOrderReader extends AbstractPaginatedDataItemReader<OrderDto> {

    private final OrderClient orders;
    private final Instant from;
    private final Instant to;

    public PagedOrderReader(OrderClient orders, Instant from, Instant to, int pageSize) {
        this.orders = orders;
        this.from = from;
        this.to = to;
        setName("ordersOfTheDay");
        setPageSize(pageSize);
    }

    @Override
    protected Iterator<OrderDto> doPageRead() {
        // "page" is counted by the base class: 0 for the first call, 1 for the second, ...
        OrdersPage result = orders.salesPage(from, to, page, pageSize);
        // an EMPTY iterator tells the base class that there is no more data (it must not be null)
        return result.content().isEmpty() ? Collections.emptyIterator() : result.content().iterator();
    }
}
