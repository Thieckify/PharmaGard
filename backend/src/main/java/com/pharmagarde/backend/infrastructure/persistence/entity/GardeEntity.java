package com.pharmagarde.backend.infrastructure.persistence.entity;

import com.pharmagarde.backend.domain.enums.TypeGarde;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "gardes")
public class GardeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacie_id", nullable = false)
    private PharmacieEntity pharmacie;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeGarde type;

    public GardeEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PharmacieEntity getPharmacie() {
        return pharmacie;
    }

    public void setPharmacie(PharmacieEntity pharmacie) {
        this.pharmacie = pharmacie;
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