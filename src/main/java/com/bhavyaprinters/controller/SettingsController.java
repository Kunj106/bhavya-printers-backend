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

    // Roughly caps a base64-encoded image at ~5MB of original file size
    // (base64 inflates size by ~33%), matching the frontend's stated limit.
    private static final int MAX_BASE64_LENGTH = 7_000_000;

    @GetMapping
    public ResponseEntity<SettingsDto> getSettings() {
        return ResponseEntity.ok(new SettingsDto(
                settingsService.getUpiId(),
                settingsService.getAdminMobile(),
                settingsService.getUpiQrCode(),
                false,
                settingsService.getAdminUsername(),
                settingsService.getGstRate(),
                settingsService.getLetterheadImage(),
                settingsService.getSignatureImage()
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

    /**
     * Accepts upiId plus an optional upiQrCode as a base64 data URL
     * (e.g. "data:image/png;base64,..."), sent as plain JSON — not
     * multipart. The frontend converts the selected file to base64
     * client-side before calling this.
     */
    @PutMapping("/upi")
    public ResponseEntity<?> updateUpi(@RequestBody Map<String, String> body) {
        String upiId = body.get("upiId");
        String upiQrCode = body.get("upiQrCode");

        if (upiId == null || upiId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDto("UPI ID is required"));
        }

        if (upiQrCode != null && upiQrCode.length() > MAX_BASE64_LENGTH) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDto("QR code image is too large (max 5 MB)"));
        }

        // Preserve the existing QR code if none was sent this time
        // (e.g. the admin only updated the UPI ID text field).
        String qrToSave = (upiQrCode != null && !upiQrCode.isBlank())
                ? upiQrCode
                : settingsService.getUpiQrCode();

        settingsService.updateUpi(upiId.trim(), qrToSave);
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

    /**
     * Uploads the invoice letterhead image (base64 data URL, JSON body).
     * This image is stamped onto every generated order invoice.
     */
    @PutMapping("/letterhead")
    public ResponseEntity<?> updateLetterhead(@RequestBody Map<String, String> body) {
        String image = body.get("image");
        if (image == null || image.isBlank())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDto("image is required"));
        if (image.length() > MAX_BASE64_LENGTH)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDto("Letterhead image is too large (max 5 MB)"));

        settingsService.updateLetterhead(image);
        return ResponseEntity.ok(new MessageResponseDto("Letterhead updated successfully"));
    }

    /**
     * Uploads the digital signature image (base64 data URL, JSON body).
     * Stamped alongside the letterhead on every generated order invoice.
     */
    @PutMapping("/signature")
    public ResponseEntity<?> updateSignature(@RequestBody Map<String, String> body) {
        String image = body.get("image");
        if (image == null || image.isBlank())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDto("image is required"));
        if (image.length() > MAX_BASE64_LENGTH)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDto("Signature image is too large (max 5 MB)"));

        settingsService.updateSignature(image);
        return ResponseEntity.ok(new MessageResponseDto("Signature updated successfully"));
    }
}