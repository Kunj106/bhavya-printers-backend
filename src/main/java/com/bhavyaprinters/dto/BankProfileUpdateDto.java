package com.bhavyaprinters.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class BankProfileUpdateDto
{
    private String address;
    private String mobile;

    @Email
    private String email;

    private String panNo;
    private String gstNo;
}
