package com.mockexams.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mockexams.backend.entity.PlatformSetting;

public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, Long> {

	List<PlatformSetting> findAllByOrderBySettingKeyAsc();

	Optional<PlatformSetting> findBySettingKey(String settingKey);
}
