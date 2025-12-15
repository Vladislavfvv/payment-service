package com.innowise.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

//DTO for response from external random number API
 
@Data
@JsonIgnoreProperties(ignoreUnknown = true) //ignore unknown properties in the response
public class RandomNumberResponse {
    private Integer random;
}

