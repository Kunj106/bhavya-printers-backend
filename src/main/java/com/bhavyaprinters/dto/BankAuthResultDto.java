package com.bhavyaprinters.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BankAuthResultDto {
    private String token;
    private String role;
    private BankDto bank;
}
