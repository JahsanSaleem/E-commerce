package com.hardwarestore.hardwarestore.repository;

import com.hardwarestore.hardwarestore.model.PasswordReset;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PasswordReset r where r.email = :email")
    Optional<PasswordReset> findEmailForUpdate(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PasswordReset r where r.resetId = :id")
    Optional<PasswordReset> findResetForUpdate(@Param("id") String id);

    void deleteByCreatedAtBefore(Instant cutoff);
}
