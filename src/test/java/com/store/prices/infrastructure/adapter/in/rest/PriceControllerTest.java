package com.store.prices.infrastructure.adapter.in.rest;

import static com.store.prices.domain.PriceFixtures.PRICE_LIST_1;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.store.prices.application.port.in.FindApplicablePriceUseCase;
import com.store.prices.application.port.in.PriceQuery;
import com.store.prices.domain.exception.PriceNotFoundException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** The REST adapter against the contract in docs/api/openapi.yaml, with the use case mocked. */
@WebMvcTest(PriceController.class)
@Import(PriceRestMapper.class)
class PriceControllerTest {

    private static final String PRICES_PATH = "/api/v1/prices";
    private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;
    private static final LocalDateTime DATE = LocalDateTime.parse("2020-06-14T10:00:00");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FindApplicablePriceUseCase useCase;

    @Test
    @DisplayName("200: the price is returned with every field, dates with seconds and the amount with 2 decimals")
    void returnsThePrice() throws Exception {
        when(useCase.find(any())).thenReturn(PRICE_LIST_1);

        mockMvc.perform(pricesRequest("2020-06-14T10:00:00", "35455", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "productId": 35455,
                          "brandId": 1,
                          "priceList": 1,
                          "startDate": "2020-06-14T00:00:00",
                          "endDate": "2020-12-31T23:59:59",
                          "price": 35.50,
                          "currency": "EUR"
                        }
                        """, JsonCompareMode.STRICT))
                // A JSON comparison treats 35.5 and 35.50 as equal, so check the scale in the raw body.
                .andExpect(content().string(containsString("\"price\":35.50")));
    }

    @Test
    @DisplayName("the use case receives the query built from the parameters")
    void passesTheParametersToTheUseCase() throws Exception {
        when(useCase.find(any())).thenReturn(PRICE_LIST_1);

        mockMvc.perform(pricesRequest("2020-06-14T10:00:00", "35455", "1"));

        verify(useCase).find(new PriceQuery(DATE, 35455L, 1L));
    }

    @Test
    @DisplayName("D5: no applicable price → 404 problem details")
    void returnsNotFoundWhenNoPriceApplies() throws Exception {
        when(useCase.find(any())).thenThrow(new PriceNotFoundException(1L, 99999L, DATE));

        mockMvc.perform(pricesRequest("2020-06-14T10:00:00", "99999", "1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(content().json(problem(404, "Not Found",
                        "No applicable price for brand 1, product 99999 at 2020-06-14T10:00:00.", PRICES_PATH),
                        JsonCompareMode.STRICT));
    }

    @ParameterizedTest(name = "{0}: {1}", quoteTextArguments = false)
    @CsvSource(delimiter = ';', textBlock = """
            # ID; description;          applicationDate;         productId; brandId; detail
            B12;  fractional seconds;      2020-06-14T10:00:00.500; 35455; 1;  Parameter 'applicationDate' must match yyyy-MM-ddTHH:mm:ss.
            B13;  missing applicationDate; ;                        35455; 1;  Required parameter 'applicationDate' is not present.
            B13;  missing productId;       2020-06-14T10:00:00;     ;      1;  Required parameter 'productId' is not present.
            B13;  missing brandId;         2020-06-14T10:00:00;     35455; ;   Required parameter 'brandId' is not present.
            B13;  date without time;       2020-06-14;              35455; 1;  Parameter 'applicationDate' must match yyyy-MM-ddTHH:mm:ss.
            B13;  date in another format;  14/06/2020 10:00;        35455; 1;  Parameter 'applicationDate' must match yyyy-MM-ddTHH:mm:ss.
            B13;  non-numeric productId;   2020-06-14T10:00:00;     abc;   1;  Parameter 'productId' must be an integer.
            B13;  productId zero;          2020-06-14T10:00:00;     0;     1;  Parameter 'productId' must be greater than or equal to 1.
            B13;  negative brandId;        2020-06-14T10:00:00;     35455; -1; Parameter 'brandId' must be greater than or equal to 1.
            """)
    void rejectsInvalidRequest(String caseId, String description, String applicationDate, String productId,
                               String brandId, String detail) throws Exception {
        mockMvc.perform(pricesRequest(applicationDate, productId, brandId))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(content().json(problem(400, "Bad Request", detail, PRICES_PATH), JsonCompareMode.STRICT));

        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("an unexpected error → 500 with a generic detail that exposes nothing internal")
    void returnsGenericInternalServerError() throws Exception {
        when(useCase.find(any())).thenThrow(new IllegalStateException("connection to db-host:9092 refused"));

        mockMvc.perform(pricesRequest("2020-06-14T10:00:00", "35455", "1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(content().json(problem(500, "Internal Server Error", "An unexpected error occurred.",
                        PRICES_PATH), JsonCompareMode.STRICT))
                .andExpect(content().string(not(containsString("db-host"))));
    }

    @Test
    @DisplayName("an unknown path → 404 problem details, not 500")
    void returnsNotFoundForUnknownPath() throws Exception {
        mockMvc.perform(get("/api/v1/unknown").accept(MediaType.APPLICATION_JSON, PROBLEM_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("an unsupported method → 405 problem details, not 500")
    void returnsMethodNotAllowedForPost() throws Exception {
        mockMvc.perform(post(PRICES_PATH).accept(MediaType.APPLICATION_JSON, PROBLEM_JSON))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(405));
    }

    private static String problem(int status, String title, String detail, String instance) {
        return """
                {
                  "type": "about:blank",
                  "title": "%s",
                  "status": %d,
                  "detail": "%s",
                  "instance": "%s"
                }
                """.formatted(title, status, detail, instance);
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
