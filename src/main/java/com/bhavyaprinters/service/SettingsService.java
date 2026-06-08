package com.bhavyaprinters.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
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
public class SettingsService {

    @Value("${app.settings.path:data/settings.json}")
    private String settingsPath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Getter private String adminUsername     = "admin";
    @Getter private String adminPasswordHash;
    @Getter private String upiId            = "bhavyaprinters@sbi";
    @Getter private String upiQrCode        = null;
    @Getter private String adminMobile      = null;
    @Getter private String fast2smsApiKey   = null;
    @Getter private int    gstRate          = 18; // default 18%

    private static final String SALT = "bhavya_salt_1996";

    @PostConstruct
    public void init() {
        adminPasswordHash = hashPassword("bhavya1996");
        load();
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

    @SuppressWarnings("unchecked")
    private void load() {
        File file = new File(settingsPath);
        if (!file.exists()) { save(); return; }
        try {
            Map<String, Object> data = objectMapper.readValue(file, Map.class);
            if (data.containsKey("adminUsername"))     adminUsername     = (String) data.get("adminUsername");
            if (data.containsKey("adminPasswordHash")) adminPasswordHash = (String) data.get("adminPasswordHash");
            if (data.containsKey("upiId"))             upiId             = (String) data.get("upiId");
            if (data.containsKey("upiQrCode"))         upiQrCode         = (String) data.get("upiQrCode");
            if (data.containsKey("adminMobile"))       adminMobile       = (String) data.get("adminMobile");
            if (data.containsKey("fast2smsApiKey"))    fast2smsApiKey    = (String) data.get("fast2smsApiKey");
            if (data.containsKey("gstRate"))           gstRate           = (int) data.get("gstRate");
        } catch (IOException e) { /* keep defaults */ }
    }

    private void save() {
        File file = new File(settingsPath);
        file.getParentFile().mkdirs();
        Map<String, Object> data = new HashMap<>();
        data.put("adminUsername",     adminUsername);
        data.put("adminPasswordHash", adminPasswordHash);
        data.put("upiId",             upiId);
        data.put("upiQrCode",         upiQrCode);
        data.put("adminMobile",       adminMobile);
        data.put("fast2smsApiKey",    fast2smsApiKey);
        data.put("gstRate",           gstRate);
        try { objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, data); }
        catch (IOException e) { throw new RuntimeException("Failed to save settings", e); }
    }

    public void updateCredentials(String newUsername, String newPasswordHash) {
        if (newUsername != null && !newUsername.isBlank()) this.adminUsername = newUsername;
        if (newPasswordHash != null) this.adminPasswordHash = newPasswordHash;
        save();
    }

    public void updateUpi(String newUpiId, String newUpiQrCode) {
        this.upiId = newUpiId; this.upiQrCode = newUpiQrCode; save();
    }

    public void updateAdminMobile(String mobile) {
        this.adminMobile = mobile; save();
    }

    public void updateFast2smsKey(String key) {
        this.fast2smsApiKey = key; save();
    }

    public void updateGstRate(int rate) {
        this.gstRate = rate; save();
    }
}
