package com.bhavyaprinters.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BankLoginInputDto {
    @NotBlank @Email private String email;
    @NotBlank private String password;
}
