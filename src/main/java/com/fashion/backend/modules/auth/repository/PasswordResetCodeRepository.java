package com.fashion.backend.modules.auth.repository;

import com.fashion.backend.modules.auth.entity.PasswordResetCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {

    Optional<PasswordResetCode> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from PasswordResetCode c where c.user.id = :userId")
    Optional<PasswordResetCode> findByUserIdForUpdate(@Param("userId") Long userId);

    void deleteByUserId(Long userId);
}
