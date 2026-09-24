package com.store.prices.infrastructure.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** Guards ADR-0007: Swagger UI presents the hand-written contract, and never a spec generated from the code. */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiServingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("the hand-written docs/api/openapi.yaml is served unchanged")
    void servesTheHandWrittenContract() throws Exception {
        byte[] contract = Files.readAllBytes(Path.of("docs/api/openapi.yaml"));

        mockMvc.perform(get("/openapi.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(contract));
    }

    @Test
    @DisplayName("Swagger UI is configured to load the hand-written contract")
    void swaggerUiLoadsTheHandWrittenContract() throws Exception {
        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/openapi.yaml"));
    }

    @Test
    @DisplayName("the spec generated from the code describes no paths")
    void generatedSpecIsEmpty() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths").isEmpty());
    }
}
