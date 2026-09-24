package com.store.prices.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.Objects;

/**
 * A price list that applies to a product of a brand within a date range.
 *
 * @param brandId   brand the price belongs to
 * @param productId product the price belongs to
 * @param priceList identifier of the price list
 * @param startDate start of the range in which the price applies (inclusive)
 * @param endDate   end of the range in which the price applies (inclusive)
 * @param priority  disambiguator: when several prices apply, the highest value wins
 * @param amount    final sale price
 * @param currency  currency of the amount
 */
public record Price(
        long brandId,
        long productId,
        long priceList,
        LocalDateTime startDate,
        LocalDateTime endDate,
        int priority,
        BigDecimal amount,
        Currency currency) {

    public Price {
        Objects.requireNonNull(startDate, "startDate must not be null");
        Objects.requireNonNull(endDate, "endDate must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
        if (priority < 0) {
            throw new IllegalArgumentException("priority must not be negative");
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
    }

    /** Whether this price applies at the given date. Both ends of the range are inclusive. */
    public boolean isApplicableAt(LocalDateTime date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}
