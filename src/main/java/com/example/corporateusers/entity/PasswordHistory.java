package com.example.corporateusers.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_history")
public class PasswordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private SystemUser user;

    @Column(nullable = false, length = 120)
    private String passwordHash;

    @Column(nullable = false, length = 80)
    private String changedBy;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    protected PasswordHistory() {
    }

    public PasswordHistory(String passwordHash, String changedBy) {
        this.passwordHash = passwordHash;
        this.changedBy = changedBy;
    }

    @PrePersist
    void prePersist() {
        this.changedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public SystemUser getUser() { return user; }
    public void setUser(SystemUser user) { this.user = user; }
    public String getPasswordHash() { return passwordHash; }
    public String getChangedBy() { return changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
