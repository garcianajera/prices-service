package com.store.prices.application.service;

import static com.store.prices.domain.PriceFixtures.BRAND_ID;
import static com.store.prices.domain.PriceFixtures.PRICE_LIST_2;
import static com.store.prices.domain.PriceFixtures.PRODUCT_ID;
import static com.store.prices.domain.PriceFixtures.SEED_PRICES;
import static com.store.prices.domain.PriceFixtures.price;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.store.prices.application.port.in.PriceQuery;
import com.store.prices.domain.exception.PriceNotFoundException;
import com.store.prices.domain.model.Price;
import com.store.prices.domain.port.PriceRepositoryPort;
import com.store.prices.domain.service.PriceSelector;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * T3: the use case, without Spring or a database. Only the port is mocked: the selector is pure domain logic,
 * so the real one is used.
 */
class FindApplicablePriceServiceTest {

    private static final LocalDateTime DATE = LocalDateTime.parse("2020-06-14T16:00:00");
    private static final PriceQuery QUERY = new PriceQuery(DATE, PRODUCT_ID, BRAND_ID);

    private final PriceRepositoryPort repository = mock(PriceRepositoryPort.class);
    private final FindApplicablePriceService service = new FindApplicablePriceService(repository, new PriceSelector());

    @Test
    @DisplayName("the port is queried with the brand, product and date of the query")
    void queriesThePortWithTheQueryValues() {
        when(repository.findCandidates(BRAND_ID, PRODUCT_ID, DATE)).thenReturn(SEED_PRICES);

        service.find(QUERY);

        verify(repository).findCandidates(BRAND_ID, PRODUCT_ID, DATE);
    }

    @Test
    @DisplayName("the price chosen by the selector among the candidates is returned")
    void returnsTheSelectedPrice() {
        when(repository.findCandidates(BRAND_ID, PRODUCT_ID, DATE)).thenReturn(SEED_PRICES);

        assertThat(service.find(QUERY)).isEqualTo(PRICE_LIST_2);
    }

    @Test
    @DisplayName("D5: no candidates → PriceNotFoundException with the query values")
    void throwsWhenThereAreNoCandidates() {
        when(repository.findCandidates(BRAND_ID, PRODUCT_ID, DATE)).thenReturn(List.of());

        assertThatExceptionOfType(PriceNotFoundException.class)
                .isThrownBy(() -> service.find(QUERY))
                .satisfies(exception -> {
                    assertThat(exception.brandId()).isEqualTo(BRAND_ID);
                    assertThat(exception.productId()).isEqualTo(PRODUCT_ID);
                    assertThat(exception.applicationDate()).isEqualTo(DATE);
                });
    }

    @Test
    @DisplayName("D5: candidates that don't apply at the date → PriceNotFoundException")
    void throwsWhenNoCandidateApplies() {
        Price expired = price(7, 5, "2020-01-01T00:00:00", "2020-06-14T15:59:59");
        when(repository.findCandidates(BRAND_ID, PRODUCT_ID, DATE)).thenReturn(List.of(expired));

        assertThatExceptionOfType(PriceNotFoundException.class).isThrownBy(() -> service.find(QUERY));
    }
}
