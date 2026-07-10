package com.bhavyaprinters.controller;

import com.bhavyaprinters.dto.*;
import com.bhavyaprinters.entity.Bank;
import com.bhavyaprinters.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SettingsService     settingsService;
    private final BankService         bankService;
    private final TokenService        tokenService;
    private final NotificationService notificationService;
    private final WhatsappService whatsappService;
    private final OtpService otpService;
    private final EmailService emailService;

    @Value("${google.admin.email:}")
    private String adminGoogleEmail;

    /**
     * Your Google OAuth Client ID (the same one used by the frontend's
     * Google Identity Services button). When set, every Google idToken is
     * checked to make sure it was actually issued for THIS app, not some
     * other Google-authenticated client. Leave blank only for local testing.
     */
    @Value("${google.client.id:}")
    private String googleClientId;

    // ── In-memory OTP store ──────────────────────────────────────────────
    private record OtpEntry(String otp, long expiresAt) {}
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private static final long OTP_TTL_MS = 5 * 60 * 1000L;

    private String generateOtp() {
        return String.format("%06d", (int)(Math.random() * 1_000_000));
    }

    /** Keep only last 10 digits of mobile number for comparison */
    private String normPhone(String mobile) {
        String digits = mobile.replaceAll("[^0-9]", "");
        return digits.length() >= 10 ? digits.substring(digits.length() - 10) : digits;
    }

    // ── Verify Google ID token ────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private Map<String, Object> verifyGoogleToken(String idToken) {
        try {
            RestTemplate rest = new RestTemplate();
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
            Map<String, Object> payload = rest.getForObject(url, Map.class);
            if (payload == null || !payload.containsKey("email") || payload.containsKey("error")) {
                return null;
            }
            // Make sure this token was issued for OUR app, not some other
            // Google-authenticated client, before trusting its contents.
            if (googleClientId != null && !googleClientId.isBlank()) {
                Object aud = payload.get("aud");
                if (aud == null || !googleClientId.equals(aud.toString())) {
                    return null;
                }
            }
            return payload;
        } catch (Exception ignored) {}
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────
    // ADMIN endpoints
    // ─────────────────────────────────────────────────────────────────────

    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(@Valid @RequestBody AdminLoginInputDto input) {
        if (!settingsService.isAdminRegistered()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponseDto("No admin account exists yet. Please register first."));
        }

        String identifier = input.getUsername(); // accepts username OR email
        boolean matches = identifier.equalsIgnoreCase(settingsService.getAdminUsername())
                || identifier.equalsIgnoreCase(settingsService.getAdminEmail());

        if (matches && settingsService.hashPassword(input.getPassword()).equals(settingsService.getAdminPasswordHash())) {
            return ResponseEntity.ok(new AdminAuthResultDto(tokenService.generateToken(0, "admin"), "admin"));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponseDto("Invalid credentials"));
    }

    @PostMapping("/admin/google")
    public ResponseEntity<?> adminGoogleLogin(@RequestBody GoogleAuthInputDto input) {
        if (input.getIdToken() == null || input.getIdToken().isBlank())
            return ResponseEntity.badRequest().body(new ErrorResponseDto("idToken required"));
        Map<String, Object> payload = verifyGoogleToken(input.getIdToken());
        if (payload == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponseDto("Invalid Google token"));
        if (adminGoogleEmail == null || adminGoogleEmail.isBlank())
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDto("Google admin login not configured. Set google.admin.email in application.properties"));
        String email = (String) payload.get("email");
        if (!adminGoogleEmail.equalsIgnoreCase(email))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponseDto("This Google account is not authorised as admin."));
        return ResponseEntity.ok(new AdminAuthResultDto(tokenService.generateToken(0, "admin"), "admin"));
    }

    /** Send OTP to admin mobile via Fast2SMS */
    @PostMapping("/admin/otp/send")
    public ResponseEntity<?> adminOtpSend(@RequestBody Map<String, String> body) {
        String mobile = body.get("mobile");
        if (mobile == null || mobile.isBlank())
            return ResponseEntity.badRequest().body(new ErrorResponseDto("mobile required"));

        String adminMobile = settingsService.getAdminMobile();
        if (adminMobile != null && !adminMobile.isBlank()) {
            if (!normPhone(mobile).equals(normPhone(adminMobile)))
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponseDto("Mobile number not recognised as admin. Configure it in Settings."));
        }
        String otp = generateOtp();
        otpStore.put("admin:" + normPhone(mobile), new OtpEntry(otp, System.currentTimeMillis() + OTP_TTL_MS));

        whatsappService.sendOtp("91" + normPhone(mobile), otp);

        return ResponseEntity.ok(new MessageResponseDto("OTP sent successfully"));
    }

    /** Verify admin OTP */
    @PostMapping("/admin/otp/verify")
    public ResponseEntity<?> adminOtpVerify(@RequestBody Map<String, String> body) {
        String mobile = body.get("mobile");
        String otp    = body.get("otp");
        if (mobile == null || otp == null)
            return ResponseEntity.badRequest().body(new ErrorResponseDto("mobile and otp required"));

        String key = "admin:" + normPhone(mobile);
        OtpEntry entry = otpStore.get(key);
        if (entry == null)
            return ResponseEntity.badRequest().body(new ErrorResponseDto("No OTP sent. Please request a new OTP."));
        if (System.currentTimeMillis() > entry.expiresAt()) {
            otpStore.remove(key);
            return ResponseEntity.badRequest().body(new ErrorResponseDto("OTP expired. Please request a new OTP."));
        }
        if (!entry.otp().equals(otp.trim()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponseDto("Incorrect OTP. Please try again."));

        otpStore.remove(key);
        return ResponseEntity.ok(new AdminAuthResultDto(tokenService.generateToken(0, "admin"), "admin"));
    }

    @GetMapping("/admin/exists")
    public ResponseEntity<?> adminExists() {
        return ResponseEntity.ok(Map.of("exists", settingsService.isAdminRegistered()));
    }

    @PostMapping("/admin/register")
    public ResponseEntity<?> registerAdmin(@Valid @RequestBody AdminRegisterInputDto input) {
        if (settingsService.isAdminRegistered()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponseDto("Admin account already exists. Registration is closed."));
        }
        if (!input.getPassword().equals(input.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(new ErrorResponseDto("Passwords do not match"));
        }

        settingsService.registerAdmin(
                input.getUsername(),
                input.getEmail(),
                settingsService.hashPassword(input.getPassword())
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AdminAuthResultDto(tokenService.generateToken(0, "admin"), "admin"));
    }

    @PostMapping("/admin/forgot-password/send-otp")
    public ResponseEntity<?> sendAdminForgotOtp(@RequestBody ForgotPasswordRequest request) {
        if (!settingsService.isAdminRegistered()
                || settingsService.getAdminEmail() == null
                || !settingsService.getAdminEmail().equalsIgnoreCase(request.getEmail())) {
            return ResponseEntity.badRequest().body(new ErrorResponseDto("Email not registered"));
        }
        String otp = otpService.generateOtp(request.getEmail());
        emailService.sendOtp(request.getEmail(), otp);
        return ResponseEntity.ok(new MessageResponseDto("OTP sent successfully"));
    }

    @PostMapping("/admin/forgot-password/verify-otp")
    public ResponseEntity<?> verifyAdminForgotOtp(@RequestBody VerifyOtpRequest request) {
        if (!otpService.verifyOtp(request.getEmail(), request.getOtp()))
            return ResponseEntity.badRequest().body(new ErrorResponseDto("Invalid OTP"));
        return ResponseEntity.ok(new MessageResponseDto("OTP Verified"));
    }

    @PostMapping("/admin/forgot-password/reset-password")
    public ResponseEntity<?> resetAdminPassword(@RequestBody ResetPasswordRequest request) {
        if (!otpService.isEmailVerified(request.getEmail()))
            return ResponseEntity.badRequest().body(new ErrorResponseDto("Verify OTP first"));
        if (!request.getEmail().equalsIgnoreCase(settingsService.getAdminEmail()))
            return ResponseEntity.badRequest().body(new ErrorResponseDto("Email not registered"));

        settingsService.updateCredentials(null, settingsService.hashPassword(request.getPassword()));
        otpService.clearVerification(request.getEmail());
        return ResponseEntity.ok(new MessageResponseDto("Password updated"));
    }

    // ─────────────────────────────────────────────────────────────────────
    // BANK endpoints
    // ─────────────────────────────────────────────────────────────────────

    @PostMapping("/bank/register")
    public ResponseEntity<?> registerBank(@Valid @RequestBody BankRegisterInputDto input) {

        if (bankService.existsByEmail(input.getEmail()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDto("Email already registered"));

        if (!otpService.isEmailVerified(input.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDto("Please verify your email first."));
        }

        String passwordHash = settingsService.hashPassword(input.getPassword());

        BankDto bank = bankService.createBank(
                input.getBankName(),
                input.getBranchName(),
                input.getGstNo(),
                input.getPanNo(),
                input.getAddress(),
                input.getMobile(),
                input.getEmail(),
                passwordHash
        );

        otpService.clearVerification(input.getEmail());

        String token = tokenService.generateToken(bank.getId(), "bank");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BankAuthResultDto(token, "bank", bank));
    }

    @PostMapping("/bank/login")
    public ResponseEntity<?> bankLogin(@Valid @RequestBody BankLoginInputDto input) {
        Optional<Bank> bankOpt = bankService.findByEmail(input.getEmail());
        if (bankOpt.isEmpty() || !settingsService.hashPassword(input.getPassword()).equals(bankOpt.get().getPasswordHash()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponseDto("Invalid email or password"));
        Bank bank = bankOpt.get();
        return ResponseEntity.ok(new BankAuthResultDto(tokenService.generateToken(bank.getId(), "bank"), "bank", bankService.toDto(bank)));
    }

    @PostMapping("/bank/google")
    public ResponseEntity<?> bankGoogleLogin(@RequestBody GoogleAuthInputDto input) {
        if (input.getIdToken() == null || input.getIdToken().isBlank())
            return ResponseEntity.badRequest().body(new ErrorResponseDto("idToken required"));
        Map<String, Object> payload = verifyGoogleToken(input.getIdToken());
        if (payload == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponseDto("Invalid Google token"));
        String email = (String) payload.get("email");
        Optional<Bank> bankOpt = bankService.findByEmail(email);
        if (bankOpt.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Bank not registered. Please complete registration.", "email", email, "name", payload.getOrDefault("name", email)));
        Bank bank = bankOpt.get();
        return ResponseEntity.ok(new BankAuthResultDto(tokenService.generateToken(bank.getId(), "bank"), "bank", bankService.toDto(bank)));
    }

    /**
     * Completes registration for a bank whose Google account isn't linked
     * to an existing bank yet. The idToken is re-verified here (never trust
     * a client-supplied email) and the verified email becomes the bank's
     * login email. A random password hash is stored since this account will
     * only ever authenticate via Google — the bank can set a real password
     * later from their profile if you want to support both.
     */
    @PostMapping("/bank/google-register")
    public ResponseEntity<?> bankGoogleRegister(@Valid @RequestBody BankGoogleRegisterInputDto input) {
        Map<String, Object> payload = verifyGoogleToken(input.getIdToken());
        if (payload == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponseDto("Invalid Google token"));

        String email = (String) payload.get("email");

        if (bankService.existsByEmail(email))
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponseDto("An account with this email already exists. Please sign in instead."));

        String randomPasswordHash = settingsService.hashPassword(UUID.randomUUID().toString());

        BankDto bank = bankService.createBank(
                input.getBankName(),
                input.getBranchName(),
                input.getGstNo(),
                input.getPanNo(),
                input.getAddress(),
                input.getMobile(),
                email,
                randomPasswordHash
        );

        String token = tokenService.generateToken(bank.getId(), "bank");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new BankAuthResultDto(token, "bank", bank));
    }

    /** Send OTP to bank's registered mobile */
    @PostMapping("/bank/otp/send")
    public ResponseEntity<?> bankOtpSend(@RequestBody Map<String, String> body) {
        String mobile = body.get("mobile");
        if (mobile == null || mobile.isBlank())
            return ResponseEntity.badRequest().body(new ErrorResponseDto("mobile required"));

        String norm = normPhone(mobile);
        Optional<Bank> matchingBank = bankService.findAll().stream()
                .filter(b -> normPhone(b.getMobile()).equals(norm))
                .findFirst();
        if (matchingBank.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponseDto("No bank account found with this mobile number. Please register first."));

        String otp = generateOtp();
        otpStore.put("bank:" + norm, new OtpEntry(otp, System.currentTimeMillis() + OTP_TTL_MS));

        whatsappService.sendOtp("91" + normPhone(mobile), otp);

        return ResponseEntity.ok(new MessageResponseDto("OTP sent successfully"));
    }

    /** Verify bank OTP */
    @PostMapping("/bank/otp/verify")
    public ResponseEntity<?> bankOtpVerify(@RequestBody Map<String, String> body) {
        String mobile = body.get("mobile");
        String otp    = body.get("otp");
        if (mobile == null || otp == null)
            return ResponseEntity.badRequest().body(new ErrorResponseDto("mobile and otp required"));

        String norm = normPhone(mobile);
        String key  = "bank:" + norm;
        OtpEntry entry = otpStore.get(key);
        if (entry == null)
            return ResponseEntity.badRequest().body(new ErrorResponseDto("No OTP sent. Please request a new OTP."));
        if (System.currentTimeMillis() > entry.expiresAt()) {
            otpStore.remove(key);
            return ResponseEntity.badRequest().body(new ErrorResponseDto("OTP expired. Please request a new OTP."));
        }
        if (!entry.otp().equals(otp.trim()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponseDto("Incorrect OTP. Please try again."));

        otpStore.remove(key);

        Optional<Bank> bankOpt = bankService.findAll().stream()
                .filter(b -> normPhone(b.getMobile()).equals(norm))
                .findFirst();
        if (bankOpt.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDto("Bank not found"));

        Bank bank = bankOpt.get();
        return ResponseEntity.ok(new BankAuthResultDto(tokenService.generateToken(bank.getId(), "bank"), "bank", bankService.toDto(bank)));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody SendOtpRequest request){

        String otp = otpService.generateOtp(request.getEmail());

        emailService.sendOtp(request.getEmail(),otp);

        return ResponseEntity.ok(new MessageResponseDto("OTP Sent Successfully"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest request) {

        boolean verified = otpService.verifyOtp(
                request.getEmail(),
                request.getOtp()
        );

        if (!verified)
            return ResponseEntity.badRequest()
                    .body(new MessageResponseDto("Invalid or Expired OTP"));

        return ResponseEntity.ok(
                new MessageResponseDto("Login Successful")
        );
    }

    @PostMapping("/bank/forgot-password/verify-otp")
    public ResponseEntity<?> verifyForgotOtp(
            @RequestBody VerifyOtpRequest request){

        if(!otpService.verifyOtp(
                request.getEmail(),
                request.getOtp()))
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDto("Invalid OTP"));

        return ResponseEntity.ok(
                new MessageResponseDto("OTP Verified"));

    }
    @PostMapping("/bank/forgot-password/send-otp")
    public ResponseEntity<?> sendForgotOtp(
            @RequestBody ForgotPasswordRequest request){

        if(!bankService.existsByEmail(request.getEmail()))
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDto("Email not registered"));

        String otp = otpService.generateOtp(request.getEmail());

        emailService.sendOtp(request.getEmail(),otp);

        return ResponseEntity.ok(
                new MessageResponseDto("OTP sent successfully"));
    }
    @PostMapping("/bank/forgot-password/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestBody ResetPasswordRequest request){

        if(!otpService.isEmailVerified(request.getEmail()))
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseDto("Verify OTP first"));

        Bank bank = bankService.findEntityByEmail(request.getEmail());

        bank.setPassword(
                settingsService.hashPassword(
                        request.getPassword()
                )
        );

        bankService.save(bank);

        otpService.clearVerification(request.getEmail());

        return ResponseEntity.ok(
                new MessageResponseDto("Password updated"));

    }
}
