package com.rms.repository;

import com.rms.domain.ProductSection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSectionRepository extends JpaRepository<ProductSection, Long> {
    List<ProductSection> findByProductId(Long productId);
}
