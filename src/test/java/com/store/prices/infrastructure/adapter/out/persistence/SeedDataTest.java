package com.store.prices.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Guards the rule that data.sql holds exactly the rows of the source CSV, so the two cannot drift apart.
 */
@DataJpaTest
class SeedDataTest {

    private static final Path SOURCE_CSV = Path.of("docs/source/prices.csv");
    private static final DateTimeFormatter SOURCE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("PRICES contains exactly the rows of docs/source/prices.csv")
    void seedDataMatchesSourceCsv() throws IOException {
        List<SeedRow> expected = readSourceCsv();

        List<SeedRow> actual = jdbcTemplate.query("""
                SELECT BRAND_ID, START_DATE, END_DATE, PRICE_LIST, PRODUCT_ID, PRIORITY, PRICE, CURR,
                       LAST_UPDATE, LAST_UPDATE_BY
                FROM PRICES
                ORDER BY ID
                """,
                (rs, rowNum) -> new SeedRow(
                        rs.getLong("BRAND_ID"),
                        rs.getObject("START_DATE", LocalDateTime.class),
                        rs.getObject("END_DATE", LocalDateTime.class),
                        rs.getLong("PRICE_LIST"),
                        rs.getLong("PRODUCT_ID"),
                        rs.getInt("PRIORITY"),
                        rs.getBigDecimal("PRICE"),
                        rs.getString("CURR"),
                        rs.getObject("LAST_UPDATE", LocalDateTime.class),
                        rs.getString("LAST_UPDATE_BY")));

        assertThat(expected).hasSize(4);
        assertThat(actual).containsExactlyElementsOf(expected);
    }

    private static List<SeedRow> readSourceCsv() throws IOException {
        return Files.readAllLines(SOURCE_CSV, StandardCharsets.UTF_8).stream()
                .skip(1)
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .map(line -> line.split(","))
                .map(fields -> new SeedRow(
                        Long.parseLong(fields[0]),
                        LocalDateTime.parse(fields[1], SOURCE_DATE_FORMAT),
                        LocalDateTime.parse(fields[2], SOURCE_DATE_FORMAT),
                        Long.parseLong(fields[3]),
                        Long.parseLong(fields[4]),
                        Integer.parseInt(fields[5]),
                        new BigDecimal(fields[6]),
                        fields[7],
                        LocalDateTime.parse(fields[8], SOURCE_DATE_FORMAT),
                        fields[9]))
                .toList();
    }

    private record SeedRow(
            long brandId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            long priceList,
            long productId,
            int priority,
            BigDecimal price,
            String currency,
            LocalDateTime lastUpdate,
            String lastUpdateBy) {
    }
}
