package com.example.scheduler.repository;

import com.example.scheduler.entity.Role;
import com.example.scheduler.enums.ERole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(ERole name);
    Role getByName(ERole name);
}
