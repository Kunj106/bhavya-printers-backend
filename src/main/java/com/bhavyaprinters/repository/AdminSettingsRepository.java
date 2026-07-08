package com.bhavyaprinters.repository;

import com.bhavyaprinters.entity.AdminSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminSettingsRepository extends JpaRepository<AdminSettings,Long> {
}
