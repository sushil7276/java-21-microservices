package com.ecommerce.product.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.Product;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    /** Database entity -> JSON answer. */
    ProductResponse toResponse(Product product);

    List<ProductResponse> toResponses(List<Product> products);

    /** JSON request -> new entity. The id is created by the database, createdAt is "now". */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    Product toEntity(ProductRequest request);

    /** Copies the request onto an EXISTING entity (used by PUT /products/{id}). */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(ProductRequest request, @MappingTarget Product product);
}
