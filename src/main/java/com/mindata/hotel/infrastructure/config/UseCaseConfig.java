package com.mindata.hotel.infrastructure.config;

import com.mindata.hotel.application.port.in.CountSearchesUseCase;
import com.mindata.hotel.application.port.in.PersistSearchUseCase;
import com.mindata.hotel.application.port.in.RegisterSearchUseCase;
import com.mindata.hotel.application.port.out.SearchEventPublisher;
import com.mindata.hotel.application.port.out.SearchRepository;
import com.mindata.hotel.application.service.CountSearchesService;
import com.mindata.hotel.application.service.PersistSearchService;
import com.mindata.hotel.application.service.RegisterSearchService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Conecta los casos de uso (sin anotaciones de Spring en la capa de aplicación). */
@Configuration
public class UseCaseConfig {

    @Bean
    RegisterSearchUseCase registerSearchUseCase(SearchEventPublisher publisher) {
        return new RegisterSearchService(publisher);
    }

    @Bean
    PersistSearchUseCase persistSearchUseCase(SearchRepository repository) {
        return new PersistSearchService(repository);
    }

    @Bean
    CountSearchesUseCase countSearchesUseCase(SearchRepository repository) {
        return new CountSearchesService(repository);
    }
}
