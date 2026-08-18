package com.pharmagarde.backend.infrastructure.persistence.repository;

import com.pharmagarde.backend.infrastructure.persistence.entity.GardeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface GardeRepository extends JpaRepository<GardeEntity, Long> {
    List<GardeEntity> findByPharmacieId(Long pharmacieId);
    List<GardeEntity> findByDateDebutLessThanEqualAndDateFinGreaterThanEqual(LocalDate date1, LocalDate date2);
}