package com.ecommerce.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ecommerce.order.client.CartClient.CartItemDto;
import com.ecommerce.order.dto.Dtos.OrderItemResponse;
import com.ecommerce.order.dto.Dtos.OrderResponse;
import com.ecommerce.order.entity.CustomerOrder;
import com.ecommerce.order.entity.OrderItem;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    /** Order entity -> JSON answer (its list of items is mapped with toItemResponse automatically). */
    OrderResponse toResponse(CustomerOrder order);

    OrderItemResponse toItemResponse(OrderItem item);

    /** One cart line -> one order line (a copy of name and price, so later price changes do not matter). */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    OrderItem toOrderItem(CartItemDto cartItem);
}
