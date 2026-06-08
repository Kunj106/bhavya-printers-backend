package com.bhavyaprinters.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankDto {
    private Long id;
    private String bankName;
    private String branchName;
    private String gstNo;
    private String panNo;
    private String address;
    private String mobile;
    private String email;
    private String createdAt;
}
