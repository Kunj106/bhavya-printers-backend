package com.bhavyaprinters.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminAuthResultDto {
    private String token;
    private String role;
}
