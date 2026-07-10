package com.bhavyaprinters.service;

import com.bhavyaprinters.dto.BankDto;
import com.bhavyaprinters.dto.BankProfileUpdateDto;
import com.bhavyaprinters.entity.Bank;
import com.bhavyaprinters.repository.BankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BankService {

    private final BankRepository bankRepository;

    public List<BankDto> listBanks() {
        return bankRepository.findAllByOrderByCreatedAtAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public Optional<BankDto> getBank(Long id) {
        return bankRepository.findById(id).map(this::toDto);
    }

    @Transactional
    public BankDto createBank(String bankName, String branchName, String gstNo, String panNo,
                              String address, String mobile, String email, String passwordHash) {
        Bank b = new Bank();
        b.setBankName(bankName);
        b.setBranchName(branchName);
        b.setGstNo(gstNo);
        b.setPanNo(panNo);
        b.setAddress(address);
        b.setMobile(mobile);
        b.setEmail(email);
        b.setPasswordHash(passwordHash);
        return toDto(bankRepository.save(b));
    }

    @Transactional
    public boolean deleteBank(Long id) {
        if (!bankRepository.existsById(id)) return false;
        bankRepository.deleteById(id);
        return true;
    }

    public Optional<Bank> findByEmail(String email) {
        return bankRepository.findByEmail(email);
    }

    public boolean existsByEmail(String email) {
        return bankRepository.existsByEmail(email);
    }

    /** Raw entity lookup — used by controllers that need to check a password hash directly. */
    public Optional<Bank> findEntity(Long id) {
        return bankRepository.findById(id);
    }

    /**
     * Partial profile update — only non-null fields are changed, so a bank
     * can update just their address without resending everything else.
     * Returns empty if the bank doesn't exist, or a special "conflict" via
     * IllegalStateException if the new email is already used by another bank.
     */
    @Transactional
    public Optional<BankDto> updateProfile(Long id, BankProfileUpdateDto input) {
        return bankRepository.findById(id).map(b -> {
            if (input.getAddress() != null && !input.getAddress().isBlank()) {
                b.setAddress(input.getAddress());
            }
            if (input.getMobile() != null && !input.getMobile().isBlank()) {
                b.setMobile(input.getMobile());
            }
            if (input.getGstNo() != null && !input.getGstNo().isBlank()) {
                b.setGstNo(input.getGstNo());
            }
            if (input.getPanNo() != null && !input.getPanNo().isBlank()) {
                b.setPanNo(input.getPanNo());
            }
            if (input.getEmail() != null && !input.getEmail().isBlank()
                    && !input.getEmail().equalsIgnoreCase(b.getEmail())) {
                Optional<Bank> conflicting = bankRepository.findByEmail(input.getEmail());
                if (conflicting.isPresent() && !conflicting.get().getId().equals(id)) {
                    throw new IllegalStateException("Email already in use by another bank");
                }
                b.setEmail(input.getEmail());
            }
            return toDto(bankRepository.save(b));
        });
    }

    public BankDto toDto(Bank b) {
        return new BankDto(
                b.getId(),
                b.getBankName(),
                b.getBranchName(),
                b.getGstNo(),
                b.getPanNo(),
                b.getAddress(),
                b.getMobile(),
                b.getEmail(),
                b.getCreatedAt().toString()
        );
    }

    public List<Bank> findAll() {
        return bankRepository.findAllByOrderByCreatedAtAsc();
    }

    public Bank findEntityByEmail(String email){
        return bankRepository.findByEmail(email)
                .orElseThrow();
    }

    public void save(Bank bank){
        bankRepository.save(bank);
    }
}