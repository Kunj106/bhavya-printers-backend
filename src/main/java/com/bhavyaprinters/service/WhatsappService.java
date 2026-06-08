package com.bhavyaprinters.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class WhatsappService
{
    @Value("${notification.whatsapp.phone-number-id}")
    private String phoneNumberId;

    @Value("${notification.whatsapp.access-token}")
    private String accessToken;

    private final WebClient webClient = WebClient.create();

    public void sendOtp(String toPhone, String otp) {
        String url = "https://graph.facebook.com/v19.0/" + phoneNumberId + "/messages";

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", toPhone,
                "type", "text",
                "text", Map.of("body", "Your Bhavya Printers OTP is: " + otp + ". Valid for 5 minutes.")
        );

        webClient.post()
                .uri(url)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe();
    }
}
