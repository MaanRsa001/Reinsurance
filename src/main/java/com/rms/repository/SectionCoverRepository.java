package com.rms.repository;

import com.rms.domain.SectionCover;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SectionCoverRepository extends JpaRepository<SectionCover, Long> {
    List<SectionCover> findBySectionId(Long sectionId);
}
