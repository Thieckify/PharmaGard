package com.pharmagarde.backend.domain.model;

public class Quartier {

    private Long id;
    private String nom; // ex: "Médina", "Almadies"
    private Long zoneId; // rattachement à une Zone

    public Quartier() {
    }

    public Quartier(Long id, String nom, Long zoneId) {
        this.id = id;
        this.nom = nom;
        this.zoneId = zoneId;
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

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }
}