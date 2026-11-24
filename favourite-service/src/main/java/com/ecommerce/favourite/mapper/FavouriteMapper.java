package com.ecommerce.favourite.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ecommerce.favourite.client.ProductClient.ProductDto;
import com.ecommerce.favourite.dto.FavouriteResponse;
import com.ecommerce.favourite.entity.Favourite;

@Mapper(componentModel = "spring")
public interface FavouriteMapper {

    /** Two sources: the database row (id, productId, createdAt) and the product details from product-service. */
    @Mapping(target = "id", source = "favourite.id")
    @Mapping(target = "productId", source = "favourite.productId")
    @Mapping(target = "addedAt", source = "favourite.createdAt")
    @Mapping(target = "product", source = "product")
    FavouriteResponse toResponse(Favourite favourite, ProductDto product);
}
