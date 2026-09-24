package com.store.prices.infrastructure.adapter.out.persistence;

import static com.store.prices.domain.PriceFixtures.BRAND_ID;
import static com.store.prices.domain.PriceFixtures.PRODUCT_ID;
import static com.store.prices.domain.PriceFixtures.SEED_PRICES;
import static org.assertj.core.api.Assertions.assertThat;

import com.store.prices.domain.model.Price;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * The pre-filter query and the entity → domain mapping, against the real seed data. Comparing with the domain
 * fixtures checks every mapped field, including the currency and the scale of the amount.
 */
@DataJpaTest
@Import({PricePersistenceAdapter.class, PriceEntityMapper.class})
class PricePersistenceAdapterTest {

    @Autowired
    private PricePersistenceAdapter adapter;

    @ParameterizedTest(name = "{0}: {1} → price lists {2}", quoteTextArguments = false)
    @CsvSource(delimiter = ';', textBlock = """
            AT-1;                  2020-06-14T10:00:00; 1
            AT-2;                  2020-06-14T16:00:00; 1|2
            AT-3;                  2020-06-14T21:00:00; 1
            AT-4;                  2020-06-15T10:00:00; 1|3
            AT-5;                  2020-06-16T21:00:00; 1|4
            B1;                    2020-06-14T00:00:00; 1
            B2;                    2020-06-14T18:30:00; 1|2
            B3;                    2020-06-14T18:30:01; 1
            B4;                    2020-06-15T11:00:00; 1|3
            B5;                    2020-06-15T11:00:01; 1
            B6;                    2020-06-15T16:00:00; 1|4
            B7;                    2020-12-31T23:59:59; 1|4
            B8;                    2020-06-13T23:59:59; none
            B9;                    2021-01-01T00:00:00; none
            start of price list 2; 2020-06-14T15:00:00; 1|2
            """)
    @DisplayName("returns the seed prices whose range contains the date, both ends inclusive")
    void returnsCandidatesWhoseRangeContainsTheDate(String label, LocalDateTime date, String priceLists) {
        List<Price> candidates = adapter.findCandidates(BRAND_ID, PRODUCT_ID, date);

        assertThat(candidates).containsExactlyInAnyOrderElementsOf(seedPrices(priceLists));
    }

    // 16:00 rather than the 10:00 of B10 and B11: two rows would match there if the filter ignored the id.
    @Test
    @DisplayName("an unknown product has no candidates")
    void returnsNothingForUnknownProduct() {
        assertThat(adapter.findCandidates(BRAND_ID, 99999L, LocalDateTime.parse("2020-06-14T16:00:00"))).isEmpty();
    }

    @Test
    @DisplayName("an unknown brand has no candidates")
    void returnsNothingForUnknownBrand() {
        assertThat(adapter.findCandidates(2L, PRODUCT_ID, LocalDateTime.parse("2020-06-14T16:00:00"))).isEmpty();
    }

    private static List<Price> seedPrices(String priceLists) {
        if (priceLists.equals("none")) {
            return List.of();
        }
        return Arrays.stream(priceLists.split("\\|"))
                .map(Long::parseLong)
                .map(priceList -> SEED_PRICES.stream().filter(p -> p.priceList() == priceList).findFirst().orElseThrow())
                .toList();
    }
}
