package com.ecommerce.shipping.mapper;

import org.mapstruct.Mapper;

import com.ecommerce.shipping.dto.ShipmentResponse;
import com.ecommerce.shipping.entity.Shipment;

@Mapper(componentModel = "spring")
public interface ShipmentMapper {

    ShipmentResponse toResponse(Shipment shipment);
}
