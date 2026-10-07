package com.mindata.hotel.infrastructure.adapter.out.persistence;

import com.mindata.hotel.domain.model.HotelSearch;
import com.mindata.hotel.domain.model.RegisteredSearch;
import com.mindata.hotel.domain.model.SearchId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fila de {@code hotel_availability_search}. Implementa {@link Persistable} para que Spring Data haga
 * INSERT directo (sin SELECT previo) aunque el identificador ya venga asignado.
 *
 * <p>Es la única clase no inmutable del proyecto porque JPA lo exige (constructor sin argumentos y campos
 * no final que Hibernate rellena por reflexión). Se limita el daño: no tiene setters, solo se crea con
 * {@link #from} y no sale del adaptador; hacia afuera se convierte en el record {@link RegisteredSearch}.
 */
@Entity
@Table(name = "hotel_availability_search")
public class SearchJpaEntity implements Persistable<String> {

    @Id
    @Column(name = "search_id", nullable = false, length = 36)
    private String searchId;

    @Column(name = "hotel_id", nullable = false, length = 256)
    private String hotelId;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    /** Edades en el orden original, separadas por coma ("30,29,1,3"). */
    @Column(name = "ages", nullable = false, length = 512)
    private String ages;

    @Column(name = "search_hash", nullable = false, length = 64)
    private String searchHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Transient
    private boolean newEntity = true;

    protected SearchJpaEntity() {
    }

    static SearchJpaEntity from(RegisteredSearch registered) {
        HotelSearch search = registered.search();
        SearchJpaEntity entity = new SearchJpaEntity();
        entity.searchId = registered.id().value();
        entity.hotelId = search.hotelId();
        entity.checkIn = search.checkIn();
        entity.checkOut = search.checkOut();
        entity.ages = search.ages().stream().map(String::valueOf).collect(Collectors.joining(","));
        entity.searchHash = search.fingerprint();
        entity.createdAt = LocalDateTime.now(ZoneOffset.UTC);
        return entity;
    }

    RegisteredSearch toDomain() {
        List<Integer> agesList = Arrays.stream(ages.split(",")).map(Integer::valueOf).toList();
        return new RegisteredSearch(new SearchId(searchId), new HotelSearch(hotelId, checkIn, checkOut, agesList));
    }

    @Override
    public String getId() {
        return searchId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.newEntity = false;
    }
}
