package com.bhavyaprinters.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_settings")
public class AdminSettings
{
    @Id
    private Long id = 1L; // singleton row — this app only ever has one admin

    private String adminUsername;
    private String adminEmail;
    private String adminPasswordHash;
    private boolean adminRegistered = false;

    private String upiId = "bhavyaprinters@sbi";
    private String upiQrCode;
    private String adminMobile;
    private String fast2smsApiKey;
    private int gstRate = 18;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getAdminPasswordHash() {
        return adminPasswordHash;
    }

    public void setAdminPasswordHash(String adminPasswordHash) {
        this.adminPasswordHash = adminPasswordHash;
    }

    public boolean isAdminRegistered() {
        return adminRegistered;
    }

    public void setAdminRegistered(boolean adminRegistered) {
        this.adminRegistered = adminRegistered;
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public String getUpiQrCode() {
        return upiQrCode;
    }

    public void setUpiQrCode(String upiQrCode) {
        this.upiQrCode = upiQrCode;
    }

    public String getAdminMobile() {
        return adminMobile;
    }

    public void setAdminMobile(String adminMobile) {
        this.adminMobile = adminMobile;
    }

    public String getFast2smsApiKey() {
        return fast2smsApiKey;
    }

    public void setFast2smsApiKey(String fast2smsApiKey) {
        this.fast2smsApiKey = fast2smsApiKey;
    }

    public int getGstRate() {
        return gstRate;
    }

    public void setGstRate(int gstRate) {
        this.gstRate = gstRate;
    }
}
