package com.bhavyaprinters.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminLoginInputDto {
    @NotBlank private String username;
    @NotBlank private String password;
}
