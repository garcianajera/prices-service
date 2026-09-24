package com.store.prices.domain.service;

import static com.store.prices.domain.PriceFixtures.PRICE_LIST_1;
import static com.store.prices.domain.PriceFixtures.SEED_PRICES;
import static com.store.prices.domain.PriceFixtures.price;
import static org.assertj.core.api.Assertions.assertThat;

import com.store.prices.domain.model.Price;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** T2: the stream-based selection rule (FR-4, D3, D4, D14), without Spring or a database. */
class PriceSelectorTest {

    private static final LocalDateTime DATE = LocalDateTime.parse("2020-06-14T16:00:00");

    private final PriceSelector selector = new PriceSelector();

    @Test
    @DisplayName("no candidates → empty")
    void returnsEmptyWhenThereAreNoCandidates() {
        assertThat(selector.select(List.of(), DATE)).isEmpty();
    }

    @Test
    @DisplayName("a single applicable candidate is selected")
    void selectsTheOnlyApplicableCandidate() {
        assertThat(selector.select(List.of(PRICE_LIST_1), DATE)).contains(PRICE_LIST_1);
    }

    @Test
    @DisplayName("candidates that don't apply at the date → empty")
    void returnsEmptyWhenNoCandidateApplies() {
        Price expired = price(7, 5, "2020-01-01T00:00:00", "2020-06-14T15:59:59");
        Price future = price(8, 5, "2020-06-14T16:00:01", "2020-12-31T23:59:59");

        assertThat(selector.select(List.of(expired, future), DATE)).isEmpty();
    }

    @Test
    @DisplayName("FR-4: the highest priority wins, even with an earlier start date and a lower price list")
    void selectsHighestPriorityOverLaterStartAndHigherPriceList() {
        // Only priority favours the winner, so this fails if priority is not the first criterion.
        Price highPriority = price(1, 5, "2020-06-14T08:00:00", "2020-06-14T20:00:00");
        Price lowPriority = price(9, 1, "2020-06-14T12:00:00", "2020-06-14T20:00:00");

        assertThat(selector.select(List.of(highPriority, lowPriority), DATE)).contains(highPriority);
        assertThat(selector.select(List.of(lowPriority, highPriority), DATE)).contains(highPriority);
    }

    @Test
    @DisplayName("a candidate that doesn't apply is ignored, even with a higher priority")
    void ignoresNonApplicableCandidateWithHigherPriority() {
        Price notYetValid = price(9, 99, "2020-06-14T16:00:01", "2020-12-31T23:59:59");

        assertThat(selector.select(List.of(PRICE_LIST_1, notYetValid), DATE)).contains(PRICE_LIST_1);
    }

    @Test
    @DisplayName("D4: same highest priority → the latest start date wins, even with a lower price list")
    void breaksPriorityTieByLatestStartDate() {
        // The later price has the lower price list, so this fails if D14 is applied before D4.
        Price earlier = price(6, 1, "2020-06-14T08:00:00", "2020-06-14T20:00:00");
        Price later = price(5, 1, "2020-06-14T12:00:00", "2020-06-14T20:00:00");

        assertThat(selector.select(List.of(later, earlier), DATE)).contains(later);
        assertThat(selector.select(List.of(earlier, later), DATE)).contains(later);
    }

    @Test
    @DisplayName("D14: same highest priority and start date → the highest price list wins")
    void breaksRemainingTieByHighestPriceList() {
        // A higher priority elsewhere must not interfere: both candidates share priority and start date.
        Price lowerList = price(5, 1, "2020-06-14T12:00:00", "2020-06-14T20:00:00");
        Price higherList = price(6, 1, "2020-06-14T12:00:00", "2020-06-14T18:00:00");

        assertThat(selector.select(List.of(higherList, lowerList), DATE)).contains(higherList);
        assertThat(selector.select(List.of(lowerList, higherList), DATE)).contains(higherList);
    }

    @ParameterizedTest(name = "{0}: {1} → price list {2}", quoteTextArguments = false)
    @DisplayName("acceptance cases on the seed prices, in memory")
    @CsvSource(textBlock = """
            # ID, applicationDate,     priceList
            AT-1, 2020-06-14T10:00:00, 1
            AT-2, 2020-06-14T16:00:00, 2
            AT-3, 2020-06-14T21:00:00, 1
            AT-4, 2020-06-15T10:00:00, 3
            AT-5, 2020-06-16T21:00:00, 4
            """)
    void selectsExpectedPriceListForAcceptanceCases(String caseId, LocalDateTime applicationDate, long priceList) {
        assertThat(selector.select(SEED_PRICES, applicationDate))
                .map(Price::priceList)
                .contains(priceList);
    }
}
