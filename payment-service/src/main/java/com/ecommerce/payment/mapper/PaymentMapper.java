package com.ecommerce.payment.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.entity.Payment;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentResponse toResponse(Payment payment);

    List<PaymentResponse> toResponses(List<Payment> payments);
}
