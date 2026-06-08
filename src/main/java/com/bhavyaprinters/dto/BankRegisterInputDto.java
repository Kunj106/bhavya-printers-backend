package com.bhavyaprinters.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BankRegisterInputDto {
    @NotBlank private String bankName;
    @NotBlank private String branchName;
    @NotBlank private String gstNo;
    @NotBlank private String panNo;
    @NotBlank private String address;
    @NotBlank private String mobile;
    @NotBlank @Email private String email;
    @NotBlank private String password;
}
