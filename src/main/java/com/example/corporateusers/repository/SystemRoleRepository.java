package com.example.corporateusers.repository;

import com.example.corporateusers.entity.RoleCode;
import com.example.corporateusers.entity.SystemRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemRoleRepository extends JpaRepository<SystemRole, Long> {

    Optional<SystemRole> findByCode(RoleCode code);
}
