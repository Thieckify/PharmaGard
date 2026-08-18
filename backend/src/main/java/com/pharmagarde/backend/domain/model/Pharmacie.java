package com.pharmagarde.backend.domain.model;

public class Pharmacie {

    private Long id;
    private String nom;
    private String adresse;
    private Long quartierId;
    private Long zoneId;

    private Double latitude;
    private Double longitude;

    private String telephone;
    private String horaireOuverture; // ex: "08:00"
    private String horaireFermeture; // ex: "23:00"

    private boolean active;

    // Tenant : le compte PHARMACIEN propriétaire de cette pharmacie
    private Long tenantUserId;

    public Pharmacie() {
    }

    public Pharmacie(Long id, String nom, String adresse, Long quartierId, Long zoneId,
                      Double latitude, Double longitude, String telephone,
                      String horaireOuverture, String horaireFermeture,
                      boolean active, Long tenantUserId) {
        this.id = id;
        this.nom = nom;
        this.adresse = adresse;
        this.quartierId = quartierId;
        this.zoneId = zoneId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.telephone = telephone;
        this.horaireOuverture = horaireOuverture;
        this.horaireFermeture = horaireFermeture;
        this.active = active;
        this.tenantUserId = tenantUserId;
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

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public Long getQuartierId() {
        return quartierId;
    }

    public void setQuartierId(Long quartierId) {
        this.quartierId = quartierId;
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

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getHoraireOuverture() {
        return horaireOuverture;
    }

    public void setHoraireOuverture(String horaireOuverture) {
        this.horaireOuverture = horaireOuverture;
    }

    public String getHoraireFermeture() {
        return horaireFermeture;
    }

    public void setHoraireFermeture(String horaireFermeture) {
        this.horaireFermeture = horaireFermeture;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getTenantUserId() {
        return tenantUserId;
    }

    public void setTenantUserId(Long tenantUserId) {
        this.tenantUserId = tenantUserId;
    }
}