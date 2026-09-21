package com.pharmagarde.backend.domain.model;

import com.pharmagarde.backend.domain.enums.TypeGarde;
import java.time.LocalDate;

/**
 * Représente une période de garde déclarée par une pharmacie : une plage de dates
 * pendant laquelle elle assure le service de garde (nuit, dimanche/férié, ou les deux).
 */
public class Garde {

    private Long id;
    private Long pharmacieId;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private TypeGarde type;

    public Garde() {
    }

    public Garde(Long id, Long pharmacieId, LocalDate dateDebut, LocalDate dateFin, TypeGarde type) {
        this.id = id;
        this.pharmacieId = pharmacieId;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPharmacieId() {
        return pharmacieId;
    }

    public void setPharmacieId(Long pharmacieId) {
        this.pharmacieId = pharmacieId;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public TypeGarde getType() {
        return type;
    }

    public void setType(TypeGarde type) {
        this.type = type;
    }
}