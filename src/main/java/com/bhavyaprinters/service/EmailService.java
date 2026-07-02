package com.bhavyaprinters.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService
{
    @Autowired
    private JavaMailSender mailSender;

    public void sendOtp(String email, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);

        message.setSubject("Bhavya Printers OTP");

        message.setText(
                "Hello,\n\n"
                        + "Your OTP is : "
                        + otp
                        + "\n\nThis OTP is valid for 5 minutes."
                        + "\n\nDo not share this OTP with anyone."
                        + "\n\nBhavya Printers");

        mailSender.send(message);
    }
}
