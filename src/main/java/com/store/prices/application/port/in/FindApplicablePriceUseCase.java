package com.store.prices.application.port.in;

import com.store.prices.domain.exception.PriceNotFoundException;
import com.store.prices.domain.model.Price;

/** Inbound port: finds the price that applies to a product of a brand at a date. */
public interface FindApplicablePriceUseCase {

    /**
     * Returns the applicable price.
     *
     * @throws PriceNotFoundException if no price applies
     */
    Price find(PriceQuery query);
}
