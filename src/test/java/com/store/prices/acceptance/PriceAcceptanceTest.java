package com.store.prices.acceptance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Acceptance cases AT-1–AT-5 and B1–B13 from docs/requirements.md, run end to end against the real
 * H2 seed data with nothing mocked (T1, ADR-0006). Expected values come from the requirements and
 * must never be changed to make a test pass.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Disabled("Pending: enabled when the price endpoint is implemented")
class PriceAcceptanceTest {

    private static final String PRICES_PATH = "/api/v1/prices";
    private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "{0}: {1} → price list {2}, {5} EUR", quoteTextArguments = false)
    @CsvSource(textBlock = """
            # ID, applicationDate,     priceList, startDate,           endDate,             price
            AT-1, 2020-06-14T10:00:00, 1,         2020-06-14T00:00:00, 2020-12-31T23:59:59, 35.50
            AT-2, 2020-06-14T16:00:00, 2,         2020-06-14T15:00:00, 2020-06-14T18:30:00, 25.45
            AT-3, 2020-06-14T21:00:00, 1,         2020-06-14T00:00:00, 2020-12-31T23:59:59, 35.50
            AT-4, 2020-06-15T10:00:00, 3,         2020-06-15T00:00:00, 2020-06-15T11:00:00, 30.50
            AT-5, 2020-06-16T21:00:00, 4,         2020-06-15T16:00:00, 2020-12-31T23:59:59, 38.95
            B1,   2020-06-14T00:00:00, 1,         2020-06-14T00:00:00, 2020-12-31T23:59:59, 35.50
            B2,   2020-06-14T18:30:00, 2,         2020-06-14T15:00:00, 2020-06-14T18:30:00, 25.45
            B3,   2020-06-14T18:30:01, 1,         2020-06-14T00:00:00, 2020-12-31T23:59:59, 35.50
            B4,   2020-06-15T11:00:00, 3,         2020-06-15T00:00:00, 2020-06-15T11:00:00, 30.50
            B5,   2020-06-15T11:00:01, 1,         2020-06-14T00:00:00, 2020-12-31T23:59:59, 35.50
            B6,   2020-06-15T16:00:00, 4,         2020-06-15T16:00:00, 2020-12-31T23:59:59, 38.95
            B7,   2020-12-31T23:59:59, 4,         2020-06-15T16:00:00, 2020-12-31T23:59:59, 38.95
            """)
    void returnsApplicablePrice(String caseId, String applicationDate, long priceList,
                                String startDate, String endDate, String price) throws Exception {
        String expectedBody = """
                {
                  "productId": 35455,
                  "brandId": 1,
                  "priceList": %d,
                  "startDate": "%s",
                  "endDate": "%s",
                  "price": %s,
                  "currency": "EUR"
                }
                """.formatted(priceList, startDate, endDate, price);

        mockMvc.perform(pricesRequest(applicationDate, "35455", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedBody, JsonCompareMode.STRICT));
    }

    @ParameterizedTest(name = "{0}: {1}", quoteTextArguments = false)
    @CsvSource(textBlock = """
            # ID, description,        applicationDate,     productId, brandId
            B8,   before any range,   2020-06-13T23:59:59, 35455,     1
            B9,   after every range,  2021-01-01T00:00:00, 35455,     1
            B10,  unknown product,    2020-06-14T10:00:00, 99999,     1
            B11,  unknown brand,      2020-06-14T10:00:00, 35455,     2
            """)
    void returnsNotFound(String caseId, String description, String applicationDate,
                         String productId, String brandId) throws Exception {
        // The problem body is asserted too: a bare 404 would also pass while no endpoint exists.
        mockMvc.perform(pricesRequest(applicationDate, productId, brandId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @ParameterizedTest(name = "{0}: {1}", quoteTextArguments = false)
    @CsvSource(textBlock = """
            # ID, description,                  applicationDate,         productId, brandId
            B12,  fractional seconds,           2020-06-14T10:00:00.500, 35455,     1
            B13,  missing applicationDate,      ,                        35455,     1
            B13,  missing productId,            2020-06-14T10:00:00,     ,          1
            B13,  missing brandId,              2020-06-14T10:00:00,     35455,
            B13,  date without time,            2020-06-14,              35455,     1
            B13,  date in another format,       14/06/2020 10:00,        35455,     1
            B13,  non-numeric productId,        2020-06-14T10:00:00,     abc,       1
            B13,  productId zero,               2020-06-14T10:00:00,     0,         1
            B13,  negative brandId,             2020-06-14T10:00:00,     35455,     -1
            """)
    void rejectsInvalidRequest(String caseId, String description, String applicationDate,
                               String productId, String brandId) throws Exception {
        mockMvc.perform(pricesRequest(applicationDate, productId, brandId))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    /** Builds the request, leaving out any parameter given as {@code null}. */
    private static MockHttpServletRequestBuilder pricesRequest(String applicationDate, String productId,
                                                               String brandId) {
        MockHttpServletRequestBuilder request = get(PRICES_PATH).accept(MediaType.APPLICATION_JSON, PROBLEM_JSON);
        if (applicationDate != null) {
            request.param("applicationDate", applicationDate);
        }
        if (productId != null) {
            request.param("productId", productId);
        }
        if (brandId != null) {
            request.param("brandId", brandId);
        }
        return request;
    }
}
