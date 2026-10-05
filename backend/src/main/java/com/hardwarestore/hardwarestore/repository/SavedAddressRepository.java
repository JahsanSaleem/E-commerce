package com.hardwarestore.hardwarestore.repository;
import com.hardwarestore.hardwarestore.model.SavedAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SavedAddressRepository extends JpaRepository<SavedAddress,Long> {
 List<SavedAddress> findByUserIdOrderByIdAsc(Long userId);
 long countByUserId(Long userId);
}
