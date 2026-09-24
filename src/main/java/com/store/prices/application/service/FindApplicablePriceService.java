package com.store.prices.application.service;

import com.store.prices.application.port.in.FindApplicablePriceUseCase;
import com.store.prices.application.port.in.PriceQuery;
import com.store.prices.domain.exception.PriceNotFoundException;
import com.store.prices.domain.model.Price;
import com.store.prices.domain.port.PriceRepositoryPort;
import com.store.prices.domain.service.PriceSelector;
import java.util.Objects;

/** Loads the candidate prices from the port and lets the domain pick the one that applies. */
public final class FindApplicablePriceService implements FindApplicablePriceUseCase {

    private final PriceRepositoryPort priceRepository;
    private final PriceSelector priceSelector;

    public FindApplicablePriceService(PriceRepositoryPort priceRepository, PriceSelector priceSelector) {
        this.priceRepository = Objects.requireNonNull(priceRepository, "priceRepository must not be null");
        this.priceSelector = Objects.requireNonNull(priceSelector, "priceSelector must not be null");
    }

    @Override
    public Price find(PriceQuery query) {
        var candidates = priceRepository.findCandidates(query.brandId(), query.productId(), query.applicationDate());
        return priceSelector.select(candidates, query.applicationDate())
                .orElseThrow(() -> new PriceNotFoundException(query.brandId(), query.productId(),
                        query.applicationDate()));
    }
}
