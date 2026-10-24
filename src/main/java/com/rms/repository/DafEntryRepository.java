package com.rms.repository;

import com.rms.domain.DafEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DafEntryRepository extends JpaRepository<DafEntry, Long> {
}
