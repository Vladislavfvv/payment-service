package com.innowise.paymentservice.dto;

import lombok.Data;
import java.time.LocalDate;

/**
 * DTO для представления пользователя из user-service
 */
@Data
public class UserDto {
    private Long id;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String email;
}

