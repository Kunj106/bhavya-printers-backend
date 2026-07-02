package com.bhavyaprinters.controller;

import com.bhavyaprinters.service.EmailService;
import com.bhavyaprinters.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mail")
@RequiredArgsConstructor
public class MailTestController
{
        private final EmailService emailService;

        @GetMapping("/test")
        public String test(@RequestParam String email) {

            emailService.sendOtp(email, "123456");

            return "Email Sent";
        }
    }
