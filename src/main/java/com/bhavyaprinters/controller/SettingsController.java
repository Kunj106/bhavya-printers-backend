package com.bhavyaprinters.controller;

import com.bhavyaprinters.dto.ErrorResponseDto;
import com.bhavyaprinters.dto.SettingsDto;
import com.bhavyaprinters.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.bhavyaprinters.dto.MessageResponseDto;

import java.util.Map;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    public ResponseEntity<SettingsDto> getSettings() {
        return ResponseEntity.ok(new SettingsDto(
                settingsService.getUpiId(),
                settingsService.getAdminMobile(),
                settingsService.getUpiQrCode(),
                false,
                settingsService.getAdminUsername(),
                settingsService.getGstRate()
        ));
    }

    @PutMapping("/credentials")
    public ResponseEntity<?> updateCredentials(@RequestBody Map<String, String> body) {
        String currentPassword = body.get("currentPassword");
        String newUsername = body.get("newUsername");
        String newPassword = body.get("newPassword");

        if (currentPassword == null || currentPassword.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDto("Current password is required"));
        }

        if (!settingsService.hashPassword(currentPassword).equals(settingsService.getAdminPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponseDto("Current password is incorrect"));
        }

        if (newPassword != null && !newPassword.isBlank() && newPassword.trim().length() < 6) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDto("New password must be at least 6 characters"));
        }

        String newUsername2 = (newUsername != null && !newUsername.isBlank()) ? newUsername.trim() : null;
        String newPasswordHash = (newPassword != null && !newPassword.isBlank())
                ? settingsService.hashPassword(newPassword.trim()) : null;

        if (newUsername2 == null && newPasswordHash == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDto("Provide at least a new username or password"));
        }

        settingsService.updateCredentials(newUsername2, newPasswordHash);
        return ResponseEntity.ok(Map.of(
                "adminUsername", settingsService.getAdminUsername(),
                "message", "Credentials updated successfully"
        ));
    }

    @PutMapping("/upi")
    public ResponseEntity<?> updateUpi(@RequestBody Map<String, String> body) {
        String upiId = body.get("upiId");
        String upiQrCode = body.get("upiQrCode");

        if (upiId == null || upiId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDto("UPI ID is required"));
        }

        settingsService.updateUpi(upiId.trim(), upiQrCode);
        return ResponseEntity.ok(Map.of(
                "upiId", settingsService.getUpiId(),
                "upiQrCode", settingsService.getUpiQrCode() != null ? settingsService.getUpiQrCode() : "",
                "message", "UPI settings updated"
        ));
    }

    @PutMapping("/admin-mobile")
    public ResponseEntity<?> updateAdminMobile(@RequestBody Map<String, String> body) {
        String mobile = body.getOrDefault("adminMobile", "").trim();
        settingsService.updateAdminMobile(mobile.isBlank() ? null : mobile);
        return ResponseEntity.ok(new MessageResponseDto("Admin mobile saved"));
    }

    @PutMapping("/fast2sms-key")
    public ResponseEntity<?> updateFast2smsKey(@RequestBody Map<String, String> body) {
        String key = body.getOrDefault("apiKey", "").trim();
        if (key.isBlank()) return ResponseEntity.badRequest().body(new ErrorResponseDto("apiKey required"));
        settingsService.updateFast2smsKey(key);
        System.setProperty("FAST2SMS_API_KEY", key);
        return ResponseEntity.ok(new MessageResponseDto("Fast2SMS key saved"));
    }

    @PutMapping("/gst-rate")
    public ResponseEntity<?> updateGstRate(@RequestBody Map<String, Object> body) {
        Object rate = body.get("gstRate");
        if (rate == null) return ResponseEntity.badRequest()
                .body(new ErrorResponseDto("gstRate required"));
        int gstRate = Integer.parseInt(rate.toString());
        if (gstRate != 12 && gstRate != 18)
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDto("gstRate must be 12 or 18"));
        settingsService.updateGstRate(gstRate);
        return ResponseEntity.ok(Map.of("gstRate", gstRate, "message", "GST rate updated"));
    }
}