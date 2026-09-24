package com.store.prices.infrastructure.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data access to {@code PRICES}. */
public interface PriceJpaRepository extends JpaRepository<PriceEntity, Long> {

    /**
     * Pre-filters the prices of a brand and product whose range contains the date. It never orders by priority or
     * limits the result: choosing the winner is left to the domain.
     */
    @Query("""
            SELECT p FROM PriceEntity p
            WHERE p.brandId = :brandId
              AND p.productId = :productId
              AND p.startDate <= :date
              AND p.endDate >= :date
            """)
    List<PriceEntity> findCandidates(@Param("brandId") long brandId, @Param("productId") long productId,
            @Param("date") LocalDateTime date);
}
