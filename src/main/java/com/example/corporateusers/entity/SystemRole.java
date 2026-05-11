package com.example.corporateusers.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_roles")
public class SystemRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 40)
    private RoleCode code;

    @Column(nullable = false, length = 255)
    private String description;

    protected SystemRole() {
    }

    public SystemRole(RoleCode code, String description) {
        this.code = code;
        this.description = description;
    }

    public Long getId() { return id; }
    public RoleCode getCode() { return code; }
    public void setCode(RoleCode code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
