package com.store.prices.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Body of a successful price query, as defined by {@code PriceResponse} in docs/api/openapi.yaml. */
public record PriceResponse(
        long productId,
        long brandId,
        long priceList,
        // Jackson's default drops zero seconds ("2020-06-14T00:00"); the contract always has them.
        @JsonFormat(pattern = PriceController.DATE_TIME_PATTERN) LocalDateTime startDate,
        @JsonFormat(pattern = PriceController.DATE_TIME_PATTERN) LocalDateTime endDate,
        BigDecimal price,
        String currency) {
}
