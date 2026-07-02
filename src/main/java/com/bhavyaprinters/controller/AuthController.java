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
            if (payload != null && payload.containsKey("email") && !payload.containsKey("error")) return payload;
        } catch (Exception ignored) {}
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────
    // ADMIN endpoints
    // ─────────────────────────────────────────────────────────────────────

    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(@Valid @RequestBody AdminLoginInputDto input) {
        if (input.getUsername().equals(settingsService.getAdminUsername())
                && settingsService.hashPassword(input.getPassword()).equals(settingsService.getAdminPasswordHash())) {
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

    // ─────────────────────────────────────────────────────────────────────
    // BANK endpoints
    // ─────────────────────────────────────────────────────────────────────

    @PostMapping("/bank/register")
    public ResponseEntity<?> registerBank(@Valid @RequestBody BankRegisterInputDto input) {
        if (bankService.existsByEmail(input.getEmail()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponseDto("Email already registered"));
        String passwordHash = settingsService.hashPassword(input.getPassword());
        BankDto bank = bankService.createBank(input.getBankName(), input.getBranchName(), input.getGstNo(), input.getPanNo(),
                input.getAddress(), input.getMobile(), input.getEmail(), passwordHash);
        String token = tokenService.generateToken(bank.getId(), "bank");
        return ResponseEntity.status(HttpStatus.CREATED).body(new BankAuthResultDto(token, "bank", bank));
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
}
