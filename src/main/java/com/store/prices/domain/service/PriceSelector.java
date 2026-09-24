package com.store.prices.domain.service;

import com.store.prices.domain.model.Price;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Picks the price that applies at a date from a list of candidates. */
public final class PriceSelector {

    // Highest priority wins; ties go to the latest start date, then to the highest price list, so the
    // result is deterministic whatever order the candidates arrive in.
    private static final Comparator<Price> PRECEDENCE = Comparator.comparingInt(Price::priority)
            .thenComparing(Price::startDate)
            .thenComparingLong(Price::priceList);

    /**
     * Returns the applicable price, or empty if none of the candidates applies at the date.
     * The date filter is applied here even if the candidates were pre-filtered, so the rule is complete
     * in the domain.
     */
    public Optional<Price> select(List<Price> candidates, LocalDateTime applicationDate) {
        return candidates.stream()
                .filter(price -> price.isApplicableAt(applicationDate))
                .max(PRECEDENCE);
    }
}
