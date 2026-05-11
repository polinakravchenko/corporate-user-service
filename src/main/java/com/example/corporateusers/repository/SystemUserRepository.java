package com.example.corporateusers.repository;

import com.example.corporateusers.entity.SystemUser;
import com.example.corporateusers.entity.UserStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SystemUserRepository extends JpaRepository<SystemUser, Long> {

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    Optional<SystemUser> findByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = "roles")
    Optional<SystemUser> findWithRolesByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = "roles")
    Optional<SystemUser> findWithRolesByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "roles")
    List<SystemUser> findByStatusOrderByCreatedAtDesc(UserStatus status);

    @EntityGraph(attributePaths = "roles")
    @Query("select u from SystemUser u order by u.createdAt desc")
    List<SystemUser> findAllWithRolesOrderByCreatedAtDesc();

    @Query("""
            select distinct u
            from SystemUser u
            left join fetch u.roles r
            where lower(u.username) like lower(concat('%', :query, '%'))
               or lower(u.email) like lower(concat('%', :query, '%'))
               or lower(u.fullName) like lower(concat('%', :query, '%'))
            order by u.createdAt desc
            """)
    List<SystemUser> search(@Param("query") String query);

    @EntityGraph(attributePaths = {"passwordHistory", "roles"})
    @Query("select u from SystemUser u where u.id = :id")
    Optional<SystemUser> findByIdWithPasswordHistory(@Param("id") Long id);
}
