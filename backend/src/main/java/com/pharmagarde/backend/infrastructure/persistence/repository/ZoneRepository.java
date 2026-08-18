package com.pharmagarde.backend.infrastructure.persistence.repository;

import com.pharmagarde.backend.infrastructure.persistence.entity.ZoneEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZoneRepository extends JpaRepository<ZoneEntity, Long> {
}