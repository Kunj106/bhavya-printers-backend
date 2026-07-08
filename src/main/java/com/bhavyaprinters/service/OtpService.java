package com.bhavyaprinters.service;

import com.bhavyaprinters.entity.Otp;
import com.bhavyaprinters.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpService
{
    private final OtpRepository otpRepository;

    @Transactional
    public String generateOtp(String email) {

        otpRepository.deleteByEmail(email);

        String otp = String.valueOf(100000 + new Random().nextInt(900000));

        Otp entity = Otp.builder()
                .email(email)
                .otp(otp)
                .expiryTime(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .build();

        otpRepository.save(entity);

        return otp;
    }

    @Transactional
    public boolean verifyOtp(String email, String otp) {

        Otp entity = otpRepository.findByEmail(email).orElse(null);

        if (entity == null)
            return false;

        if (entity.getExpiryTime().isBefore(LocalDateTime.now())) {
            otpRepository.delete(entity);
            return false;
        }

        if (!entity.getOtp().equals(otp))
            return false;

        entity.setVerified(true);

        otpRepository.save(entity);

        return true;
    }

    public boolean isEmailVerified(String email) {

        Otp entity = otpRepository.findByEmail(email).orElse(null);

        return entity != null && entity.isVerified();

    }

    @Transactional
    public void clearVerification(String email) {

        otpRepository.deleteByEmail(email);

    }
}
