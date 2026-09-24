package com.store.prices.infrastructure.adapter.out.persistence;

import com.store.prices.domain.model.Price;
import com.store.prices.domain.port.PriceRepositoryPort;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Implements the domain's price port with JPA. */
@Component
public class PricePersistenceAdapter implements PriceRepositoryPort {

    private final PriceJpaRepository repository;
    private final PriceEntityMapper mapper;

    public PricePersistenceAdapter(PriceJpaRepository repository, PriceEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Price> findCandidates(long brandId, long productId, LocalDateTime applicationDate) {
        return repository.findCandidates(brandId, productId, applicationDate).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
