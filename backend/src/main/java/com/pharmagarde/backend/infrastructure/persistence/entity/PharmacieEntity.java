package com.pharmagarde.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "pharmacies")
public class PharmacieEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String adresse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quartier_id", nullable = false)
    private QuartierEntity quartier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private ZoneEntity zone;

    private Double latitude;
    private Double longitude;

    private String telephone;

    @Column(name = "horaire_ouverture")
    private String horaireOuverture;

    @Column(name = "horaire_fermeture")
    private String horaireFermeture;

    @Column(nullable = false)
    private boolean active = true;

    // Tenant : le compte PHARMACIEN propriétaire
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_user_id", nullable = false)
    private UserEntity tenantUser;

    public PharmacieEntity() {
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

    public QuartierEntity getQuartier() {
        return quartier;
    }

    public void setQuartier(QuartierEntity quartier) {
        this.quartier = quartier;
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

    public UserEntity getTenantUser() {
        return tenantUser;
    }

    public void setTenantUser(UserEntity tenantUser) {
        this.tenantUser = tenantUser;
    }
}