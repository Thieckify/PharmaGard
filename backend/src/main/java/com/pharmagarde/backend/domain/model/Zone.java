package com.pharmagarde.backend.domain.model;

public class Zone {

    private Long id;
    private String nom; // ex: "Plateau", "Ouakam - Ngor"

    public Zone() {
    }

    public Zone(Long id, String nom) {
        this.id = id;
        this.nom = nom;
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
}