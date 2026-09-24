package com.store.prices.domain.port;

import com.store.prices.domain.model.Price;
import java.time.LocalDateTime;
import java.util.List;

/** Outbound port to the price store. */
public interface PriceRepositoryPort {

    /**
     * Returns candidate prices of the brand and product for the application date.
     * The result may include prices that don't actually apply, comes in no particular order, and is empty
     * if there are none. Choosing the applicable price is left to the domain.
     */
    List<Price> findCandidates(long brandId, long productId, LocalDateTime applicationDate);
}
