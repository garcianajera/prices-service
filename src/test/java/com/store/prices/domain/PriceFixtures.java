package com.store.prices.domain;

import com.store.prices.domain.model.Price;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;

/** Test data for domain tests: the four seed rows as {@link Price} objects, plus a factory for custom cases. */
public final class PriceFixtures {

    public static final long BRAND_ID = 1L;
    public static final long PRODUCT_ID = 35455L;
    public static final Currency EUR = Currency.getInstance("EUR");

    public static final Price PRICE_LIST_1 = seed(1, 0, "2020-06-14T00:00:00", "2020-12-31T23:59:59", "35.50");
    public static final Price PRICE_LIST_2 = seed(2, 1, "2020-06-14T15:00:00", "2020-06-14T18:30:00", "25.45");
    public static final Price PRICE_LIST_3 = seed(3, 1, "2020-06-15T00:00:00", "2020-06-15T11:00:00", "30.50");
    public static final Price PRICE_LIST_4 = seed(4, 1, "2020-06-15T16:00:00", "2020-12-31T23:59:59", "38.95");

    /** Seed rows 1–4 of docs/requirements.md, in table order. */
    public static final List<Price> SEED_PRICES = List.of(PRICE_LIST_1, PRICE_LIST_2, PRICE_LIST_3, PRICE_LIST_4);

    private PriceFixtures() {
    }

    /** A price for the seed brand and product, with a fixed amount, for cases where only these fields matter. */
    public static Price price(long priceList, int priority, String startDate, String endDate) {
        return seed(priceList, priority, startDate, endDate, "10.00");
    }

    private static Price seed(long priceList, int priority, String startDate, String endDate, String amount) {
        return new Price(BRAND_ID, PRODUCT_ID, priceList, LocalDateTime.parse(startDate), LocalDateTime.parse(endDate),
                priority, new BigDecimal(amount), EUR);
    }
}
