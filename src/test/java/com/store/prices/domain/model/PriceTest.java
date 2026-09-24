package com.store.prices.domain.model;

import static com.store.prices.domain.PriceFixtures.EUR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PriceTest {

    private static final LocalDateTime START = LocalDateTime.parse("2020-06-14T15:00:00");
    private static final LocalDateTime END = LocalDateTime.parse("2020-06-14T18:30:00");
    private static final BigDecimal AMOUNT = new BigDecimal("25.45");

    private static Price price(LocalDateTime start, LocalDateTime end, int priority, BigDecimal amount) {
        return new Price(1L, 35455L, 2L, start, end, priority, amount, EUR);
    }

    @Nested
    @DisplayName("invariants")
    class Invariants {

        @Test
        void acceptsValidPrice() {
            Price price = price(START, END, 1, AMOUNT);

            assertThat(price.startDate()).isEqualTo(START);
            assertThat(price.endDate()).isEqualTo(END);
            assertThat(price.amount()).isEqualByComparingTo("25.45");
            assertThat(price.currency()).isEqualTo(EUR);
        }

        @Test
        void acceptsRangeOfASingleInstant() {
            assertThat(price(START, START, 0, AMOUNT).endDate()).isEqualTo(START);
        }

        @Test
        void acceptsZeroPriorityAndZeroAmount() {
            Price price = price(START, END, 0, BigDecimal.ZERO);

            assertThat(price.priority()).isZero();
            assertThat(price.amount()).isEqualByComparingTo("0");
        }

        @Test
        void rejectsNullStartDate() {
            assertThatNullPointerException().isThrownBy(() -> price(null, END, 1, AMOUNT)).withMessageContaining("startDate");
        }

        @Test
        void rejectsNullEndDate() {
            assertThatNullPointerException().isThrownBy(() -> price(START, null, 1, AMOUNT)).withMessageContaining("endDate");
        }

        @Test
        void rejectsNullAmount() {
            assertThatNullPointerException().isThrownBy(() -> price(START, END, 1, null)).withMessageContaining("amount");
        }

        @Test
        void rejectsNullCurrency() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new Price(1L, 35455L, 2L, START, END, 1, AMOUNT, null))
                    .withMessageContaining("currency");
        }

        @Test
        void rejectsEndDateBeforeStartDate() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> price(END, START, 1, AMOUNT))
                    .withMessageContaining("endDate");
        }

        @Test
        void rejectsNegativePriority() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> price(START, END, -1, AMOUNT))
                    .withMessageContaining("priority");
        }

        @Test
        void rejectsNegativeAmount() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> price(START, END, 1, new BigDecimal("-0.01")))
                    .withMessageContaining("amount");
        }
    }

    @Nested
    @DisplayName("isApplicableAt: both ends of the range are inclusive (D3)")
    class IsApplicableAt {

        private final Price price = price(START, END, 1, AMOUNT);

        @ParameterizedTest(name = "{0} → {1} ({2})", quoteTextArguments = false)
        @CsvSource(textBlock = """
                2020-06-14T14:59:59, false, one second before the start
                2020-06-14T15:00:00, true,  exactly at the start
                2020-06-14T16:00:00, true,  inside the range
                2020-06-14T18:30:00, true,  exactly at the end
                2020-06-14T18:30:01, false, one second after the end
                """)
        void appliesOnlyWithinTheInclusiveRange(LocalDateTime date, boolean applicable, String description) {
            assertThat(price.isApplicableAt(date)).isEqualTo(applicable);
        }
    }
}
