package com.bhavyaprinters.controller;

import com.bhavyaprinters.dto.*;
import com.bhavyaprinters.entity.Bank;
import com.bhavyaprinters.service.BankService;
import com.bhavyaprinters.service.SettingsService;
import com.bhavyaprinters.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/banks")
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;
    private final SettingsService settingsService;
    private final TokenService tokenService;

    @GetMapping
    public ResponseEntity<List<BankDto>> listBanks() {
        return ResponseEntity.ok(bankService.listBanks());
    }

    @PostMapping
    public ResponseEntity<?> createBank(@RequestBody Map<String, String> body) {
        String bankName = body.get("bankName");
        String branchName = body.get("branchName");
        String gstNo = body.get("gstNo");
        String panNo = body.get("panNo");
        String address = body.get("address");
        String mobile = body.get("mobile");
        String email = body.get("email");
        String password = body.getOrDefault("password", "bhavya1996");

        if (bankName == null || branchName == null || gstNo == null || panNo == null
                || address == null || mobile == null || email == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponseDto("All fields are required"));
        }

        if (bankService.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponseDto("A bank with this email already exists"));
        }

        String passwordHash = settingsService.hashPassword(password);
        BankDto bank = bankService.createBank(bankName, branchName, gstNo, panNo, address, mobile, email, passwordHash);
        return ResponseEntity.status(HttpStatus.CREATED).body(bank);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBank(@PathVariable Long id) {
        return bankService.getBank(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponseDto("Bank not found")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBank(@PathVariable Long id) {
        if (bankService.deleteBank(id)) {
            return ResponseEntity.ok(new MessageResponseDto("Bank deleted successfully"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDto("Bank not found"));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Self-service profile & password endpoints
    // ─────────────────────────────────────────────────────────────────────

    @PutMapping("/{id}/profile")
    public ResponseEntity<?> updateProfile(@PathVariable Long id,
                                           @RequestHeader(value = "Authorization", required = false) String authHeader,
                                           @Valid @RequestBody BankProfileUpdateDto input) {
        if (!isAuthorizedBank(authHeader, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponseDto("Not authorized to edit this bank"));
        }

        try {
            return bankService.updateProfile(id, input)
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(new ErrorResponseDto("Bank not found")));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponseDto(e.getMessage()));
        }
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<?> updatePassword(@PathVariable Long id,
                                            @RequestHeader(value = "Authorization", required = false) String authHeader,
                                            @Valid @RequestBody BankPasswordUpdateDto input) {
        if (!isAuthorizedBank(authHeader, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponseDto("Not authorized to edit this bank"));
        }

        Bank bank = bankService.findEntity(id).orElse(null);
        if (bank == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDto("Bank not found"));
        }

        if (!settingsService.hashPassword(input.getCurrentPassword()).equals(bank.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponseDto("Current password is incorrect"));
        }

        bank.setPasswordHash(settingsService.hashPassword(input.getNewPassword()));
        bankService.save(bank);

        return ResponseEntity.ok(new MessageResponseDto("Password updated successfully"));
    }

    /**
     * Confirms the caller's token identifies them as the bank they're trying
     * to edit. See TokenService's class-level note: this stops the UI from
     * letting a logged-in bank edit a different bank's data by mistake or by
     * tampering with a request, but is not cryptographically unforgeable.
     */
    private boolean isAuthorizedBank(String authHeader, Long bankId) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return false;
        try {
            var decoded = tokenService.decodeToken(authHeader.substring(7));
            return "bank".equals(decoded.role()) && decoded.id() == bankId;
        } catch (Exception e) {
            return false;
        }
    }
}
