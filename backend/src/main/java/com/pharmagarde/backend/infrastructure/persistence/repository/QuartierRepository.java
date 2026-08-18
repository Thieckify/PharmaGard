package com.pharmagarde.backend.infrastructure.persistence.repository;

import com.pharmagarde.backend.infrastructure.persistence.entity.QuartierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuartierRepository extends JpaRepository<QuartierEntity, Long> {
    List<QuartierEntity> findByZoneId(Long zoneId);
}