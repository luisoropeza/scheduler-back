package com.example.user.repository;

import com.example.user.entity.Personal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PersonalRepository extends JpaRepository<Personal, Long> {

    @Query("SELECT p FROM Personal p WHERE " +
            "(:specialtyId IS NULL OR p.specialty.id = :specialtyId) AND " +
            "(:isActive IS NULL OR p.active = :isActive)")
    Page<Personal> findAllByFilters(@Param("specialtyId") Long specialtyId, @Param("isActive") Boolean isActive, Pageable pageable);
    Optional<Personal> findByEmail(String email);
}
