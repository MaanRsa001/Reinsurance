package com.rms.repository;

import com.rms.domain.RefLookupType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefLookupTypeRepository extends JpaRepository<RefLookupType, Long> {
    Optional<RefLookupType> findByCode(String code);
}
