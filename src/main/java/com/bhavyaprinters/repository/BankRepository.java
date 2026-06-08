package com.bhavyaprinters.repository;

import com.bhavyaprinters.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankRepository extends JpaRepository<Bank, Long> {
    List<Bank> findAllByOrderByCreatedAtAsc();
    Optional<Bank> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByGstNo(String gstNo);
}
