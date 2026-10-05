package com.hardwarestore.hardwarestore.repository;

import com.hardwarestore.hardwarestore.model.PendingRegistration;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, String> {
    boolean existsByEmail(String email);
    void deleteByCreatedAtBefore(java.time.Instant cutoff);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PendingRegistration p where p.id = :id")
    Optional<PendingRegistration> findForUpdate(@Param("id") String id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PendingRegistration> findByEmail(String email);
}
