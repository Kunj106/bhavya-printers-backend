package com.bhavyaprinters.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderStatusUpdateDto {
    @NotBlank private String status;
}
