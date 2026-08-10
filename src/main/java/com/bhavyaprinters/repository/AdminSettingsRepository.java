package com.bhavyaprinters.repository;

import com.bhavyaprinters.entity.AdminSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AdminSettingsRepository extends JpaRepository<AdminSettings,Long>
{
    @Modifying
    @Transactional
    @Query("""
        UPDATE AdminSettings s
        SET s.upiId = :upiId,
            s.upiQrCode = :upiQrCode
        WHERE s.id = 1
    """)
    int updateUpiSettings(
            @Param("upiId") String upiId,
            @Param("upiQrCode") String upiQrCode
    );
}
