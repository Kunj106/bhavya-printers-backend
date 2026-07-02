package com.bhavyaprinters.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class EmailService
{
    @Value("${brevo.api.key}")
    private String apiKey;

    private final WebClient webClient = WebClient.create();

    public void sendOtp(String email, String otp) {

        Map<String, Object> body = Map.of(

                "sender", Map.of(
                        "name", "Bhavya Printers",
                        "email", "bhavyaprinters21@gmail.com"
                ),

                "to", List.of(
                        Map.of("email", email)
                ),

                "subject", "Bhavya Printers OTP",

                "htmlContent",
                "<h2>Your OTP is <b>" + otp + "</b></h2>"
                        + "<p>This OTP is valid for <b>5 minutes</b>.</p>"
                        + "<p>Do not share this OTP with anyone.</p>"
        );

        webClient.post()
                .uri("https://api.brevo.com/v3/smtp/email")
                .header("api-key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
