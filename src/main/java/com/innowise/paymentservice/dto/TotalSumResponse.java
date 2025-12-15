package com.innowise.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotalSumResponse {
   
    private BigDecimal totalSum;    
    private Instant startDate;    
    private Instant endDate;        
    private Long paymentCount;
}

