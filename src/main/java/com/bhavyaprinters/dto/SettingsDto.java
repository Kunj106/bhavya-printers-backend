package com.bhavyaprinters.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettingsDto {
    private String upiId;
    private String adminMobile;
    private String upiQrCode;
    private boolean otpEnabled;
    private String adminUsername;
    private int gstRate;
}
