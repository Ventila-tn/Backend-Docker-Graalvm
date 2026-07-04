package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingRepository extends JpaRepository<Setting, String> {}
