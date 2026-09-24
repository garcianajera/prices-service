package com.store.prices.application.port.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PriceQueryTest {

    private static final LocalDateTime DATE = LocalDateTime.parse("2020-06-14T10:00:00");

    @Test
    void acceptsValidQuery() {
        PriceQuery query = new PriceQuery(DATE, 35455L, 1L);

        assertThat(query.applicationDate()).isEqualTo(DATE);
        assertThat(query.productId()).isEqualTo(35455L);
        assertThat(query.brandId()).isEqualTo(1L);
    }

    @Test
    void rejectsNullApplicationDate() {
        assertThatNullPointerException().isThrownBy(() -> new PriceQuery(null, 35455L, 1L))
                .withMessageContaining("applicationDate");
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void rejectsNonPositiveProductId(long productId) {
        assertThatIllegalArgumentException().isThrownBy(() -> new PriceQuery(DATE, productId, 1L))
                .withMessageContaining("productId");
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    void rejectsNonPositiveBrandId(long brandId) {
        assertThatIllegalArgumentException().isThrownBy(() -> new PriceQuery(DATE, 35455L, brandId))
                .withMessageContaining("brandId");
    }
}
