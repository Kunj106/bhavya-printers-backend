package com.bhavyaprinters.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Sends SMS via Fast2SMS (https://www.fast2sms.com) — free Indian SMS API.
 * Sends WhatsApp messages via Meta WhatsApp Business Cloud API (free tier).
 *
 * ── Setup (do this once) ──────────────────────────────────────────────────
 *
 * FAST2SMS (SMS):
 *   1. Sign up free at https://www.fast2sms.com
 *   2. Go to Dev API → copy your API key
 *   3. Set notification.fast2sms.api-key=<your_key> in application.properties
 *
 * META WHATSAPP CLOUD API (WhatsApp):
 *   1. Go to https://developers.facebook.com → create an App → select "Business"
 *   2. Add "WhatsApp" product to your app
 *   3. In WhatsApp → Getting Started, copy:
 *        - Phone Number ID  → notification.whatsapp.phone-number-id
 *        - Temporary Token  → notification.whatsapp.access-token
 *   4. To send to any number (not just test numbers), verify your business at
 *      https://business.facebook.com and add recipients.
 *   Note: 1,000 free service conversations per month.
 * ─────────────────────────────────────────────────────────────────────────
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Value("${notification.fast2sms.api-key:}")
    private String fast2smsApiKey;

    @Value("${notification.whatsapp.access-token:}")
    private String whatsappAccessToken;

    @Value("${notification.whatsapp.phone-number-id:}")
    private String whatsappPhoneNumberId;

    @Value("${notification.enabled:false}")
    private boolean notificationsEnabled;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // ─── Public API ───────────────────────────────────────────────────────

    /** Called when a new order is placed. */
    public void notifyOrderPlaced(String mobile, String email, String bankName,
                                   String branchName, Long orderId, double total) {
        if (!notificationsEnabled) return;

        String smsMsg = String.format(
            "Dear %s (%s), your order #%d has been placed with Bhavya Printers. " +
            "Total: Rs. %.2f. We will confirm shortly. - Bhavya Printers",
            bankName, branchName, orderId, total
        );

        String waMsg = String.format(
            "Hello *%s* (%s)!\n\n" +
            "Your order *#%d* has been successfully placed with *Bhavya Printers*.\n\n" +
            "Order Total: *Rs. %.2f*\n" +
            "Status: *Pending*\n\n" +
            "We will confirm your order shortly. Thank you for choosing Bhavya Printers!",
            bankName, branchName, orderId, total
        );

        sendSms(mobile, smsMsg);
        sendWhatsApp(mobile, waMsg);
    }

    /** Called when admin updates the order status. */
    public void notifyOrderStatusUpdate(String mobile, String bankName,
                                         Long orderId, String newStatus) {
        if (!notificationsEnabled) return;

        String smsMsg = String.format(
            "Dear %s, your Bhavya Printers order #%d status has been updated to: %s. " +
            "Thank you! - Bhavya Printers",
            bankName, orderId, newStatus
        );

        String waMsg = String.format(
            "Hello *%s*!\n\n" +
            "Your order *#%d* status has been updated.\n\n" +
            "New Status: *%s*\n\n" +
            "%s\n\n" +
            "Thank you for your business! - Bhavya Printers",
            bankName, orderId, newStatus, getStatusMessage(newStatus)
        );

        sendSms(mobile, smsMsg);
        sendWhatsApp(mobile, waMsg);
    }

    // ─── Fast2SMS (SMS) ───────────────────────────────────────────────────

    private void sendSms(String mobile, String message) {
        if (fast2smsApiKey == null || fast2smsApiKey.isBlank()) {
            log.warn("Fast2SMS API key not configured — skipping SMS to {}", mobile);
            return;
        }

        try {
            // Remove country code prefix if present (Fast2SMS needs 10-digit Indian number)
            String number = mobile.replaceAll("^\\+?91", "").trim();

            String encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8);
            String url = "https://www.fast2sms.com/dev/bulkV2" +
                         "?authorization=" + fast2smsApiKey +
                         "&route=v3" +
                         "&message=" + encodedMsg +
                         "&flash=0" +
                         "&numbers=" + number;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("cache-control", "no-cache")
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                log.info("SMS sent to {} via Fast2SMS", number);
            } else {
                log.warn("Fast2SMS returned status {} for {}: {}", response.statusCode(), number, response.body());
            }
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", mobile, e.getMessage());
        }
    }

    // ─── Meta WhatsApp Business Cloud API ────────────────────────────────

    private void sendWhatsApp(String mobile, String message) {
        if (whatsappAccessToken == null || whatsappAccessToken.isBlank() ||
            whatsappPhoneNumberId == null || whatsappPhoneNumberId.isBlank()) {
            log.warn("WhatsApp credentials not configured — skipping WhatsApp to {}", mobile);
            return;
        }

        try {
            // Ensure number has country code (add +91 for India if missing)
            String number = mobile.startsWith("+") ? mobile
                    : mobile.startsWith("91") ? "+" + mobile
                    : "+91" + mobile;

            // Escape double-quotes inside the message for JSON
            String safeMsg = message.replace("\"", "\\\"").replace("\n", "\\n");

            String body = "{"
                + "\"messaging_product\":\"whatsapp\","
                + "\"to\":\"" + number + "\","
                + "\"type\":\"text\","
                + "\"text\":{\"preview_url\":false,\"body\":\"" + safeMsg + "\"}"
                + "}";

            String url = "https://graph.facebook.com/v18.0/" + whatsappPhoneNumberId + "/messages";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + whatsappAccessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                log.info("WhatsApp message sent to {}", number);
            } else {
                log.warn("WhatsApp API returned status {} for {}: {}", response.statusCode(), number, response.body());
            }
        } catch (Exception e) {
            log.error("Failed to send WhatsApp to {}: {}", mobile, e.getMessage());
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private String getStatusMessage(String status) {
        return switch (status) {
            case "Confirmed"  -> "Your order has been confirmed and is being prepared.";
            case "Processing" -> "Your order is currently being processed and printed.";
            case "Delivered"  -> "Your order has been delivered. Please confirm receipt.";
            case "Cancelled"  -> "Your order has been cancelled. Contact us for queries.";
            default           -> "Please check your order details on the portal.";
        };
    }

    /**
     * Send OTP via Fast2SMS. Returns true if sent successfully, false otherwise.
     * In dev mode (no API key), returns false — caller should log OTP to console.
     */
    /**
     * Send OTP via Fast2SMS. Returns true if sent successfully, false otherwise.
     * In dev mode (no API key), returns false — caller should log OTP to console.
     */
    public boolean sendOtpSms(String mobile, String otp) {
        if (fast2smsApiKey == null || fast2smsApiKey.isBlank()) {
            log.warn("Fast2SMS API key not configured — skipping OTP SMS to {}", mobile);
            return false;
        }
        try {
            String digits = mobile.replaceAll("[^0-9]", "");
            if (digits.length() > 10) digits = digits.substring(digits.length() - 10);

            String body = "{"
                    + "\"route\":\"otp\","
                    + "\"variables_values\":\"" + otp + "\","
                    + "\"numbers\":\"" + digits + "\","
                    + "\"language\":\"english\","
                    + "\"flash\":\"0\""
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://www.fast2sms.com/dev/bulkV2"))
                    .header("authorization", fast2smsApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body().contains("\"return\":true")) {
                log.info("OTP SMS sent to {}", digits);
                return true;
            } else {
                log.warn("Fast2SMS OTP returned status {} for {}: {}", response.statusCode(), digits, response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to send OTP SMS to {}: {}", mobile, e.getMessage());
            return false;
        }
    }

}