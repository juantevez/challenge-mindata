package com.mindata.hotel.infrastructure.adapter.out.persistence;

import com.mindata.hotel.application.port.out.SearchRepository;
import com.mindata.hotel.domain.model.RegisteredSearch;
import com.mindata.hotel.domain.model.SearchId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaSearchRepositoryAdapter implements SearchRepository {

    private static final Logger log = LoggerFactory.getLogger(JpaSearchRepositoryAdapter.class);

    private final SpringDataSearchRepository jpa;

    public JpaSearchRepositoryAdapter(SpringDataSearchRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(RegisteredSearch search) {
        try {
            jpa.save(SearchJpaEntity.from(search));
        } catch (DataIntegrityViolationException e) {
            // Redelivery de Kafka: si la fila ya existe es un duplicado inofensivo.
            // Si no existe, la violación es otra (p. ej. dato demasiado largo) y no se debe ocultar.
            if (jpa.existsById(search.id().value())) {
                log.debug("Search {} already persisted, ignoring duplicate delivery", search.id().value());
            } else {
                throw e;
            }
        }
    }

    @Override
    public Optional<RegisteredSearch> findById(SearchId id) {
        return jpa.findById(id.value()).map(SearchJpaEntity::toDomain);
    }

    @Override
    public long countByFingerprint(String fingerprint) {
        return jpa.countBySearchHash(fingerprint);
    }
}
