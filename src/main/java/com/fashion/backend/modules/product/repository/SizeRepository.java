package com.fashion.backend.modules.product.repository;

import com.fashion.backend.modules.product.entity.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SizeRepository extends JpaRepository<Size, Long> {
    Optional<Size> findByName(String name);
    boolean existsByName(String name);
}
