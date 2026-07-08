package com.bhavyaprinters.repository;

import com.bhavyaprinters.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp,Long>
{
    Optional<Otp> findByEmail(String email);

    @Modifying
    @Transactional
    void deleteByEmail(String email);
}
