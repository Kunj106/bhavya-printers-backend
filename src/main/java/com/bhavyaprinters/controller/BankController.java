package com.bhavyaprinters.controller;

import com.bhavyaprinters.dto.*;
import com.bhavyaprinters.service.BankService;
import com.bhavyaprinters.service.SettingsService;
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
}
