package com.bhavyaprinters.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BankPasswordUpdateDto
{
    @NotBlank
    private String currentPassword;

    @NotBlank
    @Size(min = 6, message = "New password must be at least 6 characters")
    private String newPassword;
}
