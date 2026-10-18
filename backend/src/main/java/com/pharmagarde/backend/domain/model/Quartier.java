package com.pharmagarde.backend.domain.model;

/**
 * Quartier de Dakar, rattaché à exactement une zone de garde (RG-10).
 * Porte aussi un centre approximatif, utilisé pour ouvrir les cartes au bon endroit.
 */
public class Quartier {

    private Long id;
    private String nom; // ex: "Médina", "Almadies"
    private Long zoneId; // rattachement à une Zone
    private Double latitude; // centre approximatif du quartier
    private Double longitude;

    public Quartier() {
    }

    public Quartier(Long id, String nom, Long zoneId) {
        this(id, nom, zoneId, null, null);
    }

    public Quartier(Long id, String nom, Long zoneId, Double latitude, Double longitude) {
        this.id = id;
        this.nom = nom;
        this.zoneId = zoneId;
        this.latitude = latitude;
        this.longitude = longitude;
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
