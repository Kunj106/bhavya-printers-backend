package com.bhavyaprinters.service;

import com.bhavyaprinters.dto.BankDto;
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