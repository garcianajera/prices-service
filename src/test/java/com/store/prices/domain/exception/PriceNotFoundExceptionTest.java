package com.store.prices.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PriceNotFoundExceptionTest {

    @Test
    void carriesTheQueryAndDescribesIt() {
        LocalDateTime date = LocalDateTime.parse("2020-06-14T10:00:00");

        PriceNotFoundException exception = new PriceNotFoundException(1L, 99999L, date);

        assertThat(exception.brandId()).isEqualTo(1L);
        assertThat(exception.productId()).isEqualTo(99999L);
        assertThat(exception.applicationDate()).isEqualTo(date);
        assertThat(exception).hasMessage("No applicable price for brand 1, product 99999 at 2020-06-14T10:00:00");
    }
}
