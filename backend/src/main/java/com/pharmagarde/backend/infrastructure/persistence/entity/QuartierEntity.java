package com.pharmagarde.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;

/**
 * Entité JPA d'un quartier de Dakar, rattaché à une zone de garde. Porte aussi le centre
 * approximatif du quartier (latitude/longitude), utilisé pour recentrer les cartes du frontend.
 */
@Entity
@Table(name = "quartiers")
public class QuartierEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private ZoneEntity zone;

    // Centre approximatif du quartier (null si inconnu). Colonnes ajoutées sur une table déjà
    // remplie : volontairement nullables, jamais NOT NULL (piège rencontré en S3).
    private Double latitude;

    private Double longitude;

    public QuartierEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public ZoneEntity getZone() {
        return zone;
    }

    public void setZone(ZoneEntity zone) {
        this.zone = zone;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}