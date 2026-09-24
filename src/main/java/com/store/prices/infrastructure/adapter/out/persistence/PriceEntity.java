package com.store.prices.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A row of {@code PRICES}. The service is read-only, so there are no setters. {@code ID} and the audit columns are
 * mapped but have no getters, so they never reach the domain.
 */
@Entity
@Table(name = "PRICES")
public class PriceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "BRAND_ID", nullable = false)
    private long brandId;

    @Column(name = "START_DATE", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "END_DATE", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "PRICE_LIST", nullable = false)
    private long priceList;

    @Column(name = "PRODUCT_ID", nullable = false)
    private long productId;

    @Column(name = "PRIORITY", nullable = false)
    private int priority;

    @Column(name = "PRICE", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "CURR", nullable = false, length = 3)
    private String currency;

    @Column(name = "LAST_UPDATE", nullable = false)
    private LocalDateTime lastUpdate;

    @Column(name = "LAST_UPDATE_BY", nullable = false, length = 50)
    private String lastUpdateBy;

    protected PriceEntity() {
        // Required by JPA.
    }

    public long getBrandId() {
        return brandId;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public long getPriceList() {
        return priceList;
    }

    public long getProductId() {
        return productId;
    }

    public int getPriority() {
        return priority;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCurrency() {
        return currency;
    }
}
