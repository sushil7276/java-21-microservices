package com.ecommerce.cart.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ecommerce.cart.dto.Dtos.CartItemResponse;
import com.ecommerce.cart.entity.CartItem;

@Mapper(componentModel = "spring")
public interface CartMapper {

    /** lineTotal does not exist on the entity, so we tell MapStruct how to calculate it. */
    @Mapping(target = "lineTotal",
            expression = "java(item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))")
    CartItemResponse toItemResponse(CartItem item);

    List<CartItemResponse> toItemResponses(List<CartItem> items);
}
