package com.store.prices.infrastructure.adapter.in.rest;

import com.store.prices.application.port.in.FindApplicablePriceUseCase;
import com.store.prices.application.port.in.PriceQuery;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The price query endpoint. The contract is docs/api/openapi.yaml, so there are no OpenAPI annotations here. */
@RestController
public class PriceController {

    /** Whole seconds only (D13). {@code iso = DATE_TIME} would also accept fractional seconds. */
    static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    // An explicit message keeps the problem detail in English whatever the locale of the request or the JVM.
    private static final String ID_MIN_MESSAGE = "must be greater than or equal to 1";

    private final FindApplicablePriceUseCase findApplicablePrice;
    private final PriceRestMapper mapper;

    public PriceController(FindApplicablePriceUseCase findApplicablePrice, PriceRestMapper mapper) {
        this.findApplicablePrice = findApplicablePrice;
        this.mapper = mapper;
    }

    @GetMapping("/api/v1/prices")
    public PriceResponse getApplicablePrice(
            // Without fallbackPatterns, Spring retries a failed pattern with ISO parsing, which accepts fractional
            // seconds (B12). Giving the same pattern as the only fallback turns that retry off.
            @RequestParam @DateTimeFormat(pattern = DATE_TIME_PATTERN, fallbackPatterns = DATE_TIME_PATTERN)
            LocalDateTime applicationDate,
            @RequestParam @Min(value = 1, message = ID_MIN_MESSAGE) long productId,
            @RequestParam @Min(value = 1, message = ID_MIN_MESSAGE) long brandId) {
        return mapper.toResponse(findApplicablePrice.find(new PriceQuery(applicationDate, productId, brandId)));
    }
}
