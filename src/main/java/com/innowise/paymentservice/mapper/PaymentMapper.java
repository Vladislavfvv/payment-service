package com.innowise.paymentservice.mapper;

import com.innowise.paymentservice.dto.CreatePaymentRequest;
import com.innowise.paymentservice.dto.PaymentDto;
import com.innowise.paymentservice.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    PaymentDto toDto(Payment payment);

    Payment toEntity(PaymentDto dto);
    
    /**
     * Convert CreatePaymentRequest to Payment entity
     * id and timestamp are ignored as they are set by the service
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    Payment toEntity(CreatePaymentRequest request);
    
    /**
     * Convert list of Payment entities to list of PaymentDto
     */
    List<PaymentDto> toDtoList(java.util.List<Payment> payments);
}

