package com.pharmagarde.backend.domain.model;

import com.pharmagarde.backend.domain.enums.Role;

public class User {

    private Long id;
    private String email;
    private String password; // hash BCrypt
    private Role role;

    // Portée tenant : rattaché à UNE pharmacie (PHARMACIEN)
    private Long tenantId;

    // Portée zone : rattaché à UNE zone (SUPERVISEUR de zone)
    // null si SUPERVISEUR général ou ADMIN
    private Long zoneId;

    private boolean active;

    public User() {
    }

    public User(Long id, String email, String password, Role role, Long tenantId, Long zoneId, boolean active) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.tenantId = tenantId;
        this.zoneId = zoneId;
        this.active = active;
    }

    // Getters et setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}