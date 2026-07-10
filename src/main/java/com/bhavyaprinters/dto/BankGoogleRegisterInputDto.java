package com.bhavyaprinters.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BankGoogleRegisterInputDto
{
    @NotBlank private String idToken;

    @NotBlank private String bankName;
    @NotBlank private String branchName;
    @NotBlank private String gstNo;
    @NotBlank private String panNo;
    @NotBlank private String address;
    @NotBlank
    private String mobile;
}
