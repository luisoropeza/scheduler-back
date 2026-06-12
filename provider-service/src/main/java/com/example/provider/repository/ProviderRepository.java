package com.example.provider.repository;

import com.example.provider.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {
    List<Provider> findByActiveTrue();
    List<Provider> findBySpecialtyIgnoreCaseAndActiveTrue(String specialty);
}
