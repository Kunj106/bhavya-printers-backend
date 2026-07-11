package com.bhavyaprinters.service;

import com.bhavyaprinters.entity.AdminSettings;
import com.bhavyaprinters.repository.AdminSettingsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final AdminSettingsRepository repository;

    private static final Long SETTINGS_ID = 1L;
    private static final String SALT = "bhavya_salt_1996";

    private AdminSettings getOrCreate() {
        return repository.findById(SETTINGS_ID).orElseGet(() -> {
            AdminSettings s = new AdminSettings();
            s.setId(SETTINGS_ID);
            return repository.save(s);
        });
    }

    public String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((password + SALT).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    // ── Admin identity ──────────────────────────────────────────────
    public String getAdminUsername()     { return getOrCreate().getAdminUsername(); }
    public String getAdminEmail()        { return getOrCreate().getAdminEmail(); }
    public String getAdminPasswordHash() { return getOrCreate().getAdminPasswordHash(); }
    public boolean isAdminRegistered()   { return getOrCreate().isAdminRegistered(); }

    /** Called once, at registration time only. */
    public void registerAdmin(String username, String email, String passwordHash) {
        AdminSettings s = getOrCreate();
        s.setAdminUsername(username);
        s.setAdminEmail(email);
        s.setAdminPasswordHash(passwordHash);
        s.setAdminRegistered(true);
        repository.save(s);
    }

    /** Called when an already-logged-in admin updates username/password. */
    public void updateCredentials(String newUsername, String newPasswordHash) {
        AdminSettings s = getOrCreate();
        if (newUsername != null && !newUsername.isBlank()) s.setAdminUsername(newUsername);
        if (newPasswordHash != null) s.setAdminPasswordHash(newPasswordHash);
        repository.save(s);
    }

    // ── Other settings (unchanged behavior, now DB-backed) ──────────
    public String getUpiId()           { return getOrCreate().getUpiId(); }
    public String getUpiQrCode()       { return getOrCreate().getUpiQrCode(); }
    public String getAdminMobile()     { return getOrCreate().getAdminMobile(); }
    public String getFast2smsApiKey()  { return getOrCreate().getFast2smsApiKey(); }
    public int getGstRate()            { return getOrCreate().getGstRate(); }

    public void updateUpi(String newUpiId, String newUpiQrCode) {
        AdminSettings s = getOrCreate();
        s.setUpiId(newUpiId);
        s.setUpiQrCode(newUpiQrCode);
        repository.save(s);
    }

    public void updateAdminMobile(String mobile) {
        AdminSettings s = getOrCreate();
        s.setAdminMobile(mobile);
        repository.save(s);
    }

    public void updateFast2smsKey(String key) {
        AdminSettings s = getOrCreate();
        s.setFast2smsApiKey(key);
        repository.save(s);
    }

    public void updateGstRate(int rate) {
        AdminSettings s = getOrCreate();
        s.setGstRate(rate);
        repository.save(s);
    }

    // ── Invoice branding ─────────────────────────────────────────────
    // Both stored as base64 data URLs (e.g. "data:image/png;base64,...")
    // so the invoice generator can drop them straight into iText without
    // any extra file-system or blob-storage lookups.

    public String getLetterheadImage() { return getOrCreate().getLetterheadImage(); }
    public String getSignatureImage()  { return getOrCreate().getSignatureImage(); }

    public void updateLetterhead(String base64Image) {
        AdminSettings s = getOrCreate();
        s.setLetterheadImage(base64Image);
        repository.save(s);
    }

    public void updateSignature(String base64Image) {
        AdminSettings s = getOrCreate();
        s.setSignatureImage(base64Image);
        repository.save(s);
    }
}