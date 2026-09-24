package com.store.prices.infrastructure.config;

import com.store.prices.application.port.in.FindApplicablePriceUseCase;
import com.store.prices.application.service.FindApplicablePriceService;
import com.store.prices.domain.port.PriceRepositoryPort;
import com.store.prices.domain.service.PriceSelector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the framework-free domain and application classes, which carry no Spring annotations. */
@Configuration
public class BeanConfiguration {

    @Bean
    public PriceSelector priceSelector() {
        return new PriceSelector();
    }

    @Bean
    public FindApplicablePriceUseCase findApplicablePriceUseCase(PriceRepositoryPort priceRepository,
            PriceSelector priceSelector) {
        return new FindApplicablePriceService(priceRepository, priceSelector);
    }
}
