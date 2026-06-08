package com.bhavyaprinters.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class OtpService
{
    private final Map<String, String> otpStore = new HashMap<>();

    public String generateOtp(String phone) {
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        otpStore.put(phone, otp);
        return otp;
    }

    public boolean verifyOtp(String phone, String otp) {
        return otp.equals(otpStore.get(phone));
    }

    public void clearOtp(String phone) {
        otpStore.remove(phone);
    }
}
